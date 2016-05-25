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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;
import com.intellij.gwt.sdk.GwtVersion;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.rpc.GwtGenericsUtil;
import com.intellij.gwt.rpc.GwtSerializableUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;

/**
 * @author nik
 */
public class GwtNonSerializableRemoteServiceMethodParametersInspection extends BaseGwtInspection
{
	private final GwtSerializableInspectionState myGwtSerializableInspectionState = new GwtSerializableInspectionState();
	@NonNls
	private static final String SETTINGS_ELEMENT = "settings";

	@RequiredReadAction
	@Override
	@Nullable
	public ProblemDescriptor[] checkClassImpl(@NotNull GoogleGwtModuleExtension extension, @NotNull GwtVersion version, @NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly)
	{
		GoogleGwtModuleExtension gwtFacet = getExtension(aClass);
		if(gwtFacet == null)
		{
			return null;
		}

		if(RemoteServiceUtil.isRemoteServiceInterface(aClass))
		{
			return checkRemoteService(gwtFacet, aClass, manager);
		}
		return null;
	}

	@Override
	public void readSettings(final Element node) throws InvalidDataException
	{
		final Element settings = node.getChild(SETTINGS_ELEMENT);
		if(settings != null)
		{
			XmlSerializer.deserializeInto(myGwtSerializableInspectionState, settings);
		}
	}

	@Override
	public void writeSettings(final Element node) throws WriteExternalException
	{
		final Element settings = new Element(SETTINGS_ELEMENT);
		XmlSerializer.serializeInto(myGwtSerializableInspectionState, settings, new SkipDefaultValuesSerializationFilters());
		if(!settings.getContent().isEmpty())
		{
			node.addContent(settings);
		}
	}

	@Override
	public JComponent createOptionsPanel()
	{
		final JPanel panel = new JPanel(new BorderLayout());
		final JCheckBox reportInterfacesCheckbox = new JCheckBox(GwtBundle.message("checkbox.text.report.interfaces"));
		reportInterfacesCheckbox.setSelected(myGwtSerializableInspectionState.isReportInterfaces());
		reportInterfacesCheckbox.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
			{
				myGwtSerializableInspectionState.setReportInterfaces(reportInterfacesCheckbox.isSelected());
			}
		});
		panel.add(reportInterfacesCheckbox, BorderLayout.NORTH);
		return panel;
	}

	private ProblemDescriptor[] checkRemoteService(final GoogleGwtModuleExtension gwtFacet, final PsiClass aClass, final InspectionManager manager)
	{
		ArrayList<ProblemDescriptor> result = new ArrayList<ProblemDescriptor>(0);

		GwtSerializableUtil.SerializableChecker serializableChecker = GwtSerializableUtil.createSerializableChecker(gwtFacet,
				myGwtSerializableInspectionState.isReportInterfaces());

		GlobalSearchScope scope = aClass.getResolveScope();
		JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(manager.getProject());
		PsiClass exceptionClass = psiFacade.findClass(Exception.class.getName(), scope);

		for(final PsiMethod method : aClass.getMethods())
		{
			for(final PsiParameter param : method.getParameterList().getParameters())
			{
				String typeParametersString = GwtGenericsUtil.getTypeParametersString(method, param.getName());
				checkTypeSerial(param.getTypeElement(), typeParametersString, serializableChecker, manager, result);
			}
			final PsiTypeElement returnTypeElement = method.getReturnTypeElement();
			if(returnTypeElement != null)
			{
				String typeParameters = GwtGenericsUtil.getReturnTypeParametersString(method);
				checkTypeSerial(returnTypeElement, typeParameters, serializableChecker, manager, result);
			}

			PsiJavaCodeReferenceElement[] thrown = method.getThrowsList().getReferenceElements();
			for(PsiJavaCodeReferenceElement referenceElement : thrown)
			{
				PsiClassType classType = psiFacade.getElementFactory().createType(referenceElement);
				PsiClass psiClass = classType.resolve();

				if(exceptionClass != null && psiClass != null && !InheritanceUtil.isInheritorOrSelf(psiClass, exceptionClass, true))
				{
					String message = GwtBundle.message("problem.description.0.is.not.a.checked.exception", psiClass.getQualifiedName());
					result.add(manager.createProblemDescriptor(referenceElement, message, LocalQuickFix.EMPTY_ARRAY,
							ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
				}
				else
				{
					checkTypeSerial(classType, referenceElement, null, serializableChecker, manager, result);
				}
			}
		}

		return result.toArray(new ProblemDescriptor[result.size()]);
	}

	private static void checkTypeSerial(PsiTypeElement typeElement, final @Nullable String typeParameterStrings,
			final GwtSerializableUtil.SerializableChecker serializableChecker, InspectionManager manager, List<ProblemDescriptor> result)
	{
		PsiType type = typeElement.getType();
		checkTypeSerial(type, typeElement, typeParameterStrings, serializableChecker, manager, result);
	}

	private static void checkTypeSerial(PsiType type, final PsiElement typeElement, final @Nullable String typeParameterStrings,
			final GwtSerializableUtil.SerializableChecker serializableChecker, final InspectionManager manager, final List<ProblemDescriptor> result)
	{
		if(!type.isValid())
		{
			return;
		}
		List<PsiType> typeParameters = GwtGenericsUtil.getTypeParameters(typeElement, typeParameterStrings);

		if(serializableChecker.isSerializable(type, typeParameters))
		{
			return;
		}
		while(type instanceof PsiArrayType)
		{
			type = ((PsiArrayType) type).getComponentType();
		}

		if(!(type instanceof PsiClassType))
		{
			return;
		}

		PsiClassType classType = (PsiClassType) type;

		if(!serializableChecker.getVersion().isGenericsSupported() && classType.getParameters().length > 0)
		{
			String description = GwtBundle.message("problem.description.generics.isnt.supported.in.gwt.before.1.5.version");
			result.add(manager.createProblemDescriptor(typeElement, description, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
			return;
		}

		PsiClass aClass = classType.resolve();
		if(aClass != null)
		{
			boolean haveGenericParameters = serializableChecker.getVersion().isGenericsSupported() && classType.getParameters().length > 0;
			final String description;
			final LocalQuickFix[] quickFixes;
			String typeString = type.getCanonicalText();
			if(typeParameterStrings == null && !haveGenericParameters && GwtSerializableUtil.isCollection(type))
			{
				description = GwtBundle.message("problem.description.type.of.collection.elements.is.not.specified", typeString);
				quickFixes = new LocalQuickFix[]{};
			}
			else
			{
				if(typeParameterStrings != null && !haveGenericParameters)
				{
					typeString += typeParameterStrings;
				}

				if(!isInSources(aClass))
				{
					quickFixes = LocalQuickFix.EMPTY_ARRAY;
					description = GwtBundle.message("problem.description.type.is.not.serializable", typeString);
				}
				else
				{
					final List<PsiClass> list = serializableChecker.getSerializableMarkerInterfaces();
					final PsiElementFactory psiFactory = JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory();
					quickFixes = new LocalQuickFix[list.size()];
					for(int i = 0; i < list.size(); i++)
					{
						quickFixes[i] = IntentionManager.getInstance().convertToFix(QuickFixFactory.getInstance().createExtendsListFix(aClass,
								psiFactory.createType(list.get(i)), true));
					}
					description = GwtBundle.message("problem.description.gwt.serializable.type.0.should.implements.marker.interface.1", typeString,
							serializableChecker.getPresentableSerializableClassesString());
				}
			}
			result.add(manager.createProblemDescriptor(typeElement, description, quickFixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
		}
	}

	private static boolean isInSources(final PsiClass aClass)
	{
		VirtualFile file = aClass.getContainingFile().getVirtualFile();
		if(file == null)
		{
			return false;
		}
		Module module = ModuleUtil.findModuleForFile(file, aClass.getProject());
		return module != null && ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(file);
	}

	@Override
	@NotNull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.non.serializable.service.method.parameters");
	}

	@Override
	@NotNull
	@NonNls
	public String getShortName()
	{
		return "NonSerializableServiceParameters";
	}

	public static class GwtSerializableInspectionState
	{
		private boolean myReportInterfaces = true;

		@Tag("report-interfaces")
		public boolean isReportInterfaces()
		{
			return myReportInterfaces;
		}

		public void setReportInterfaces(final boolean reportInterfaces)
		{
			myReportInterfaces = reportInterfaces;
		}
	}
}
