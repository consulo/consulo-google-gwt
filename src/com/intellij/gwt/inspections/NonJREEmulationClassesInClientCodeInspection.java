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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.google.gwt.module.extension.GoogleGwtModuleExtension;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiUtil;

/**
 * @author nik
 */
public class NonJREEmulationClassesInClientCodeInspection extends BaseGwtInspection
{
	@Override
	@NotNull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.classes.not.from.jre.emulation.library.in.client.code");
	}

	@Override
	@NotNull
	@NonNls
	public String getShortName()
	{
		return "NonJREEmulationClassesInClientCode";
	}

	@Override
	@Nullable
	public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, boolean isOnTheFly)
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

		final GoogleGwtModuleExtension gwtFacet = GwtFacet.findFacetBySourceFile(file.getProject(), virtualFile);
		if(gwtFacet == null || gwtFacet.getSdk() == null)
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
									final String message = GwtBundle.message("problem.description.class.0.is.defined.in.module.1.which.is.not.inherited.in.module.2",
											className, referencedModule.getQualifiedName(), gwtModule.getQualifiedName());
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
					if(!containsJreEmulationClass(gwtFacet.getSdk(), topLevelClass.getQualifiedName()))
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

	public static boolean containsJreEmulationClass(Sdk sdk, String className)
	{
		VirtualFile userJar = GwtSdkUtil.getUserJar(sdk);
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
		public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor problemDescriptor)
		{
			if(!ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(myGwtModule.getModuleFile()).hasReadonlyFiles())
			{
				myGwtModule.addInherits().getName().setValue(myReferencedModule.getQualifiedName());
			}
		}
	}
}
