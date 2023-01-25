package com.intellij.gwt.jakartaee.rpc;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtServlet;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.javaee.model.xml.web.Servlet;
import com.intellij.javaee.model.xml.web.ServletMapping;
import com.intellij.javaee.model.xml.web.WebApp;
import consulo.gwt.jakartaee.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.language.psi.PsiManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author nik
 */
public class GwtServletUtil
{
	private GwtServletUtil()
	{
	}

	@Nonnull
	public static String getDefaultServletPath(GwtModule module, String serviceName)
	{
		return "/" + module.getQualifiedName() + "/" + serviceName;
	}

	@Nonnull
	private static String getServletPath(GwtModule module, String serviceName, String serviceImplClassName)
	{
		List<GwtServlet> list = module.getServlets();
		for(GwtServlet servlet : list)
		{
			if(serviceImplClassName.equals(servlet.getServletClass().getValue()))
			{
				String path = servlet.getPath().getValue();
				if(path != null)
				{
					if(!path.startsWith("/"))
					{
						path = "/" + path;
					}
					return path;
				}
			}
		}
		return getDefaultServletPath(module, serviceName);
	}

	public static String getServletUrlPattern(JavaEEGoogleGwtModuleExtension gwtFacet, GwtModule module, String serviceName,
			String serviceImplClassName)
	{
		String base = gwtFacet.getPackagingRelativePath(module);
		if(base.endsWith("/"))
		{
			base = base.substring(0, base.length() - 1);
		}
		return base + getServletPath(module, serviceName, serviceImplClassName);
	}

	private static String getDefaultServletName(final GwtModule gwtModule, final String serviceName)
	{
		return gwtModule.getQualifiedName() + " " + serviceName;
	}

	public static void registerServletForService(JavaEEGoogleGwtModuleExtension gwtFacet, final GwtModule gwtModule, final WebApp root,
												 final PsiClass servletImpl, final String serviceName)
	{
		final Servlet servlet = root.addServlet();
		servlet.getServletClass().setValue(servletImpl);
		final String servletName = getDefaultServletName(gwtModule, serviceName);
		servlet.getServletName().setValue(servletName);

		addServletMapping(root, servlet, getServletUrlPattern(gwtFacet, gwtModule, serviceName, servletImpl.getQualifiedName()));
	}

	public static void addServletMapping(final WebApp root, final Servlet servlet, final String servletUrlPattern)
	{
		final ServletMapping mapping = root.addServletMapping();
		mapping.getServletName().setValue(servlet);
		mapping.addUrlPattern().setValue(servletUrlPattern);
	}

	@Nullable
	public static Servlet findServlet(WebApp root, PsiClass servletImpl)
	{
		final List<Servlet> servlets = root.getServlets();
		final PsiManager psiManager = servletImpl.getManager();
		for(Servlet servlet : servlets)
		{
			if(psiManager.areElementsEquivalent(servletImpl, servlet.getServletClass().getValue()))
			{
				return servlet;
			}
		}
		return null;
	}
}
