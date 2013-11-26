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

package com.intellij.gwt.module;

import com.intellij.gwt.module.index.GwtHtmlFileIndex;
import com.intellij.gwt.module.model.GwtEntryPoint;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.css.*;
import com.intellij.psi.css.resolve.CssResolveManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.DomService;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author nik
 */
public class GwtModulesManagerImpl extends GwtModulesManager {
  @NonNls private static final String CSS_EXTENSION = ".css";
  private static final Key<CachedValue<Map<String, CssClass>>> CACHED_CSS_CLASS_DECLARATIONS = Key.create("CACHED_CSS_CLASS_DECLARATIONS");
  private static final Key<CachedValue<Set<GwtModule>>> CACHED_GWT_INHERITED_MODULES = Key.create("CACHED_GWT_INHERITED_MODULES");
  private Project myProject;
  private ProjectFileIndex myProjectFileIndex;

  public GwtModulesManagerImpl(final Project project) {
    myProject = project;
    myProjectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
  }

  @NotNull
  public GwtModule[] getAllGwtModules() {
    return getGwtModules(GlobalSearchScope.allScope(myProject));
  }

  private GwtModule[] getGwtModules(@NotNull GlobalSearchScope scope) {
    final GwtModulesFinder finder = new GwtModulesFinder(myProject);
    final Collection<VirtualFile> candidates = DomService.getInstance().getDomFileCandidates(GwtModule.class, myProject, scope);
    for (VirtualFile file : candidates) {
      if (myProjectFileIndex.isInSource(file) || myProjectFileIndex.isInLibraryClasses(file)) {
        finder.processFile(file);
      }
    }

    final List<GwtModule> list = finder.getResults();
    return list.toArray(new GwtModule[list.size()]);
  }

  @NotNull
  public GwtModule[] getGwtModules(@NotNull final Module module) {
    return getGwtModules(GlobalSearchScope.moduleScope(module));
  }


  @Nullable
  public GwtModule findGwtModuleByClientSourceFile(@NotNull VirtualFile file) {
    List<GwtModule> gwtModules = findGwtModulesByClientSourceFile(file);
    return !gwtModules.isEmpty() ? gwtModules.get(0) : null;
  }

  @NotNull
  public List<GwtModule> findGwtModulesByClientSourceFile(@NotNull final VirtualFile file) {
    return findModulesByClientOrPublicFile(file, true, false);
  }

  @Nullable
  public GwtModule findGwtModuleByClientOrPublicFile(@NotNull VirtualFile file) {
    List<GwtModule> gwtModules = findModulesByClientOrPublicFile(file, true, true);
    return !gwtModules.isEmpty() ? gwtModules.get(0) : null;
  }

  @NotNull
  private List<GwtModule> findModulesByClientOrPublicFile(final VirtualFile file, final boolean clientFileAllowed, final boolean publicFileAllowed) {
    final GwtModulesFinder finder = new GwtModulesFinder(myProject);
    VirtualFile parent = file.getParent();
    while (parent != null && (myProjectFileIndex.isInSource(parent) || myProjectFileIndex.isInLibraryClasses(parent))) {
      finder.processChildren(parent);
      parent = parent.getParent();
    }

    ArrayList<GwtModule> gwtModules = new ArrayList<GwtModule>();
    for (GwtModule module : finder.getResults()) {
      if (clientFileAllowed) {
        final List<VirtualFile> sourceRoots = module.getSourceRoots();
        for (VirtualFile sourceRoot : sourceRoots) {
          if (VfsUtil.isAncestor(sourceRoot, file, false)) {
            gwtModules.add(module);
          }
        }
      }
      if (publicFileAllowed) {
        final List<VirtualFile> publicRoots = module.getPublicRoots();
        for (VirtualFile publicRoot : publicRoots) {
          if (VfsUtil.isAncestor(publicRoot, file, false)) {
            gwtModules.add(module);
          }
        }
      }
    }
    return gwtModules;
  }

  //todo[nik] return all files
  @Nullable
  public XmlFile findHtmlFileByModule(@NotNull GwtModule module) {
    final Collection<VirtualFile> htmlFiles = GwtHtmlFileIndex.getHtmlFilesByModule(myProject, module.getQualifiedName());
    if (htmlFiles.isEmpty()) return null;

    final VirtualFile parent = module.getModuleDirectory();
    final VirtualFile defaultFile = parent.findFileByRelativePath(DEFAULT_PUBLIC_PATH + "/" + module.getShortName() + "." + StdFileTypes.HTML.getDefaultExtension());
    final VirtualFile htmlFile;
    if (defaultFile != null && htmlFiles.contains(defaultFile)) {
      htmlFile = defaultFile;
    }
    else {
      htmlFile = htmlFiles.iterator().next();
    }

    final FileViewProvider viewProvider = PsiManager.getInstance(myProject).findViewProvider(htmlFile);
    if (viewProvider == null) return null;

    return (XmlFile)viewProvider.getPsi(StdLanguages.HTML);
  }

  @Nullable
  public PsiElement findTagById(@NotNull XmlFile htmlFile, final String id) {
    final Map<String, XmlTag> id2Tag = getHtmlId2TagMap(htmlFile);
    return id2Tag.get(id);
  }

  private static Map<String, XmlTag> getHtmlId2TagMap(final XmlFile htmlFile) {
    final Map<String, XmlTag> id2Tag = new HashMap<String, XmlTag>();
    htmlFile.accept(new XmlRecursiveElementVisitor() {
      @Override public void visitXmlTag(XmlTag tag) {
        final String elementId = tag.getAttributeValue("id");
        if (elementId != null) {
          id2Tag.put(elementId, tag);
        }
        super.visitXmlTag(tag);
      }
    });
    return id2Tag;
  }

  private void processModuleWithInherited(final GwtModule gwtModule, final Map<String, CssClass> cssClass2Declaration, final Set<GwtModule> processedModules) {
    if (!processedModules.add(gwtModule)) return;

    final List<CssFile> list = gwtModule.getStylesheetFiles();
    for (CssFile cssFile : list) {
      final CssStylesheet stylesheet = cssFile.getStylesheet();
      if (stylesheet != null) {
        for (CssRuleset ruleset : stylesheet.getRulesets()) {
          collectDeclarations(ruleset, cssClass2Declaration);
        }
      }
    }

    final XmlFile xmlFile = findHtmlFileByModule(gwtModule);
    final CssResolveManager resolveManager = CssResolveManager.getInstance();
    for (CssRuleset cssRuleset : resolveManager.getNewResolver().resolveAll(xmlFile)) {
      collectDeclarations(cssRuleset, cssClass2Declaration);
    }

    Module module = gwtModule.getModule();
    if (module != null) {
      List<GwtModule> inheritedList = gwtModule.getInherited(GlobalSearchScope.moduleWithDependenciesScope(module));
      for (GwtModule inherited : inheritedList) {
        processModuleWithInherited(inherited, cssClass2Declaration, processedModules);
      }
    }
  }

  private static void collectDeclarations(final CssRuleset cssRuleset, final Map<String, CssClass> cssClass2Declaration) {
    for (CssSelector selector : cssRuleset.getSelectorList().getSelectors()) {
      for (PsiElement selectorElement : selector.getElements()) {
        if (selectorElement instanceof CssSimpleSelector) {
          CssSimpleSelector simpleSelector = (CssSimpleSelector)selectorElement;
          final CssSelectorSuffix[] selectorSuffixes = simpleSelector.getSelectorSuffixes();
          for (CssSelectorSuffix suffix : selectorSuffixes) {
            if (suffix instanceof CssClass) {
              final CssClass cssClass = (CssClass)suffix;
              String name = cssClass.getName();
              if (!cssClass2Declaration.containsKey(name)) {
                cssClass2Declaration.put(name, cssClass);
              }
            }
          }
        }
      }
    }
  }

  @Nullable
  public CssClass findCssDeclarationByClass(final GwtModule module, final String cssClass) {
    return getCssClass2DeclarationMap(module).get(cssClass);
  }

  private Map<String, CssClass> getCssClass2DeclarationMap(final GwtModule module) {
    CachedValue<Map<String, CssClass>> cachedValue = module.getModuleXmlFile().getUserData(CACHED_CSS_CLASS_DECLARATIONS);
    if (cachedValue == null) {
      cachedValue =
        PsiManager.getInstance(myProject).getCachedValuesManager().createCachedValue(new CachedValueProvider<Map<String, CssClass>>() {
          public Result<Map<String, CssClass>> compute() {
            final Map<String, CssClass> cssClass2Declaration = new HashMap<String, CssClass>();
            processModuleWithInherited(module, cssClass2Declaration, new HashSet<GwtModule>());
            return Result.create(cssClass2Declaration, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                                 ProjectRootManager.getInstance(myProject));
          }
        }, false);
      module.getModuleXmlFile().putUserData(CACHED_CSS_CLASS_DECLARATIONS, cachedValue);
    }
    return cachedValue.getValue();
  }

  public String[] getAllCssClassNames(final GwtModule module) {
    final Set<String> classesSet = getCssClass2DeclarationMap(module).keySet();
    return ArrayUtil.toStringArray(classesSet);
  }

  @Nullable
  public CssFile findPreferableCssFile(final GwtModule module) {
    final List<CssFile> list = module.getStylesheetFiles();
    if (!list.isEmpty()) {
      return list.get(0);
    }

    final XmlFile htmlFile = findHtmlFileByModule(module);
    if (htmlFile == null) {
      return null;
    }

    final CssFile[] cssFiles = CssResolveManager.getInstance().getNewResolver().resolveStyleSheets(htmlFile, null);
    final String expectedFileName = module.getShortName() + CSS_EXTENSION;
    for (CssFile cssFile : cssFiles) {
      if (expectedFileName.equals(cssFile.getName())) {
        return cssFile;
      }
    }
    if (cssFiles.length > 0) {
      return cssFiles[0];
    }
    return null;
  }

  public boolean isGwtModuleFile(final VirtualFile file) {
    return file.getName().endsWith(GWT_XML_SUFFIX) && myProjectFileIndex.isInSourceContent(file);
  }

  public boolean isInheritedOrSelf(GwtModule gwtModule, GwtModule inheritedModule) {
    final Set<GwtModule> set = getInheritedModules(gwtModule);
    return set.contains(inheritedModule);
  }

  private Set<GwtModule> getInheritedModules(final GwtModule gwtModule) {
    CachedValue<Set<GwtModule>> cachedValue = gwtModule.getModuleXmlFile().getUserData(CACHED_GWT_INHERITED_MODULES);
    if (cachedValue == null) {
      cachedValue = PsiManager.getInstance(myProject).getCachedValuesManager().createCachedValue(new CachedValueProvider<Set<GwtModule>>() {
        public Result<Set<GwtModule>> compute() {
          final Set<GwtModule> set = new HashSet<GwtModule>();
          Module module = gwtModule.getModule();
          List<Object> dependencies = new ArrayList<Object>();
          GlobalSearchScope scope =
            module != null ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module) : GlobalSearchScope.allScope(myProject);
          collectAllInherited(gwtModule, set, scope, dependencies);
          dependencies.add(ProjectRootManager.getInstance(myProject));
          return Result.create(set, dependencies.toArray(new Object[dependencies.size()]));
        }
      }, false);
      gwtModule.getModuleXmlFile().putUserData(CACHED_GWT_INHERITED_MODULES, cachedValue);
    }
    return cachedValue.getValue();
  }

  public boolean isLibraryModule(GwtModule module) {
    return module.getEntryPoints().isEmpty() && findHtmlFileByModule(module) == null;
  }

  public boolean isUnderGwtModule(final VirtualFile file) {
    final GwtModulesFinder finder = new GwtModulesFinder(myProject);
    VirtualFile parent = file.getParent();
    while (parent != null && myProjectFileIndex.isInSource(parent)) {
      finder.processChildren(parent);
      parent = parent.getParent();
    }
    return !finder.getResults().isEmpty();
  }

  private static void collectAllInherited(final GwtModule gwtModule, final Set<GwtModule> set, final GlobalSearchScope scope,
                                          final List<Object> dependencies) {
    if (!set.add(gwtModule)) return;

    dependencies.add(gwtModule.getModuleXmlFile());
    for (GwtModule module : gwtModule.getInherited(scope)) {
      collectAllInherited(module, set, scope, dependencies);
    }
  }

  @Nullable
  public GwtModule findGwtModuleByName(final @NotNull String qualifiedName, final GlobalSearchScope scope) {
    final GwtModule[] gwtModules = findGwtModulesByName(qualifiedName, scope);
    return gwtModules.length > 0 ? gwtModules[0] : null;
  }

  @Nullable
  public String getPathFromPublicRoot(@NotNull final GwtModule gwtModule, @NotNull VirtualFile file) {
    for (VirtualFile root : gwtModule.getPublicRoots()) {
      if (VfsUtil.isAncestor(root, file, false)) {
        return VfsUtil.getRelativePath(file, root, '/');
      }
    }
    return null;
  }

  private GwtModule[] findGwtModulesByName(final String qualifiedName, final GlobalSearchScope scope) {
    List<GwtModule> modules = new ArrayList<GwtModule>();
    String name = qualifiedName;
    String packageName = "";
    do {
      final PsiPackage psiPackage = JavaPsiFacade.getInstance(myProject).findPackage(packageName);
      if (psiPackage != null) {
        final PsiDirectory[] directories = psiPackage.getDirectories(scope);
        for (PsiDirectory directory : directories) {
          final PsiFile psiFile = directory.findFile(name + GWT_XML_SUFFIX);
          if (psiFile instanceof XmlFile) {
            final DomFileElement<GwtModule> fileElement = DomManager.getDomManager(myProject).getFileElement((XmlFile)psiFile, GwtModule.class);
            if (fileElement != null) {
              modules.add(fileElement.getRootElement());
            }
          }
        }
      }

      int dot = name.indexOf('.');
      if (dot == -1) break;

      final String shortName = name.substring(0, dot);
      packageName = packageName.length() > 0 ? packageName + "." + shortName : shortName;
      name = name.substring(dot + 1);
    } while(true);

    return modules.toArray(new GwtModule[modules.size()]);
  }

  public String[] getAllIds(@NotNull XmlFile htmlFile) {
    final Set<String> idSet = getHtmlId2TagMap(htmlFile).keySet();
    return ArrayUtil.toStringArray(idSet);
  }

  @NotNull
  public List<GwtModule> findModulesByClass(@NotNull final PsiElement context, final @Nullable String className) {
    if (className == null) return Collections.emptyList();

    PsiClass[] psiClasses = JavaPsiFacade.getInstance(context.getProject()).findClasses(className, context.getResolveScope());
    for (PsiClass psiClass : psiClasses) {
      PsiFile psiFile = psiClass.getContainingFile();
      if (psiFile != null) {
        VirtualFile file = psiFile.getVirtualFile();
        if (file != null) {
          List<GwtModule> modules = findGwtModulesByClientSourceFile(file);
          if (!modules.isEmpty()) {
            return modules;
          }
        }
      }
    }
    return Collections.emptyList();
  }

  public GwtModule findGwtModuleByEntryPoint(@NotNull final PsiClass psiClass) {
    PsiFile psiFile = psiClass.getContainingFile();
    if (psiFile == null) return null;

    VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return null;

    List<GwtModule> gwtModules = findGwtModulesByClientSourceFile(file);
    for (GwtModule gwtModule : gwtModules) {
      List<GwtEntryPoint> entryPoints = gwtModule.getEntryPoints();
      for (GwtEntryPoint entryPoint : entryPoints) {
        String className = entryPoint.getEntryClass().getValue();
        if (className != null && className.equals(psiClass.getQualifiedName())) {
          return gwtModule;
        }
      }
    }
    return null;
  }

  @NotNull
  public List<Pair<GwtModule, String>> findGwtModulesByPublicFile(@NotNull final VirtualFile file) {
    List<GwtModule> gwtModules = findModulesByClientOrPublicFile(file, false, true);
    List<Pair<GwtModule, String>> pairs = new ArrayList<Pair<GwtModule, String>>();
    for (GwtModule gwtModule : gwtModules) {
      String path = getPathFromPublicRoot(gwtModule, file);
      if (path != null) {
        pairs.add(Pair.create(gwtModule, path));
      }
    }
    return pairs;
  }

  @Nullable
  public GwtModule getGwtModuleByXmlFile(@NotNull PsiFile file) {
    if (file instanceof XmlFile) {
      DomFileElement<GwtModule> fileElement = DomManager.getDomManager(myProject).getFileElement((XmlFile)file, GwtModule.class);
      if (fileElement != null) {
        return fileElement.getRootElement();
      }
    }
    return null;
  }

  public boolean isInheritedOrSelf(final GwtModule gwtModule, final List<GwtModule> referencedModules) {
    for (GwtModule referencedModule : referencedModules) {
      if (isInheritedOrSelf(gwtModule, referencedModule)) {
        return true;
      }
    }
    return false;
  }

  private static class GwtModulesFinder implements ContentIterator {
    private final List<GwtModule> myResults;
    private final PsiManager myPsiManager;
    private final DomManager myDomManager;

    public GwtModulesFinder(final Project project) {
      myResults = new ArrayList<GwtModule>();
      myPsiManager = PsiManager.getInstance(project);
      myDomManager = DomManager.getDomManager(project);
    }

    public boolean processFile(VirtualFile fileOrDir) {
      if (!fileOrDir.isDirectory() && fileOrDir.getFileType() == StdFileTypes.XML &&
          fileOrDir.getNameWithoutExtension().endsWith(GWT_SUFFIX)) {
        final PsiFile psiFile = myPsiManager.findFile(fileOrDir);
        if (psiFile instanceof XmlFile) {
          final DomFileElement<GwtModule> fileElement = myDomManager.getFileElement((XmlFile)psiFile, GwtModule.class);
          if (fileElement != null) {
            myResults.add(fileElement.getRootElement());
          }
        }
      }
      return true;
    }

    public List<GwtModule> getResults() {
      return myResults;
    }

    public void processChildren(final VirtualFile parent) {
      List<VirtualFile> directories = getDirectories(parent);

      for (VirtualFile directory : directories) {
        final VirtualFile[] files = directory.getChildren();
        if (files != null) {
          for (VirtualFile virtualFile : files) {
            processFile(virtualFile);
          }
        }
      }
    }

    private List<VirtualFile> getDirectories(final VirtualFile directory) {
      Module module = ModuleUtil.findModuleForFile(directory, myPsiManager.getProject());
      
      if (module != null) {
        PsiDirectory psiDirectory = myPsiManager.findDirectory(directory);
        if (psiDirectory != null) {
          PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
          if (psiPackage != null) {
            List<VirtualFile> directories = new ArrayList<VirtualFile>();
            PsiDirectory[] psiDirectories = psiPackage.getDirectories(module.getModuleWithDependentsScope());
            for (PsiDirectory dir : psiDirectories) {
              directories.add(dir.getVirtualFile());
            }
            return directories;
          }
        }
      }

      return Collections.singletonList(directory);
    }
  }

}
