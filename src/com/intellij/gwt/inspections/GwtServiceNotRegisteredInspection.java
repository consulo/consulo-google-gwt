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

import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredReadAction;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;

/**
 * @author nik
 */
public class GwtServiceNotRegisteredInspection extends BaseGwtInspection
{
	@Override
	@NotNull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.gwt.remote.service.is.not.registered.in.web.xml");
	}

	@Override
	@NotNull
	@NonNls
	public String getShortName()
	{
		return "GwtServiceNotRegistered";
	}

	@Override
	@Nullable
	public ProblemDescriptor[] checkFile(@NotNull final PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly)
	{
		/*if(!DeploymentDescriptorsConstants.WEB_XML_META_DATA.getFileName().equals(file.getName()))
		{
			return null;
		}

		WebFacet webFacet = WebUtil.getWebFacet(file);
		if(webFacet == null)
		{
			return null;
		}

		GwtFacet gwtFacet = GwtFacet.getInstance(webFacet.getModule());
		if(gwtFacet == null)
		{
			return null;
		}

		WebApp webApp = webFacet.getRoot();
		if(webApp == null)
		{
			return null;
		}

		Map<Servlet, String> expectedUrlPatterns = new HashMap<Servlet, String>();
		Map<Servlet, String> serviceNames = new HashMap<Servlet, String>();
		for(Servlet servlet : webApp.getServlets())
		{
			PsiClass servletClass = servlet.getServletClass().getValue();
			if(servletClass == null || !RemoteServiceUtil.isRemoteServiceImplementation(servletClass))
			{
				continue;
			}

			PsiClass serviceInterface = RemoteServiceUtil.findRemoteServiceInterface(servletClass);
			if(serviceInterface == null)
			{
				continue;
			}

			PsiFile psiFile = serviceInterface.getContainingFile();
			if(psiFile == null)
			{
				continue;
			}

			VirtualFile virtualFile = psiFile.getVirtualFile();
			if(virtualFile == null)
			{
				continue;
			}

			GwtModule gwtModule = GwtModulesManager.getInstance(serviceInterface.getProject()).findGwtModuleByClientSourceFile(virtualFile);
			if(gwtModule == null)
			{
				continue;
			}

			String serviceName = serviceInterface.getName();
			expectedUrlPatterns.put(servlet, GwtServletUtil.getServletUrlPattern(gwtFacet, gwtModule, serviceName, servletClass.getQualifiedName()));
			serviceNames.put(servlet, serviceName);
		}

		Map<Servlet, ServletMapping> singleMappings = new HashMap<Servlet, ServletMapping>();
		for(ServletMapping mapping : webApp.getServletMappings())
		{
			Servlet servlet = mapping.getServletName().getValue();
			if(servlet != null)
			{
				String urlPattern = expectedUrlPatterns.get(servlet);

				if(singleMappings.containsKey(servlet))
				{
					singleMappings.remove(servlet);
				}
				else
				{
					singleMappings.put(servlet, mapping);
				}
				if(urlPattern != null && containsUrlPattern(mapping, urlPattern))
				{
					expectedUrlPatterns.remove(servlet);
				}
			}
		}

		if(expectedUrlPatterns.isEmpty())
		{
			return null;
		}      */

		List<ProblemDescriptor> problems = new SmartList<ProblemDescriptor>();
		/*
		for(Servlet servlet : expectedUrlPatterns.keySet())
		{
			ServletMapping mapping = singleMappings.get(servlet);
			String serviceName = serviceNames.get(servlet);
			String urlPattern = expectedUrlPatterns.get(servlet);
			AddServletMappingFix quickfix = new AddServletMappingFix(webApp, servlet, serviceName, urlPattern, mapping);
			List<GenericDomValue<String>> urlPatterns = mapping != null ? mapping.getUrlPatterns() : Collections.<GenericDomValue<String>>emptyList();
			if(urlPatterns.size() != 1)
			{
				String message = GwtBundle.message("problem.description.correct.servlet.mapping.is.not.specified.for.remote.service.0", serviceName);
				XmlToken token = ObjectUtils.assertNotNull(XmlTagUtil.getStartTagNameElement(ObjectUtils.assertNotNull(servlet.getXmlTag())));
				problems.add(manager.createProblemDescriptor(token, message, quickfix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
			}
			else
			{
				XmlTag tag = ObjectUtils.assertNotNull(urlPatterns.get(0).getXmlTag());
				String message = GwtBundle.message("problem.description.incorrect.servlet.mapping.for.remote.service.0", serviceName);
				problems.add(manager.createProblemDescriptor(tag, XmlTagUtil.getTrimmedValueRange(tag), message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
						quickfix));
			}
		}
             */
		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}
   /*
	private static boolean containsUrlPattern(final ServletMapping mapping, final String urlPattern)
	{
		for(GenericDomValue<String> pattern : mapping.getUrlPatterns())
		{
			if(urlPattern.equals(pattern.getValue()))
			{
				return true;
			}
		}
		return false;
	}        */

	@RequiredReadAction
	@Override
	@Nullable
	public ProblemDescriptor[] checkClassImpl(@NotNull GoogleGwtModuleExtension extension, @NotNull GwtVersion version, @NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly)
	{
	/*	if(!shouldCheck(aClass))
		{
			return null;
		}

		final Project project = manager.getProject();
		if(!RemoteServiceUtil.isRemoteServiceImplementation(aClass))
		{
			return null;
		}

		final PsiClass service = RemoteServiceUtil.findRemoteServiceInterface(aClass);
		if(service == null)
		{
			return null;
		}

		GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(project);
		final VirtualFile virtualFile = service.getContainingFile().getVirtualFile();
		if(virtualFile == null)
		{
			return null;
		}

		GwtModule gwtModule = gwtModulesManager.findGwtModuleByClientSourceFile(virtualFile);
		if(gwtModule == null)
		{
			return null;
		}

		final Module module = gwtModule.getModule();
		if(module == null)
		{
			return null;
		}

		GwtFacet facet = GwtFacet.findFacetBySourceFile(project, gwtModule.getModuleFile());
		if(facet == null)
		{
			return null;
		}

		final WebFacet webFacet = facet.getWebFacet();
		if(webFacet == null)
		{
			return null;
		}

		final WebApp webApp = webFacet.getRoot();
		if(webApp == null)
		{
			return null;
		}

		String serviceName = service.getName();
		Servlet servlet = GwtServletUtil.findServlet(webApp, aClass);
		if(servlet == null)
		{
			PsiElement place = aClass.getNameIdentifier();
			if(place == null)
			{
				place = aClass;
			}
			String message = GwtBundle.message("problem.description.remote.service.is.not.registered.as.a.servlet.in.web.xml", serviceName);
			RegisterServiceQuickFix quickFix = new RegisterServiceQuickFix(facet, gwtModule, webApp, aClass, serviceName);
			return new ProblemDescriptor[]{manager.createProblemDescriptor(place, message, quickFix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)};
		}
		   */
		return null;
	}

	/*private static class RegisterServiceQuickFix extends BaseGwtLocalQuickFix
	{
		private final GwtFacet myFacet;
		private GwtModule myGwtModule;
		private WebApp myWebApp;
		private PsiClass myServiceImpl;
		private String myServiceName;

		public RegisterServiceQuickFix(final GwtFacet facet, final GwtModule gwtModule, final WebApp webApp, final PsiClass serviceImpl,
				final String serviceName)
		{
			super(GwtBundle.message("quickfix.name.register.remote.service.0.in.web.xml", serviceName));
			myFacet = facet;
			myGwtModule = gwtModule;
			myWebApp = webApp;
			myServiceImpl = serviceImpl;
			myServiceName = serviceName;
		}

		@Override
		public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor problemDescriptor)
		{
			if(!ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(myWebApp.getRoot().getFile().getVirtualFile()).hasReadonlyFiles())
			{
				GwtServletUtil.registerServletForService(myFacet, myGwtModule, myWebApp, myServiceImpl, myServiceName);
			}
		}
	}

	private static class AddServletMappingFix implements LocalQuickFix
	{
		private WebApp myRoot;
		private Servlet myServlet;
		private String myServiceName;
		private String myUrlPattern;
		private final ServletMapping myExistingMapping;

		public AddServletMappingFix(final WebApp root, final Servlet servlet, final String serviceName, final String urlPattern,
				ServletMapping existentMapping)
		{
			myRoot = root;
			myServlet = servlet;
			myServiceName = serviceName;
			myUrlPattern = urlPattern;
			myExistingMapping = existentMapping;
		}

		@Override
		@NotNull
		public String getName()
		{
			return myExistingMapping == null || myExistingMapping.getUrlPatterns().size() > 1 ? GwtBundle.message("quickfix.name.add.servlet.mapping.for" +
					".remote.service.0", myServiceName) : GwtBundle.message("quickfix.name.set.correct.servlet.mapping.for.remote.service.0", myServiceName);
		}

		@Override
		@NotNull
		public String getFamilyName()
		{
			return getName();
		}

		@Override
		public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor descriptor)
		{
			if(myExistingMapping == null)
			{
				GwtServletUtil.addServletMapping(myRoot, myServlet, myUrlPattern);
			}
			else
			{
				List<GenericDomValue<String>> urlPatterns = myExistingMapping.getUrlPatterns();
				if(urlPatterns.size() == 1)
				{
					urlPatterns.get(0).setValue(myUrlPattern);
				}
				else
				{
					myExistingMapping.addUrlPattern().setValue(myUrlPattern);
				}
			}
		}
	}   */
}
