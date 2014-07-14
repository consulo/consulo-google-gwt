package com.intellij.gwt.inspections;

import gnu.trove.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.google.gwt.module.extension.GoogleGwtModuleExtension;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.rpc.GwtGenericsUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.search.searches.DefinitionsSearch;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author nik
 */
public class GwtObsoleteTypeArgsJavadocTagInspection extends BaseGwtInspection
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.inspections.GwtObsoleteTypeArgsJavadocTagInspection");

	@Override
	public ProblemDescriptor[] checkClass(@NotNull final PsiClass aClass, @NotNull final InspectionManager manager, final boolean isOnTheFly)
	{
		GoogleGwtModuleExtension gwtFacet = getFacet(aClass);
		if(gwtFacet == null || !gwtFacet.getSdkVersion().isGenericsSupported())
		{
			return null;
		}

		if(RemoteServiceUtil.isRemoteServiceInterface(aClass))
		{
			return checkRemoteServiceInterface(aClass, manager, gwtFacet.getSdkVersion());
		}
		return null;
	}

	private static ProblemDescriptor[] checkRemoteServiceInterface(final PsiClass aClass, final InspectionManager manager, final GwtVersion gwtVersion)
	{
		List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
		for(PsiMethod method : aClass.getMethods())
		{
			PsiDocComment comment = method.getDocComment();
			if(comment != null)
			{
				PsiDocTag[] tags = comment.findTagsByName(GwtGenericsUtil.TYPE_ARGS_TAG);
				if(tags.length > 0)
				{
					GenerifyServiceMethodFix fix = new GenerifyServiceMethodFix(method, gwtVersion);
					String message = GwtBundle.message("problem.description.gwt.typeargs.tag.is.obsolete.in.gwt.1.5");
					problems.add(manager.createProblemDescriptor(comment, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
				}
			}
		}
		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}

	@Override
	@NotNull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.obsolete.gwt.typeargs.tag.in.javadoc.comments");
	}

	@Override
	@NotNull
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.WARNING;
	}

	@Override
	@NotNull
	public String getShortName()
	{
		return "GwtObsoleteTypeArgsJavadocTag";
	}

	private static class GenerifyServiceMethodFix extends BaseGwtLocalQuickFix
	{
		private final PsiMethod myMethod;
		private final GwtVersion myGwtVersion;

		protected GenerifyServiceMethodFix(final PsiMethod method, final GwtVersion gwtVersion)
		{
			super(GwtBundle.message("quickfix.name.generify.types.in.method.0.instead.of.using.gwt.typeargs.tags", PsiFormatUtil.formatMethod(method,
					PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE)));
			myMethod = method;
			myGwtVersion = gwtVersion;
		}

		@Override
		public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor descriptor)
		{
			try
			{
				PsiType newReturnType;
				String returnTypeParameters = GwtGenericsUtil.getReturnTypeParametersString(myMethod);
				if(returnTypeParameters != null)
				{
					newReturnType = appendTypeParameters(myMethod.getReturnType(), returnTypeParameters, myMethod);
				}
				else
				{
					newReturnType = null;
				}

				TIntObjectHashMap<PsiType> newParameterTypes = new TIntObjectHashMap<PsiType>();
				PsiParameter[] parameters = myMethod.getParameterList().getParameters();
				for(int i = 0; i < parameters.length; i++)
				{
					PsiParameter parameter = parameters[i];
					String typeParametersString = GwtGenericsUtil.getTypeParametersString(myMethod, parameter.getName());
					if(typeParametersString != null)
					{
						PsiType type = appendTypeParameters(parameter.getType(), typeParametersString, myMethod);
						newParameterTypes.put(i, type);
					}
				}

				if(newReturnType == null && newParameterTypes.isEmpty())
				{
					return;
				}

				List<PsiMethod> methods = findImplementations(project);
				if(methods == null)
				{
					return;
				}

				methods.add(0, myMethod);

				Set<VirtualFile> affectedFiles = new HashSet<VirtualFile>();
				for(PsiMethod method : methods)
				{
					affectedFiles.add(method.getContainingFile().getVirtualFile());
				}

				PsiClass async = RemoteServiceUtil.findAsynchronousInterface(myMethod.getContainingClass());
				PsiMethod asyncMethod = null;
				if(async != null)
				{
					asyncMethod = RemoteServiceUtil.findAsynchronousMethod(myMethod);
					affectedFiles.add(async.getContainingFile().getVirtualFile());
				}

				if(ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(affectedFiles.toArray(new VirtualFile[affectedFiles.size()]))
						.hasReadonlyFiles())
				{
					return;
				}

				SmartPsiElementPointer<PsiMethod> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(myMethod);
				for(PsiMethod method : methods)
				{
					updateSignature(method, newReturnType, newParameterTypes);
				}
				PsiMethod newMethod = pointer.getElement();
				if(newMethod != null)
				{
					if(asyncMethod != null)
					{
						asyncMethod.delete();
						RemoteServiceUtil.copyMethodToAsync(newMethod, async, myGwtVersion);
					}
					GwtGenericsUtil.removeTypeArgsJavadocTags(newMethod);
				}
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}

		private static void updateSignature(final PsiMethod method, final PsiType newReturnType, final TIntObjectHashMap<PsiType> newParameterTypes)
				throws IncorrectOperationException
		{
			PsiElementFactory elementFactory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();

			if(newReturnType != null)
			{
				PsiTypeElement returnTypeElement = method.getReturnTypeElement();
				if(returnTypeElement != null)
				{
					returnTypeElement.replace(elementFactory.createTypeElement(newReturnType));
				}
			}

			PsiParameter[] parameters = method.getParameterList().getParameters();
			for(int i : newParameterTypes.keys())
			{
				parameters[i].getTypeElement().replace(elementFactory.createTypeElement(newParameterTypes.get(i)));
			}
		}

		@Nullable
		private List<PsiMethod> findImplementations(final Project project)
		{
			final List<PsiMethod> methods = new ArrayList<PsiMethod>();
			if(!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable()
			{
				@Override
				public void run()
				{
					Collection<PsiElement> elements = DefinitionsSearch.search(myMethod).findAll();
					for(PsiElement element : elements)
					{
						if(element instanceof PsiMethod)
						{
							methods.add((PsiMethod) element);
						}
					}
				}
			}, GwtBundle.message("gwt.searching.for.implementations"), true, project))
			{
				return null;
			}

			return methods;
		}

		private static PsiType appendTypeParameters(final @NotNull PsiType type, final @NotNull String typeParametersString,
				final @NotNull PsiElement context) throws IncorrectOperationException
		{
			if(type instanceof PsiClassType)
			{
				final PsiClassType classType = (PsiClassType) type;

				if(classType.isRaw())
				{
					PsiElementFactory elementFactory = JavaPsiFacade.getInstance(context.getProject()).getElementFactory();
					return elementFactory.createTypeFromText(type.getCanonicalText() + typeParametersString, context);
				}
			}
			return type;
		}
	}
}
