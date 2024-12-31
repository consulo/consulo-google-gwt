package com.intellij.gwt.impl.references;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public abstract class GwtModuleReference<T extends PsiElement> extends PsiReferenceBase<T>
{
	private GwtModulesManager myGwtModulesManager;

	public GwtModuleReference(T element)
	{
		super(element);
		myGwtModulesManager = GwtModulesManager.getInstance(element.getProject());
	}

	@Nullable
	public Module getModule()
	{
		return ModuleUtilCore.findModuleForPsiElement(myElement);
	}

	@Override
	public Object[] getVariants()
	{
		List<String> names = new ArrayList<String>();
		for(GwtModule module : myGwtModulesManager.getAllGwtModules())
		{
			names.add(module.getQualifiedName());
		}
		return ArrayUtil.toStringArray(names);
	}

	@Override
	@Nullable
	public PsiElement resolve()
	{
		String moduleName = getStringValue();
		if(moduleName != null)
		{
			final Module module = getModule();
			final GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module) : GlobalSearchScope.allScope
					(myElement.getProject());
			final GwtModule gwtModule = myGwtModulesManager.findGwtModuleByName(moduleName, scope);
			if(gwtModule != null)
			{
				return gwtModule.getXmlTag();
			}
		}
		return null;
	}

	@Nullable
	protected abstract String getStringValue();
}
