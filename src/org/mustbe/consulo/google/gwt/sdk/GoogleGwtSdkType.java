package org.mustbe.consulo.google.gwt.sdk;

import java.io.File;
import java.io.IOException;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.google.gwt.GoogleGwtIcons;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.util.ArchiveVfsUtil;

/**
 * @author VISTALL
 * @since 09.07.13.
 */
public class GoogleGwtSdkType extends SdkType
{
	private static final String LINE_START = "Google Web Toolkit ";

	public GoogleGwtSdkType()
	{
		super("GOOGLE_GWT_SDK");
	}

	@Nullable
	@Override
	public Icon getGroupIcon()
	{
		return getIcon();
	}

	@Nullable
	@Override
	public Icon getIcon()
	{
		return GoogleGwtIcons.Gwt;
	}

	@Nullable
	@Override
	public String suggestHomePath()
	{
		return null;
	}

	@Override
	public boolean isValidSdkHome(String s)
	{
		File file = new File(s, "gwt-dev.jar");
		if(!file.exists())
		{
			return false;
		}
		return true;
	}

	@Nullable
	@Override
	public String getVersionString(String s)
	{
		File file = new File(s, "about.txt");
		if(!file.exists())
		{
			return "unknown";
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
		catch(IOException e)
		{

		}
		return "unknown";
	}

	@Override
	public boolean setupSdkPaths(Sdk sdk, SdkModel sdkModel)
	{
		SdkModificator sdkModificator = sdk.getSdkModificator();

		VirtualFile homeDirectory = sdk.getHomeDirectory();
		if(homeDirectory == null)
		{
			sdkModificator.commitChanges();
			return true;
		}

		for(VirtualFile virtualFile : homeDirectory.getChildren())
		{
			String extension = virtualFile.getExtension();
			String name = virtualFile.getNameWithoutExtension();

			if(Comparing.equal(extension, "jar"))
			{
				if(name.endsWith("-src") || name.endsWith("-sources"))
				{
					sdkModificator.addRoot(ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile), OrderRootType.SOURCES);
				}
				else if(!name.endsWith("+src"))
				{
					sdkModificator.addRoot(ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile), OrderRootType.CLASSES);
				}

				if(name.equals("gwt-user"))
				{
					sdkModificator.addRoot(ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile), OrderRootType.SOURCES);
				}
			}
			else if(name.equals("doc") && virtualFile.isDirectory())
			{
				VirtualFile javadoc = virtualFile.findChild("javadoc");
				if(javadoc != null)
				{
					sdkModificator.addRoot(javadoc, OrderRootType.DOCUMENTATION);
				}
			}
		}

		sdkModificator.commitChanges();
		return true;
	}

	@Override
	public String suggestSdkName(String s, String s2)
	{
		File file = new File(s2);
		return file.getName();
	}

	@Override
	public boolean isRootTypeApplicable(OrderRootType type)
	{
		return JavaSdk.getInstance().isRootTypeApplicable(type);
	}

	@NotNull
	@Override
	public String getPresentableName()
	{
		return "Google GWT SDK";
	}
}
