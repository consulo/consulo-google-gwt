/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import consulo.annotation.access.RequiredReadAction;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GwtInconsistentAsyncInterfaceInspection extends BaseGwtInspection
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.inspections.GwtInconsistentAsyncInterfaceInspection");

	@RequiredReadAction
	@Override
	@Nullable
	public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension, @Nonnull GwtVersion version, @Nonnull final PsiClass aClass, @Nonnull InspectionManager manager, boolean isOnTheFly)
	{
		GoogleGwtModuleExtension gwtFacet = getExtension(aClass);
		if(gwtFacet == null)
		{
			return null;
		}

		if(RemoteServiceUtil.isRemoteServiceInterface(aClass))
		{
			return checkRemoteServiceForAsync(aClass, version, manager);
		}

		final PsiClass synch = RemoteServiceUtil.findSynchronousInterface(aClass);
		if(synch != null)
		{
			return checkAsyncServiceForRemote(synch, aClass, version, manager);
		}

		return null;
	}

	@Nullable
	private static ProblemDescriptor[] checkAsyncServiceForRemote(PsiClass sync, PsiClass async, final GwtVersion gwtVersion, InspectionManager manager)
	{
		List<ProblemDescriptor> problems = new SmartList<ProblemDescriptor>();
		List<PsiMethod> methodsToCopy = new SmartList<PsiMethod>();
		for(PsiMethod method : sync.getMethods())
		{
			if(!RemoteServiceUtil.isMethodPresentedInAsync(method, async))
			{
				methodsToCopy.add(method);
			}
		}

		for(PsiMethod asyncMethod : async.getMethods())
		{
			if(!RemoteServiceUtil.isMethodPresentedInSync(asyncMethod, sync))
			{
				final String message = GwtBundle.message("problem.description.async.method.doesn.t.have.sync.variant", asyncMethod.getName());
				final LocalQuickFix quickFix = new CopyMethodToSyncQuickFix(asyncMethod, sync);
				problems.add(manager.createProblemDescriptor(getElementToHighlight(asyncMethod), message, quickFix,
						ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
			}
		}

		if(!methodsToCopy.isEmpty())
		{
			String message = GwtBundle.message("problem.description.methods.of.async.remote.service.0.isn.t.synchronized.with.1", async.getName(),
					sync.getName());
			List<LocalQuickFix> quickFixesList = new ArrayList<LocalQuickFix>();
			for(PsiMethod method : methodsToCopy)
			{
				quickFixesList.add(new CopyMethodToAsyncQuickFix(async, method, gwtVersion));
			}
			quickFixesList.add(new SynchronizeAllMethodsInAsyncQuickFix(async, sync, gwtVersion));
			LocalQuickFix[] fixes = quickFixesList.toArray(new LocalQuickFix[quickFixesList.size()]);
			problems.add(manager.createProblemDescriptor(getElementToHighlight(async), message, fixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
		}


		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}

	private static ProblemDescriptor[] checkRemoteServiceForAsync(PsiClass aClass, final GwtVersion gwtVersion, InspectionManager manager)
	{
		GlobalSearchScope scope = aClass.getResolveScope();


		final PsiClass async = JavaPsiFacade.getInstance(manager.getProject()).findClass(aClass.getQualifiedName() + RemoteServiceUtil.ASYNC_SUFFIX,
				scope);
		if(async == null)
		{
			final String description = GwtBundle.message("problem.description.remote.service.0.doesn.t.have.corresponding.async.variant", aClass.getName());
			return new ProblemDescriptor[]{
					manager.createProblemDescriptor(getElementToHighlight(aClass), description, new CreateAsyncClassQuickFix(aClass, gwtVersion),
							ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
			};
		}

		ArrayList<ProblemDescriptor> result = new ArrayList<ProblemDescriptor>(0);

		for(final PsiMethod method : aClass.getMethods())
		{
			if(!RemoteServiceUtil.isMethodPresentedInAsync(method, async))
			{
				LocalQuickFix fix = new SynchronizeAllMethodsInAsyncQuickFix(async, aClass, gwtVersion);
				String descr = GwtBundle.message("problem.description.async.remote.service.0.doesn.t.define.corresponding.method", async.getName());
				result.add(manager.createProblemDescriptor(getElementToHighlight(method), descr, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
			}
		}

		return result.toArray(new ProblemDescriptor[result.size()]);
	}

	@Override
	@Nonnull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.inconsistent.gwt.remoteservice");
	}

	@Override
	@Nonnull
	@NonNls
	public String getShortName()
	{
		return "GWTRemoteServiceAsyncCheck";
	}

	private static class CopyMethodToSyncQuickFix extends BaseGwtLocalQuickFix
	{
		private final PsiClass mySync;
		private final PsiMethod myMethod;

		private CopyMethodToSyncQuickFix(final PsiMethod method, final PsiClass sync)
		{
			super(GwtBundle.message("quick.fix.name.create.sync.method.for.async", PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY,
					PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE)));
			mySync = sync;
			myMethod = method;
		}

		@Override
		public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor)
		{
			VirtualFile file = mySync.getContainingFile().getVirtualFile();
			if(file == null || ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(file).hasReadonlyFiles())
			{
				return;
			}
			try
			{
				PsiMethod method = RemoteServiceUtil.copyMethodToSync(myMethod, mySync);
				PsiElement reformatted = CodeStyleManager.getInstance(project).reformat(method);
				OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project, file);
				Editor editor = FileEditorManager.getInstance(project).openTextEditor(fileDescriptor, true);
				if(editor != null)
				{
					editor.getCaretModel().moveToOffset(reformatted.getTextRange().getStartOffset());
					if(reformatted instanceof PsiMethod)
					{
						PsiTypeElement returnTypeElement = ((PsiMethod) reformatted).getReturnTypeElement();
						if(returnTypeElement != null)
						{
							TextRange typeRange = returnTypeElement.getTextRange();
							editor.getSelectionModel().setSelection(typeRange.getStartOffset(), typeRange.getEndOffset());
						}
					}
				}
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}
	}

	private static class CopyMethodToAsyncQuickFix extends BaseGwtLocalQuickFix
	{
		private final PsiClass myAsync;
		private final PsiMethod myMethod;
		private final GwtVersion myGwtVersion;

		private CopyMethodToAsyncQuickFix(final PsiClass async, final PsiMethod method, final GwtVersion gwtVersion)
		{
			super(GwtBundle.message("quick.fix.name.copy.method.to.asynch", PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY,
					PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE)));
			myAsync = async;
			myMethod = method;
			myGwtVersion = gwtVersion;
		}

		@Override
		public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
		{
			if(ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(myAsync.getContainingFile().getVirtualFile()).hasReadonlyFiles())
			{
				return;
			}

			try
			{
				PsiMethod newMethod = RemoteServiceUtil.copyMethodToAsync(myMethod, myAsync, myGwtVersion);
				CodeStyleManager.getInstance(project).reformat(newMethod);
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}
	}


	private static class SynchronizeAllMethodsInAsyncQuickFix extends BaseGwtLocalQuickFix
	{
		private final PsiClass myAsync;
		private final PsiClass myRemoteServiceInterface;
		private final GwtVersion myGwtVersion;


		public SynchronizeAllMethodsInAsyncQuickFix(final PsiClass async, final PsiClass remoteServiceInterface, final GwtVersion gwtVersion)
		{
			super(GwtBundle.message("quick.fix.name.synchronize.all.methods.of.0.with.1", async.getName(), remoteServiceInterface.getName()));
			myAsync = async;
			myRemoteServiceInterface = remoteServiceInterface;
			myGwtVersion = gwtVersion;
		}

		@Override
		public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
		{
			if(ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(myAsync.getContainingFile().getVirtualFile()).hasReadonlyFiles())
			{
				return;
			}

			try
			{
				RemoteServiceUtil.copyAllMethodsToAsync(myRemoteServiceInterface, myAsync, myGwtVersion);
				CodeStyleManager.getInstance(project).reformat(myAsync);
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}
	}

	private static class CreateAsyncClassQuickFix extends BaseGwtLocalQuickFix
	{
		private final PsiClass myRemoteServiceInterface;
		private final GwtVersion myGwtVersion;

		public CreateAsyncClassQuickFix(final PsiClass remoteServiceInterface, final GwtVersion gwtVersion)
		{
			super(GwtBundle.message("quick.fix.name.create.interface.0", remoteServiceInterface.getName()) + RemoteServiceUtil.ASYNC_SUFFIX);
			myRemoteServiceInterface = remoteServiceInterface;
			myGwtVersion = gwtVersion;
		}

		@Override
		public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor)
		{
			try
			{
				PsiJavaFile classFile = (PsiJavaFile) myRemoteServiceInterface.getContainingFile();
				LOG.assertTrue(classFile != null);
				final String name = myRemoteServiceInterface.getName() + RemoteServiceUtil.ASYNC_SUFFIX;
				final PsiTypeParameterList typeParameterList = myRemoteServiceInterface.getTypeParameterList();
				final PsiPackageStatement packageStatement = classFile.getPackageStatement();
				@NonNls StringBuilder source = new StringBuilder();
				source.append(packageStatement != null ? packageStatement.getText() : "").append("\n\n");
				final PsiImportList psiImportList = classFile.getImportList();
				source.append(psiImportList != null ? psiImportList.getText() : "");
				source.append("public interface ").append(name);
				if(typeParameterList != null)
				{
					source.append(typeParameterList.getText());
				}
				source.append("\n{\n}\n");
				PsiJavaFile asyncFile = (PsiJavaFile) PsiFileFactory.getInstance(project).createFileFromText(name + ".java", source.toString());

				PsiClass async = asyncFile.getClasses()[0];
				RemoteServiceUtil.copyAllMethodsToAsync(myRemoteServiceInterface, async, myGwtVersion);

				CodeStyleManager.getInstance(project).reformat(asyncFile);

				final PsiDirectory directory = classFile.getContainingDirectory();
				LOG.assertTrue(directory != null);
				directory.add(asyncFile);
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}
	}

}
