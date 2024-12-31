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

package com.intellij.gwt.impl.rpc;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.util.collection.Lists;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

/**
 * @author nik
 */
public class GwtSerializableUtil
{
	@NonNls
	private static final Set<String> STANDARD_SERIALIZABLE_CLASSES = new HashSet<String>(Arrays.asList("java.util.Date", "java.lang.Boolean",
			"java.lang.Byte", "java.lang.Character", "java.lang.Double", "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Short",
			"java.lang.String"));
	@NonNls
	private static final Set<String> COLLECTION_CLASSES = new HashSet<String>(Arrays.asList("java.util.Vector", "java.util.Stack", "java.util.List",
			"java.util.Collection", "java.util.ArrayList", "java.util.Map", "java.util.Set", "java.util.HashMap", "java.util.HashSet"));
	@NonNls
	private static final String IS_SERIALIZABLE_INTERFACE_NAME = "com.google.gwt.user.client.rpc.IsSerializable";

	private GwtSerializableUtil()
	{
	}

	@RequiredReadAction
	public static SerializableChecker createSerializableChecker(GoogleGwtModuleExtension extension, final boolean checkInterfaces)
	{
		GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(extension.getModule());

		GwtVersion version = GwtModuleExtensionUtil.getVersion(extension);

		return new SerializableChecker(version, scope, JavaPsiFacade.getInstance(extension.getModule().getProject()), checkInterfaces);
	}

	public static boolean isCollection(PsiType type)
	{
		return COLLECTION_CLASSES.contains(TypeConversionUtil.erasure(type).getCanonicalText());
	}

	public static boolean hasPublicNoArgConstructor(final PsiClass aClass)
	{
		final PsiMethod[] constructors = aClass.getConstructors();
		if(constructors.length == 0)
		{
			return true;
		}

		PsiMethod constructor = findNoArgConstructor(aClass);
		return constructor != null && constructor.hasModifierProperty(PsiModifier.PUBLIC);
	}

	@Nullable
	public static PsiMethod findNoArgConstructor(@Nonnull PsiClass psiClass)
	{
		for(PsiMethod constructor : psiClass.getConstructors())
		{
			if(constructor.getParameterList().getParametersCount() == 0)
			{
				return constructor;
			}
		}
		return null;
	}

	public static class SerializableChecker
	{
		private final List<PsiClassType> mySerializableMarkerTypes;
		private final List<PsiClass> mySerializableMarkerInterfaces;
		private final GwtVersion myVersion;
		private boolean myCheckInterfaces;
		private PsiClass myIsSerializableInterface;

		private SerializableChecker(GwtVersion version, GlobalSearchScope scope, final JavaPsiFacade javaPsiFacade, final boolean checkInterfaces)
		{
			myVersion = version;
			myCheckInterfaces = checkInterfaces;
			myIsSerializableInterface = javaPsiFacade.findClass(IS_SERIALIZABLE_INTERFACE_NAME, scope);
			PsiClass class1 = myIsSerializableInterface;
			PsiClass class2 = null;
			if(version.isJavaIoSerializableSupported())
			{
				class2 = javaPsiFacade.findClass(Serializable.class.getName(), scope);
			}
			mySerializableMarkerInterfaces = Lists.packNullables(class1, class2);
			mySerializableMarkerTypes = new ArrayList<PsiClassType>();
			for(PsiClass anInterface : mySerializableMarkerInterfaces)
			{
				mySerializableMarkerTypes.add(javaPsiFacade.getElementFactory().createType(anInterface));
			}
		}

		public GwtVersion getVersion()
		{
			return myVersion;
		}

		public List<PsiClass> getSerializableMarkerInterfaces()
		{
			return mySerializableMarkerInterfaces;
		}

		public boolean isSerializable(PsiType type)
		{
			return isSerializable(type, Collections.<PsiType>emptyList());
		}

		public boolean isSerializable(PsiType type, final List<PsiType> typeParameters)
		{
			if(type instanceof PsiPrimitiveType)
			{
				return true;
			}

			if(type instanceof PsiArrayType)
			{
				return isSerializable(((PsiArrayType) type).getComponentType(), typeParameters);
			}

			if(STANDARD_SERIALIZABLE_CLASSES.contains(type.getCanonicalText()))
			{
				return true;
			}

			if(isCollection(type))
			{
				if(myVersion.isGenericsSupported() && type instanceof PsiClassType)
				{
					PsiType[] parameters = ((PsiClassType) type).getParameters();
					if(parameters.length > 0)
					{
						boolean serializable = true;
						for(PsiType parameter : parameters)
						{
							serializable &= isSerializable(parameter);
						}
						if(serializable)
						{
							return true;
						}
					}
				}

				if(!typeParameters.isEmpty() && isSerializableTypes(typeParameters))
				{
					return true;
				}
			}

			for(PsiClassType psiClassType : mySerializableMarkerTypes)
			{
				if(psiClassType.isAssignableFrom(type))
				{
					return true;
				}
			}

			if(type instanceof PsiClassType)
			{
				PsiClass psiClass = ((PsiClassType) type).resolve();
				if(psiClass instanceof PsiTypeParameter || !myCheckInterfaces && psiClass != null && psiClass.isInterface())
				{
					return true;
				}
			}
			return false;
		}

		private boolean isSerializableTypes(final List<PsiType> typeParameters)
		{
			for(PsiType type : typeParameters)
			{
				if(!isSerializable(type))
				{
					return false;
				}
			}
			return true;
		}

		public boolean isGwtSerializable(@Nonnull PsiClass psiClass)
		{
			return myIsSerializableInterface != null && psiClass.isInheritor(myIsSerializableInterface, true);
		}

		public boolean isMarkedSerializable(final @Nonnull PsiClass psiClass)
		{
			for(PsiClass markerInterface : mySerializableMarkerInterfaces)
			{
				if(psiClass.isInheritor(markerInterface, true))
				{
					return true;
				}
			}
			return false;
		}

		public String getPresentableSerializableClassesString()
		{
			if(mySerializableMarkerInterfaces.isEmpty())
			{
				return "";
			}

			String name1 = mySerializableMarkerInterfaces.get(0).getQualifiedName();
			if(mySerializableMarkerInterfaces.size() == 1)
			{
				return "'" + name1 + "'";
			}
			String name2 = mySerializableMarkerInterfaces.get(1).getQualifiedName();
			return GwtBundle.message("text.0.or.1", name1, name2);
		}
	}
}
