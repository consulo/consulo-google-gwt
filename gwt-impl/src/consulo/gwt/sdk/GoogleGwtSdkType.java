package consulo.gwt.sdk;

import java.io.File;
import java.io.IOException;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.types.BinariesOrderRootType;
import com.intellij.openapi.roots.types.DocumentationOrderRootType;
import com.intellij.openapi.roots.types.SourcesOrderRootType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.util.ArchiveVfsUtil;
import consulo.gwt.GwtIcons;
import consulo.gwt.module.extension.path.GwtSdkUtil;

/**
 * @author VISTALL
 * @since 09.07.13.
 */
public class GoogleGwtSdkType extends GwtSdkBaseType
{
	private static final String LINE_START = "Google Web Toolkit ";

	public GoogleGwtSdkType()
	{
		super("GOOGLE_GWT_SDK");
	}

	@Override
	@NotNull
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
	public Icon getIcon()
	{
		return GwtIcons.Gwt;
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
			if(lines.length > 0 && lines[0].startsWith(LINE_START))
			{
				return lines[0].substring(LINE_START.length(), lines[0].length());
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

	@NotNull
	@Override
	public String getPresentableName()
	{
		return "GWT";
	}
}