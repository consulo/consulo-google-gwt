/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package com.intellij.gwt.ant;

/**
 * @author peter
 */
public class GwtExplodedAndJarBuildGenerator //extends ExplodedAndJarBuildGenerator
{
/*	@Nullable
	public Tag[] generateTagsForExplodedTarget(@NotNull final BuildInstruction instruction, @NotNull final ExplodedAndJarTargetParameters
			parameters, final int instructionCount) throws Exception
	{
		Pair<GwtFacet, String> pair = GwtWebBuildParticipant.getGwtModuleInfo(instruction);
		if(pair != null)
		{
			Copy copy = new Copy(BuildProperties.propertyRef(parameters.getExplodedPathParameter()) + "/" + instruction.getOutputRelativePath());
			copy.add(new FileSet(getGwtCompilerOutputDirectory(pair)));
			return new Tag[]{copy};
		}
		else
		{
			return null;
		}
	}

	private static String getGwtCompilerOutputDirectory(final Pair<GwtFacet, String> pair)
	{
		String sourceRoot = BuildProperties.propertyRef(GwtBuildProperties.getGwtCompilerOutputPropertyName(pair.getFirst()));
		return sourceRoot + "/" + pair.getSecond();
	}

	@Nullable
	public ZipFileSet[] generateTagsForJarTarget(@NotNull final BuildInstruction instruction, @NotNull final ExplodedAndJarTargetParameters
			parameters, final Ref<Boolean> tempDirUsed) throws Exception
	{
		Pair<GwtFacet, String> pair = GwtWebBuildParticipant.getGwtModuleInfo(instruction);
		if(pair != null)
		{
			return new ZipFileSet[]{new ZipFileSet(getGwtCompilerOutputDirectory(pair), instruction.getOutputRelativePath(), true)};
		}
		else
		{
			return null;
		}
	}

	@Nullable
	public Tag[] generateJarBuildPrepareTags(@NotNull final BuildInstruction instruction, @NotNull final ExplodedAndJarTargetParameters parameters)
			throws Exception
	{
		return GwtWebBuildParticipant.isCopyGwtOutputInstruction(instruction) ? Tag.EMPTY_ARRAY : null;
	}   */
}
