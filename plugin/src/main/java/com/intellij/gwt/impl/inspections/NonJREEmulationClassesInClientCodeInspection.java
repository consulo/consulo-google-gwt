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

package com.intellij.gwt.impl.inspections;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.base.inspections.BaseGwtInspection;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.java.language.psi.JavaRecursiveElementVisitor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.javadoc.PsiDocComment;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.gwt.base.module.extension.path.GwtSdkUtil;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
@ExtensionImpl
public class NonJREEmulationClassesInClientCodeInspection extends BaseGwtInspection
{
	@Override
	@Nonnull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.classes.not.from.jre.emulation.library.in.client.code");
	}

	@Override
	@Nonnull
	@NonNls
	public String getShortName()
	{
		return "NonJREEmulationClassesInClientCode";
	}

	@Override
	@Nullable
	public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull final InspectionManager manager, boolean isOnTheFly, Object state)
	{
		if(!shouldCheck(file))
		{
			return null;
		}

		final GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(file.getProject());
		final VirtualFile virtualFile = file.getVirtualFile();
		if(virtualFile == null)
		{
			return null;
		}

		final List<GwtModule> gwtModules = gwtModulesManager.findGwtModulesByClientSourceFile(virtualFile);
		if(gwtModules.isEmpty())
		{
			return null;
		}

		final GoogleGwtModuleExtension extension = GwtModuleExtensionUtil.findModuleExtension(file.getProject(), virtualFile);
		if(extension == null)
		{
			return null;
		}

		final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();

		file.accept(new JavaRecursiveElementVisitor()
		{
			@Override
			public void visitDocComment(final PsiDocComment comment)
			{
			}

			@Override
			public void visitReferenceElement(PsiJavaCodeReferenceElement reference)
			{
				final PsiElement resolved = reference.resolve();
				if(resolved instanceof PsiClass)
				{
					PsiClass referencedClass = (PsiClass) resolved;
					if(referencedClass.isAnnotationType())
					{
						return;
					}

					String className = referencedClass.getQualifiedName();

					final PsiFile psiFile = referencedClass.getContainingFile();
					if(psiFile != null)
					{
						final VirtualFile vFile = psiFile.getVirtualFile();
						if(vFile != null)
						{
							List<GwtModule> referencedModules = gwtModulesManager.findGwtModulesByClientSourceFile(vFile);
							if(referencedModules.isEmpty())
							{
								referencedModules = gwtModulesManager.findModulesByClass(reference, referencedClass.getQualifiedName());
							}

							boolean inherited = true;
							for(GwtModule gwtModule : gwtModules)
							{
								inherited &= gwtModulesManager.isInheritedOrSelf(gwtModule, referencedModules);

								if(!inherited && !referencedModules.isEmpty())
								{
									GwtModule referencedModule = referencedModules.get(0);
									final String message = GwtBundle.message("problem.description.class.0.is.defined.in.module.1.which.is.not.inherited.in.module.2", className,
											referencedModule.getQualifiedName(), gwtModule.getQualifiedName());
									problems.add(manager.createProblemDescriptor(reference, message, new InheritModuleQuickFix(gwtModule, referencedModule),
											ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
									return;
								}
							}

							if(inherited)
							{
								return;
							}
						}
					}

					PsiClass topLevelClass = PsiUtil.getTopLevelClass(referencedClass);
					if(topLevelClass == null)
					{
						topLevelClass = referencedClass;
					}

					if(!containsJreEmulationClass(extension, topLevelClass.getQualifiedName()))
					{
						final String message = GwtBundle.message("problem.description.class.0.is.not.presented.in.jre.emulation.library", className);
						problems.add(manager.createProblemDescriptor(reference, message, ((LocalQuickFix) null), ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
					}
				}
				super.visitReferenceElement(reference);
			}
		});

		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}

	public static boolean containsJreEmulationClass(GoogleGwtModuleExtension<?> extension, String className)
	{
		GwtLibraryPathProvider.Info info = GwtLibraryPathProvider.EP_NAME.computeSafeIfAny(it -> it.resolveInfo(extension));
		assert info != null;
		VirtualFile userJar = info.getUserJar();
		if(userJar == null)
		{
			return true;
		}

		VirtualFile emulFile = userJar.findFileByRelativePath(GwtSdkUtil.getJreEmulationClassPath(className));
		return emulFile != null;
	}

	private static class InheritModuleQuickFix extends BaseGwtLocalQuickFix
	{
		private GwtModule myGwtModule;
		private GwtModule myReferencedModule;

		public InheritModuleQuickFix(final GwtModule gwtModule, final GwtModule referencedModule)
		{
			super(GwtBundle.message("quick.fix.name.inherit.module.0.from.1", gwtModule.getQualifiedName(), referencedModule.getQualifiedName()));
			myGwtModule = gwtModule;
			myReferencedModule = referencedModule;
		}

		@Override
		public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor problemDescriptor)
		{
			if(!ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(myGwtModule.getModuleFile()).hasReadonlyFiles())
			{
				myGwtModule.addInherits().getName().setValue(myReferencedModule.getQualifiedName());
			}
		}
	}
}
