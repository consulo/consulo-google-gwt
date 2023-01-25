package consulo.gwt.base.sdk;

import com.intellij.gwt.sdk.GwtVersion;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.DocumentationOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.google.gwt.base.icon.GwtIconGroup;
import consulo.gwt.base.module.extension.path.GwtSdkUtil;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author VISTALL
 * @since 09.07.13.
 */
@ExtensionImpl
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
			String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
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
