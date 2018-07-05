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

package com.intellij.gwt.rpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NonNls;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author nik
 */
public class RemoteServiceUtil
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.rpc.RemoteServiceUtil");
	@NonNls
	private static final String REMOTE_SERVICE_INTERFACE_NAME = "com.google.gwt.user.client.rpc.RemoteService";
	@NonNls
	public static final String ASYNC_CALLBACK_INTERFACE_NAME = "com.google.gwt.user.client.rpc.AsyncCallback";
	@NonNls
	private static final String REMOTE_SERVICE_SERVLET_NAME = "com.google.gwt.user.server.rpc.RemoteServiceServlet";
	@NonNls
	public static final String ASYNC_SUFFIX = "Async";
	@NonNls
	public static final String IMPL_SERVICE_SUFFIX = "Impl";
	@NonNls
	private static final String VOID_CLASS_NAME = "java.lang.Void";

	private RemoteServiceUtil()
	{
	}

	public static boolean isMethodPresentedInAsync(@Nonnull PsiMethod method, @Nonnull PsiClass async)
	{
		return findMethodInAsync(method, async) != null;
	}

	public static boolean isMethodPresentedInSync(@Nonnull PsiMethod asyncMethod, @Nonnull PsiClass sync)
	{
		return findMethodInSync(asyncMethod, sync) != null;
	}

	@Nullable
	public static PsiMethod findMethodInSync(final PsiMethod asyncMethod, final PsiClass sync)
	{
		PsiParameter[] asyncParameters = asyncMethod.getParameterList().getParameters();

		for(PsiMethod method : sync.findMethodsByName(asyncMethod.getName(), false))
		{
			PsiParameter[] syncParameters = method.getParameterList().getParameters();

			if(areParametersCorresponding(syncParameters, asyncParameters, method.getReturnType()))
			{
				return method;
			}
		}

		return null;
	}

	@Nullable
	public static PsiMethod findMethodInAsync(@Nonnull PsiMethod method, @Nonnull PsiClass async)
	{
		PsiParameter[] parameters = method.getParameterList().getParameters();

		for(PsiMethod asyncMethod : async.findMethodsByName(method.getName(), false))
		{
			final PsiType returnType = asyncMethod.getReturnType();
			if(returnType == null || !returnType.equals(PsiType.VOID))
			{
				continue;
			}

			PsiParameter[] asyncParams = asyncMethod.getParameterList().getParameters();
			if(areParametersCorresponding(parameters, asyncParams, method.getReturnType()))
			{
				return asyncMethod;
			}
		}
		return null;
	}

	private static boolean areParametersCorresponding(final PsiParameter[] syncParameters, final PsiParameter[] asyncParameters,
			@Nullable PsiType syncReturnType)
	{
		if(asyncParameters.length != syncParameters.length + 1)
		{
			return false;
		}

		for(int i = 0; i != syncParameters.length; ++i)
		{
			if(!areErasuresEqual(syncParameters[i].getType(), asyncParameters[i].getType()))
			{
				return false;
			}
		}

		PsiParameter lastParameter = asyncParameters[syncParameters.length];
		PsiType lastParameterType = lastParameter.getType();
		if(!(lastParameterType instanceof PsiClassType))
		{
			return false;
		}
		final PsiClassType lastClassType = (PsiClassType) lastParameterType;
		PsiClassType.ClassResolveResult resolveResult = lastClassType.resolveGenerics();
		PsiClass psiClass = resolveResult.getElement();
		if(psiClass == null || !ASYNC_CALLBACK_INTERFACE_NAME.equals(psiClass.getQualifiedName()))
		{
			return false;
		}
		if(PsiClassType.isRaw(resolveResult) || psiClass.getTypeParameters().length == 0)
		{
			return true;
		}

		PsiType actualTypeParameter = resolveResult.getSubstitutor().substitute(psiClass.getTypeParameters()[0]);
		if(syncReturnType instanceof PsiPrimitiveType)
		{
			PsiClassType boxedType = getBoxedType((PsiPrimitiveType) syncReturnType, lastParameter.getResolveScope(), lastParameter.getProject());
			if(boxedType != null)
			{
				syncReturnType = boxedType;
			}
		}

		return actualTypeParameter != null && syncReturnType != null && areErasuresEqual(syncReturnType, actualTypeParameter);
	}

	private static boolean areErasuresEqual(@Nonnull PsiType t1, @Nonnull PsiType t2)
	{
		final PsiType type1 = TypeConversionUtil.erasure(t1);
		final PsiType type2 = TypeConversionUtil.erasure(t2);
		return type1.equals(type2);
	}

	public static boolean isRemoteServiceInterface(final @Nullable PsiClass aClass)
	{
		if(aClass == null || !aClass.isInterface())
		{
			return false;
		}

		final PsiClass remoteService = JavaPsiFacade.getInstance(aClass.getProject()).findClass(REMOTE_SERVICE_INTERFACE_NAME, aClass.getResolveScope());

		return remoteService != null && aClass.isInheritor(remoteService, true);
	}

	public static boolean isRemoteServiceImplementation(final @Nullable PsiClass aClass)
	{
		if(aClass == null || aClass.isInterface())
		{
			return false;
		}

		final PsiClass servlet = JavaPsiFacade.getInstance(aClass.getProject()).findClass(REMOTE_SERVICE_SERVLET_NAME, aClass.getResolveScope());
		return servlet != null && aClass.isInheritor(servlet, true);
	}

	public static
	@Nullable
	PsiClass findRemoteServiceInterface(PsiClass serviceImpl)
	{
		final PsiClass[] interfaces = serviceImpl.getInterfaces();
		for(PsiClass anInterface : interfaces)
		{
			if(isRemoteServiceInterface(anInterface))
			{
				return anInterface;
			}
		}
		return null;
	}

	public static
	@Nullable
	PsiClass findSynchronousInterface(final PsiClass asynchInterface)
	{
		String name = asynchInterface.getQualifiedName();
		if(name == null || !name.endsWith(ASYNC_SUFFIX))
		{
			return null;
		}

		final PsiManager psiManager = asynchInterface.getManager();
		final GlobalSearchScope scope = asynchInterface.getResolveScope();
		final PsiClass remoteService = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(REMOTE_SERVICE_INTERFACE_NAME, scope);
		if(remoteService == null)
		{
			return null;
		}


		String syncName = name.substring(0, name.length() - ASYNC_SUFFIX.length());
		final PsiClass sync = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(syncName, scope);
		if(sync != null && sync.isInheritor(remoteService, true))
		{
			return sync;
		}
		return null;
	}

	public static
	@Nullable
	PsiClass findAsynchronousInterface(PsiClass aClass)
	{
		return JavaPsiFacade.getInstance(aClass.getProject()).findClass(aClass.getQualifiedName() + ASYNC_SUFFIX, aClass.getResolveScope());
	}

	@Nullable
	public static PsiMethod findAsynchronousMethod(@Nonnull PsiMethod method)
	{
		PsiClass psiClass = method.getContainingClass();
		if(psiClass == null || !isRemoteServiceInterface(psiClass))
		{
			return null;
		}

		PsiClass asyncClass = findAsynchronousInterface(psiClass);
		if(asyncClass == null)
		{
			return null;
		}

		return findMethodInAsync(method, asyncClass);
	}

	public static void copyAllMethodsToAsync(@Nonnull PsiClass sync, @Nonnull PsiClass async, @Nonnull GwtVersion gwtVersion) throws
			IncorrectOperationException
	{
		PsiElementFactory elementFactory = JavaPsiFacade.getInstance(sync.getProject()).getElementFactory();

		for(PsiMethod method : async.getMethods())
		{
			method.delete();
		}

		for(PsiMethod method : sync.getMethods())
		{
			copyMethodToAsync(method, async, elementFactory, gwtVersion);
		}
	}

	public static PsiMethod copyMethodToAsync(@Nonnull PsiMethod method, @Nonnull PsiClass async, @Nonnull GwtVersion gwtVersion) throws
			IncorrectOperationException
	{
		return copyMethodToAsync(method, async, JavaPsiFacade.getInstance(method.getProject()).getElementFactory(), gwtVersion);
	}

	private static PsiMethod copyMethodToAsync(final @Nonnull PsiMethod method, final @Nonnull PsiClass async,
			final @Nonnull PsiElementFactory elementFactory, final @Nonnull GwtVersion gwtVersion) throws IncorrectOperationException
	{
		final PsiType asyncCallbackType;
		if(!gwtVersion.isGenericsSupported())
		{
			asyncCallbackType = createAsynchCallbackType(async, null);
		}
		else
		{
			asyncCallbackType = createAsynchCallbackType(async, method.getReturnType());
		}
		PsiMethod newMethod = (PsiMethod) method.copy();
		newMethod.getParameterList().add(elementFactory.createParameter("async", asyncCallbackType));
		final PsiReferenceList throwsList = newMethod.getThrowsList();
		for(PsiJavaCodeReferenceElement element : throwsList.getReferenceElements())
		{
			element.delete();
		}
		GwtGenericsUtil.removeTypeArgsJavadocTags(newMethod);
		final PsiTypeElement returnTypeElement = newMethod.getReturnTypeElement();
		assert returnTypeElement != null;
		returnTypeElement.replace(elementFactory.createTypeElement(PsiType.VOID));
		return (PsiMethod) async.add(newMethod);
	}

	public static PsiClassType createAsynchCallbackType(@Nonnull PsiElement context, @Nullable PsiType parameter)
	{
		Project project = context.getProject();
		JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
		GlobalSearchScope scope = context.getResolveScope();
		PsiClass asyncCallbackClass = psiFacade.findClass(ASYNC_CALLBACK_INTERFACE_NAME, scope);
		PsiElementFactory elementFactory = psiFacade.getElementFactory();
		if(asyncCallbackClass == null)
		{
			return elementFactory.createTypeByFQClassName(ASYNC_CALLBACK_INTERFACE_NAME, scope);
		}

		PsiTypeParameter[] typeParameters = asyncCallbackClass.getTypeParameters();
		if(parameter instanceof PsiPrimitiveType)
		{
			parameter = getBoxedType((PsiPrimitiveType) parameter, scope, project);
		}
		if(parameter == null || typeParameters.length == 0)
		{
			return elementFactory.createType(asyncCallbackClass);
		}
		return elementFactory.createType(asyncCallbackClass, PsiSubstitutor.EMPTY.put(typeParameters[0], parameter));
	}

	@Nullable
	private static PsiClassType getBoxedType(@Nonnull PsiPrimitiveType type, @Nonnull GlobalSearchScope scope, @Nonnull Project project)
	{
		if(TypeConversionUtil.isVoidType(type))
		{
			return JavaPsiFacade.getInstance(project).getElementFactory().createTypeByFQClassName(VOID_CLASS_NAME, scope);
		}
		return type.getBoxedType(PsiManager.getInstance(project), scope);
	}

	public static PsiMethod copyMethodToSync(final PsiMethod method, final PsiClass sync) throws IncorrectOperationException
	{
		Project project = method.getProject();
		PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
		PsiMethod newMethod = (PsiMethod) method.copy();
		PsiParameter[] parameters = newMethod.getParameterList().getParameters();
		PsiType newReturnType = PsiType.getJavaLangObject(PsiManager.getInstance(project), sync.getResolveScope());
		if(parameters.length > 0)
		{
			PsiParameter callbackParameter = parameters[parameters.length - 1];
			PsiType type = method.getParameterList().getParameters()[parameters.length - 1].getType();
			if(type instanceof PsiClassType)
			{
				final PsiClassType classType = (PsiClassType) type;
				PsiClassType.ClassResolveResult resolveResult = classType.resolveGenerics();
				PsiClass psiClass = resolveResult.getElement();
				if(psiClass != null && ASYNC_CALLBACK_INTERFACE_NAME.equals(psiClass.getQualifiedName()))
				{
					PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
					if(typeParameters.length > 0)
					{
						PsiType parameter = resolveResult.getSubstitutor().substitute(typeParameters[0]);
						if(parameter != null)
						{
							PsiType unboxed;
							if(isBoxedVoidType(parameter))
							{
								unboxed = PsiType.VOID;
							}
							else
							{
								unboxed = PsiPrimitiveType.getUnboxedType(parameter);
							}
							newReturnType = unboxed != null ? unboxed : parameter;
						}
					}
					callbackParameter.delete();
				}
			}
		}
		PsiTypeElement typeElement = newMethod.getReturnTypeElement();
		if(typeElement != null)
		{
			PsiTypeElement newTypeElement = elementFactory.createTypeElement(newReturnType);
			typeElement.replace(newTypeElement);
		}
		return (PsiMethod) sync.add(newMethod);
	}

	private static boolean isBoxedVoidType(PsiType parameter)
	{
		if(!(parameter instanceof PsiClassType))
		{
			return false;
		}
		final PsiClass psiClass = ((PsiClassType) parameter).resolve();
		return psiClass != null && VOID_CLASS_NAME.equals(psiClass.getQualifiedName());
	}

}
