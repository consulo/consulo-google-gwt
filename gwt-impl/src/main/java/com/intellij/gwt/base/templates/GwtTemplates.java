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

package com.intellij.gwt.base.templates;

import org.jetbrains.annotations.NonNls;

/**
 * @author nik
 */
public class GwtTemplates
{
	@NonNls
	public static final String GWT_ENTRY_POINT_JAVA = "GwtEntryPoint.java";
	//@NonNls
	//public static final String GWT_MODULE_CSS = "GwtAppCss.css";
	@NonNls
	public static final String GWT_MODULE_GWT_XML = "GwtApp.gwt.xml";
	@NonNls
	public static final String GWT_MODULE_HTML = "GwtAppHtml.html";
	@NonNls
	public static final String GWT_MODULE_HTML_1_4 = "GwtAppHtml_1_4.html";
	@NonNls
	public static final String GWT_SERVICE_JAVA = "GwtAppService.java";
	@NonNls
	public static final String GWT_SERVICE_JAVA_1_0 = "GwtAppService_1_0.java";
	@NonNls
	public static final String GWT_SERVICE_ASYNC_JAVA = "GwtAppServiceAsync.java";
	@NonNls
	public static final String GWT_SERVICE_IMPL_JAVA = "GwtAppServiceImpl.java";
	@NonNls
	public static final String GWT_SAMPLE_APP_GWT_XML = "GwtSampleApp.gwt.xml";
	@NonNls
	public static final String GWT_SAMPLE_APP_HTML = "GwtSampleApp.html";
	@NonNls
	public static final String GWT_SAMPLE_APP_SERVICE_JAVA = "GwtSampleAppService.java";
	@NonNls
	public static final String GWT_SAMPLE_APP_SERVICE_ASYNC_JAVA = "GwtSampleAppServiceAsync.java";
	@NonNls
	public static final String GWT_SAMPLE_APP_SERVICE_IMPL_JAVA = "GwtSampleAppServiceImpl.java";
	@NonNls
	public static final String GWT_SAMPLE_ENTRY_POINT_JAVA = "GwtSampleEntryPoint.java";
	@NonNls
	public static final String GWT_SERIAL_CLASS_JAVA = "GwtSerialClass.java";
	@NonNls
	public static final String GWT_TEST_CASE_JAVA = "GwtTestCase.java";

	@NonNls
	public static final String[] TEMPLATES = {
			//GWT_MODULE_CSS,
			GWT_MODULE_GWT_XML,
			GWT_MODULE_HTML,
			GWT_MODULE_HTML_1_4,
			GWT_SERVICE_JAVA,
			GWT_SERVICE_JAVA_1_0,
			GWT_SERVICE_ASYNC_JAVA,
			GWT_SERVICE_IMPL_JAVA,
			GWT_ENTRY_POINT_JAVA,

			GWT_SAMPLE_APP_GWT_XML,
			GWT_SAMPLE_APP_HTML,
			GWT_SAMPLE_APP_SERVICE_JAVA,
			GWT_SAMPLE_APP_SERVICE_ASYNC_JAVA,
			GWT_SAMPLE_APP_SERVICE_IMPL_JAVA,
			GWT_SAMPLE_ENTRY_POINT_JAVA,
			GWT_SERIAL_CLASS_JAVA,
			GWT_TEST_CASE_JAVA
	};

	private GwtTemplates()
	{
	}
}
