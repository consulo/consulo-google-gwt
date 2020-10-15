package consulo.gwt.sdk;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.google.gwt.icon.GwtIconGroup;
import consulo.gwt.module.extension.path.GwtSdkUtil;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.DocumentationOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import consulo.ui.image.Image;
import consulo.vfs.util.ArchiveVfsUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 09.07.13.
 */
public class GoogleGwtSdkType extends GwtSdkBaseType
{
	private static final String LINE_START = "Google Web Toolkit ";
	private static final String LINE_START2 = "GWT ";

	public GoogleGwtSdkType()
	{
		super("GOOGLE_GWT_SDK");
	}

	@Override
	@Nonnull
	public GwtVersion getVersion(Sdk sdk)
	{
		return GwtSdkUtil.detectVersion(sdk);
	}

	@Nullable
	public String getDevJarPath(Sdk sdk)
	{
		return GwtSdkUtil.getDevJarPath(sdk.getHomePath());
	}

	@Nullable
	public String getUserJarPath(Sdk sdk)
	{
		return GwtSdkUtil.getUserJarPath(sdk.getHomePath());
	}

	@Nullable
	@Override
	public Image getIcon()
	{
		return GwtIconGroup.gwt();
	}

	@Override
	public boolean isValidSdkHome(String s)
	{
		File file = new File(s, "gwt-dev.jar");
		return file.exists();
	}

	@Nullable
	@Override
	public String getVersionString(String s)
	{
		File file = new File(s, "about.txt");
		if(!file.exists())
		{
			return "0.0";
		}
		try
		{
			String text = FileUtil.loadFile(file, "UTF-8");
			String[] lines = text.split("\n");
			if(lines.length > 0)
			{
				String line = lines[0];
				if(line.startsWith(LINE_START))
				{
					return line.substring(LINE_START.length(), line.length());
				}
				else if(line.startsWith(LINE_START2))
				{
					return line.substring(LINE_START2.length(), line.length());
				}
			}
		}
		catch(IOException ignored)
		{
		}
		return "0.0";
	}

	@Override
	public void setupSdkPaths(Sdk sdk)
	{
		SdkModificator sdkModificator = sdk.getSdkModificator();

		VirtualFile homeDirectory = sdk.getHomeDirectory();
		if(homeDirectory == null)
		{
			sdkModificator.commitChanges();
			return;
		}

		for(VirtualFile virtualFile : homeDirectory.getChildren())
		{
			String extension = virtualFile.getExtension();
			String name = virtualFile.getNameWithoutExtension();

			if(Comparing.equal(extension, "jar"))
			{
				if(name.endsWith("-src") || name.endsWith("-sources"))
				{
					sdkModificator.addRoot(ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile), SourcesOrderRootType.getInstance());
				}
				else if(!name.endsWith("+src"))
				{
					sdkModificator.addRoot(ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile), BinariesOrderRootType.getInstance());
				}

				if(name.equals("gwt-user"))
				{
					sdkModificator.addRoot(ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile), SourcesOrderRootType.getInstance());
				}
			}
			else if(name.equals("doc") && virtualFile.isDirectory())
			{
				VirtualFile javadoc = virtualFile.findChild("javadoc");
				if(javadoc != null)
				{
					sdkModificator.addRoot(javadoc, DocumentationOrderRootType.getInstance());
				}
			}
		}

		sdkModificator.commitChanges();
	}

	@Override
	public String suggestSdkName(String currentSdkName, String sdkHome)
	{
		return getPresentableName() + " " + getVersionString(sdkHome);
	}

	@Nonnull
	@Override
	public String getPresentableName()
	{
		return "GWT";
	}
}
