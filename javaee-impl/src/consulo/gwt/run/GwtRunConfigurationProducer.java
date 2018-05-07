/*
 * Copyright 2013-2016 must-be.org
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

package consulo.gwt.run;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.run.GwtRunConfiguration;
import com.intellij.gwt.run.GwtRunConfigurationEditor;
import com.intellij.gwt.run.GwtRunConfigurationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.xml.XmlFile;
import consulo.gwt.module.extension.GwtModuleExtensionUtil;

/**
 * @author VISTALL
 * @since 02-Jun-16
 */
public class GwtRunConfigurationProducer extends RunConfigurationProducer<GwtRunConfiguration>
{
	public GwtRunConfigurationProducer()
	{
		super(GwtRunConfigurationType.getInstance());
	}

	@Override
	protected boolean setupConfigurationFromContext(GwtRunConfiguration runConfiguration, ConfigurationContext configurationContext, Ref<PsiElement> ref)
	{
		Pair<GwtModule, String> pair = findGwtModule(configurationContext.getLocation());
		if(pair != null)
		{
			GwtModule gwtModule = pair.getFirst();
			String path = GwtRunConfigurationEditor.getPath(gwtModule, pair.getSecond());
			runConfiguration.setModule(gwtModule.getModule());
			runConfiguration.setPage(path);
			return true;
		}
		return false;
	}

	@Override
	public boolean isConfigurationFromContext(GwtRunConfiguration runConfiguration, ConfigurationContext configurationContext)
	{
		Pair<GwtModule, String> pair = findGwtModule(configurationContext.getLocation());
		if(pair != null)
		{
			String pagePath1 = runConfiguration.getPage();
			Module module1 = runConfiguration.getModule();

			GwtModule gwtModule = pair.getFirst();
			String pagePath2 = GwtRunConfigurationEditor.getPath(gwtModule, pair.getSecond());
			Module module2 = gwtModule.getModule();
			return pagePath2.equals(pagePath1) && Comparing.equal(module1, module2);
		}
		return false;
	}

	@Nullable
	private static Pair<GwtModule, String> findGwtModule(Location<?> location)
	{
		PsiFile psiFile = location.getPsiElement().getContainingFile();
		if(psiFile == null)
		{
			return null;
		}

		VirtualFile file = psiFile.getVirtualFile();
		if(file == null || !GwtModuleExtensionUtil.hasModuleExtension(location.getProject(), file))
		{
			return null;
		}

		GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(location.getProject());
		GwtModule gwtModule = gwtModulesManager.getGwtModuleByXmlFile(psiFile);
		if(gwtModule != null)
		{
			return getModuleWithFile(gwtModulesManager, gwtModule);
		}

		if(psiFile instanceof PsiJavaFile)
		{
			PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
			if(classes.length == 1)
			{
				PsiClass psiClass = classes[0];
				GwtModule module = gwtModulesManager.findGwtModuleByEntryPoint(psiClass);
				if(module != null)
				{
					return getModuleWithFile(gwtModulesManager, module);
				}
			}
		}
		return null;
	}

	@Nullable
	private static Pair<GwtModule, String> getModuleWithFile(@Nonnull GwtModulesManager gwtModulesManager, @Nonnull GwtModule gwtModule)
	{
		XmlFile psiHtmlFile = gwtModulesManager.findHtmlFileByModule(gwtModule);
		if(psiHtmlFile != null)
		{
			VirtualFile htmlFile = psiHtmlFile.getVirtualFile();
			if(htmlFile != null)
			{
				String path = gwtModulesManager.getPathFromPublicRoot(gwtModule, htmlFile);
				if(path != null)
				{
					return Pair.create(gwtModule, path);
				}
			}
		}
		return null;
	}
}
