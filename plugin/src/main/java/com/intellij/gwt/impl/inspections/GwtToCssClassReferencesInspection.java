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

import com.intellij.gwt.base.inspections.BaseGwtInspection;
import consulo.annotation.component.ExtensionImpl;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

@ExtensionImpl
public class GwtToCssClassReferencesInspection extends BaseGwtInspection<Object> {
    private static final Logger LOG = Logger.getInstance(GwtToCssClassReferencesInspection.class);

    @Nonnull
    @Override
    public PsiElementVisitor buildVisitorImpl(@Nonnull ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession session, Object o) {
        /*Project project = holder.getManager().getProject();
        if(hasGwtFacets(project))
		{
			return new CssReferencesProblemsCollectingVisitor(holder);
		}                                           */
        return super.buildVisitorImpl(holder, isOnTheFly, session, o);
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return GwtLocalize.inspectionUnresolvedReferencesToCssClassesDisplayName();
    }

    @Override
    @Nonnull
    @NonNls
    public String getShortName() {
        return "GWTStyleCheck";
    }

	/*private static class MyLocalQuickFix extends BaseGwtLocalQuickFix
	{
		private CssFile myCssFile;
		private String myClassName;

		public MyLocalQuickFix(PsiLiteralExpression expression, final CssFile cssFile, final String className)
		{
			super(GwtBundle.message("quick.fix.create.css.class.name", expression.getText()));
			myCssFile = cssFile;
			myClassName = className;
		}

		@Override
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor)
		{
			VirtualFile virtualFile = myCssFile.getVirtualFile();
			if(virtualFile == null)
			{
				PsiFile originalFile = myCssFile.getOriginalFile();
				if(originalFile != null)
				{
					virtualFile = originalFile.getVirtualFile();
				}
				if(virtualFile == null)
				{
					return;
				}
			}

			if(ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFile).hasReadonlyFiles())
			{
				return;
			}

			try
			{
				CssStylesheet stylesheet = myCssFile.getStylesheet();
				CssRuleset cssRuleset = CssElementFactory.getInstance(project).createRuleset("." + myClassName + "{\n\n}\n");
				stylesheet.addRuleset(cssRuleset);
				final CssRuleset[] rulesets = stylesheet.getRulesets();
				final CssRuleset added = rulesets[rulesets.length - 1];
				final OpenFileDescriptor fileDescriptor = new OpenFileDescriptor(project, virtualFile, added.getBlock().getTextOffset());
				FileEditorManager.getInstance(project).openEditor(fileDescriptor, true);
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}
	}

	private static class CssReferencesProblemsCollectingVisitor extends JavaElementVisitor
	{
		private final ProblemsHolder myProblemsHolder;

		public CssReferencesProblemsCollectingVisitor(ProblemsHolder problemsHolder)
		{
			myProblemsHolder = problemsHolder;
		}

		@Override
		public void visitLiteralExpression(PsiLiteralExpression expression)
		{
			final GwtToCssClassReference reference = findUnresolvedReference(expression);
			if(reference != null)
			{
				addProblem(expression, reference);
			}
		}

		@Nullable
		private static GwtToCssClassReference findUnresolvedReference(final PsiLiteralExpression expression)
		{
			final PsiReference[] references = expression.getReferences();
			for(PsiReference reference : references)
			{
				if(reference instanceof GwtToCssClassReference && reference.resolve() == null)
				{
					return (GwtToCssClassReference) reference;
				}
			}
			return null;
		}

		private void addProblem(final PsiLiteralExpression expression, final GwtToCssClassReference reference)
		{
			MyLocalQuickFix fix = null;
			final GwtModule module = reference.findGwtModule();
			if(module != null)
			{
				CssFile cssFile = GwtModulesManager.getInstance(expression.getProject()).findPreferableCssFile(module);
				String className = String.valueOf(expression.getValue());
				if(cssFile != null && !className.contains("."))
				{
					fix = new MyLocalQuickFix(expression, cssFile, className);
				}
			}
			myProblemsHolder.registerProblem(expression, GwtBundle.message("problem.description.unknown.css.class", expression.getValue()),
					ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, fix);
		}

		@Override
		public void visitReferenceExpression(PsiReferenceExpression expression)
		{
		}
	}  */
}
