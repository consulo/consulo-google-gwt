package com.intellij.gwt.module.index;

import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author nik
 */
public class GwtHtmlFileIndex extends ScalarIndexExtension<String>
{
	private static final ID<String, Void> NAME = ID.create("GwtHtmlFile");
	private static final FileBasedIndex.InputFilter INPUT_FILTER = new FileBasedIndex.InputFilter()
	{
		public boolean acceptInput(Project project, @Nonnull VirtualFile file)
		{
			return file.getFileType() == HtmlFileType.INSTANCE;
		}
	};
	private GwtHtmlFileIndexer myIndexer;
	private EnumeratorStringDescriptor myKeyDescriptor;

	public GwtHtmlFileIndex()
	{
		myIndexer = new GwtHtmlFileIndexer();
		myKeyDescriptor = new EnumeratorStringDescriptor();
	}

	@Nonnull
	@Override
	public ID<String, Void> getName()
	{
		return NAME;
	}

	@Nonnull
	@Override
	public DataIndexer<String, Void, FileContent> getIndexer()
	{
		return myIndexer;
	}

	@Nonnull
	@Override
	public KeyDescriptor<String> getKeyDescriptor()
	{
		return myKeyDescriptor;
	}

	@Nonnull
	@Override
	public FileBasedIndex.InputFilter getInputFilter()
	{
		return INPUT_FILTER;
	}

	@Override
	public boolean dependsOnFileContent()
	{
		return true;
	}

	@Override
	public int getVersion()
	{
		return 0;
	}

	public static Collection<VirtualFile> getHtmlFilesByModule(@Nonnull Project project, @Nonnull String moduleName)
	{
		final Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(NAME, moduleName, GlobalSearchScope.allScope(project));
		final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
		if(allFilesInSourceContent(files, fileIndex))
		{
			return files;
		}

		final List<VirtualFile> filtered = new ArrayList<VirtualFile>();
		for(VirtualFile file : files)
		{
			if(fileIndex.isInSourceContent(file))
			{
				filtered.add(file);
			}
		}
		return filtered;
	}

	private static boolean allFilesInSourceContent(Collection<VirtualFile> files, ProjectFileIndex fileIndex)
	{
		for(VirtualFile file : files)
		{
			if(!fileIndex.isInSourceContent(file))
			{
				return false;
			}
		}
		return true;
	}

	private static class GwtHtmlFileIndexer implements DataIndexer<String, Void, FileContent>
	{
		@Override
		@Nonnull
		public Map<String, Void> map(FileContent inputData)
		{
			final Map<String, Void> gwtModules = new HashMap<String, Void>();
			GwtHtmlUtil.collectGwtModules(inputData.getContentAsText(), gwtModules);
			return gwtModules;
		}
	}
}
