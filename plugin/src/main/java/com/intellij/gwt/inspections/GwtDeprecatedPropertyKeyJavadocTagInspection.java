package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.rpc.GwtGenericsUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.IncorrectOperationException;
import consulo.annotation.access.RequiredReadAction;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class GwtDeprecatedPropertyKeyJavadocTagInspection extends BaseGwtInspection
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.inspections.GwtDeprecatedPropertyKeyJavadocTagInspection");

	@RequiredReadAction
	@Override
	public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension, @Nonnull GwtVersion version, @Nonnull final PsiClass aClass, @Nonnull final InspectionManager manager, final boolean isOnTheFly)
	{
		if(!version.isGenericsSupported())
		{
			return null;
		}

		PropertiesFile[] files = GwtI18nManager.getInstance(manager.getProject()).getPropertiesFiles(aClass);
		if(files.length == 0)
		{
			return null;
		}

		return checkInterface(aClass, manager);
	}

	private static ProblemDescriptor[] checkInterface(final PsiClass anInterface, final InspectionManager manager)
	{
		List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
		for(PsiMethod method : anInterface.getMethods())
		{
			PsiDocComment comment = method.getDocComment();
			if(comment != null)
			{
				PsiDocTag tag = comment.findTagByName(GwtI18nUtil.GWT_KEY_TAG);
				if(tag != null)
				{
					ReplaceTagByAnnotationQuickFix fix = new ReplaceTagByAnnotationQuickFix(tag, method);
					String message = GwtBundle.message("problem.description.gwt.key.tag.is.deprecated.in.gwt.1.5");
					problems.add(manager.createProblemDescriptor(tag, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
				}
			}
		}
		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}

	@Override
	@Nonnull
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.WARNING;
	}

	@Override
	@Nonnull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.deprecated.gwt.key.tag.in.javadoc.comments");
	}

	@Override
	@Nonnull
	public String getShortName()
	{
		return "GwtDeprecatedPropertyKeyJavadocTag";
	}

	private static class ReplaceTagByAnnotationQuickFix extends BaseGwtLocalQuickFix
	{
		private final PsiDocTag myTag;
		private final PsiMethod myMethod;

		public ReplaceTagByAnnotationQuickFix(final PsiDocTag tag, final PsiMethod method)
		{
			super(GwtBundle.message("quickfix.name.replace.gwt.key.tag.with.key.annotation.in.method.0", PsiFormatUtil.formatMethod(method,
					PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE)));
			myTag = tag;
			myMethod = method;
		}

		@Override
		public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor)
		{
			String propertyName = myTag.getValueElement().getText();
			try
			{
				GwtI18nUtil.addKeyAnnotation(propertyName, myMethod, JavaPsiFacade.getInstance(project).getElementFactory());
				GwtGenericsUtil.removeJavadocTags(myMethod, GwtI18nUtil.GWT_KEY_TAG);
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}
	}
}
