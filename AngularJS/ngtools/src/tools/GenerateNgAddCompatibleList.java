// Copyright 2000-2018 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package tools;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.intellij.idea.IdeaTestApplication;
import com.intellij.javascript.nodejs.packageJson.NodePackageBasicInfo;
import com.intellij.javascript.nodejs.packageJson.NpmRegistryService;
import com.intellij.javascript.nodejs.packageJson.NpmRegistryServiceImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.util.Consumer;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.HttpRequests;
import org.angularjs.cli.AngularCliSchematicsRegistryServiceImpl;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Execute to generate in the console JSON object with list of ng-add supported packages.
 * The result has to be manually merged into {@code resources/org/angularjs/cli/ng-packages.json}.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
public class GenerateNgAddCompatibleList {

  public static void main(String[] args) throws Exception {
    try {
      setUpApplication();
      generate();
    } finally {
      System.exit(0);
    }
  }

  public static void generate() throws Exception {
    Map<String, NodePackageBasicInfo> angularPkgs = ContainerUtil.newHashMap();
    NpmRegistryService service = new NpmRegistryServiceImpl();

    ApplicationImpl app = (ApplicationImpl)ApplicationManager.getApplication();
    Field f = ApplicationImpl.class.getDeclaredField("myTestModeFlag");
    f.setAccessible(true);
    f.setBoolean(app, false);

    Consumer<NodePackageBasicInfo> addPkg = pkg -> angularPkgs.putIfAbsent(pkg.getName(), pkg);
    System.out.println("Loading list of Angular packages.");

    Stream.of("angular schematics", "angular components", "angular").parallel().forEach(search -> {
      try {
        service.findPackages(
          null, NpmRegistryService.fullTextSearch(search), 100000, pkg -> true,
          addPkg);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    System.out.println("\nFound " + angularPkgs.size() + " Angular packages.");

    AtomicInteger progress = new AtomicInteger();

    List<Pair<NodePackageBasicInfo, JsonObject>> schematicsPkgs = angularPkgs.values().stream().parallel().map(info -> {
      try {
        JsonObject obj = service.fetchPackageJson(info.getName(), "latest", null);
        if (obj != null && obj.has("schematics")) {
          return Pair.create(info, obj);
        }
      }
      catch (Throwable t) {
        System.out.println("Error for " + info.getName() + ", " + t.getMessage());
      }
      finally {
        System.out.print(".");
        if (progress.incrementAndGet() % 100 == 0) {
          System.out.println(" " + ((progress.get() * 100) / angularPkgs.size()) + "%\n");
        }
      }
      return null;
    }).filter(r -> r != null).collect(Collectors.toList());

    System.out.println("\nFound " + schematicsPkgs.size() + " packages which support schematics.");
    progress.set(0);

    Map<String, String> ngAddPkgs = schematicsPkgs.stream().parallel().map(p -> {
      NodePackageBasicInfo info = p.getFirst();
      JsonObject pkgJson = p.getSecond();
      String url = pkgJson.get("dist").getAsJsonObject().get("tarball").getAsString();
      String schematicsFile = pkgJson.get("schematics").getAsString();
      schematicsFile = StringUtil.trimStart(schematicsFile, "./");
      try {
        byte[] contents = HttpRequests.request(url).readBytes(null);
        InputStream bi = new BufferedInputStream(new ByteArrayInputStream(contents));
        InputStream gzi = new GzipCompressorInputStream(bi);
        ArchiveInputStream input = new TarArchiveInputStream(gzi);
        ArchiveEntry e;
        while ((e = input.getNextEntry()) != null) {
          if (e.getName().endsWith(schematicsFile)) {
            if (input.canReadEntryData(e)) {
              String schematicsCollection = FileUtil.loadTextAndClose(input);
              if (hasNgAddSchematic(schematicsCollection)) {
                return info;
              }
              else {
                return null;
              }
            }
          }
        }
      }
      catch (Throwable t) {
        System.out.println("Error for " + info.getName() + ": " + t.getMessage());
        t.printStackTrace();
      }
      finally {
        System.out.print(".");
        if (progress.incrementAndGet() % 20 == 0) {
          System.out.println(" " + ((progress.get() * 100) / schematicsPkgs.size()) + "%\n");
        }
      }
      return null;
    }).filter(info -> info != null).collect(
      Collectors.toMap(NodePackageBasicInfo::getName, NodePackageBasicInfo::getDescription));

    ngAddPkgs = ContainerUtil.newTreeMap(ngAddPkgs);

    System.out.println("\nFound " + ngAddPkgs.size() + " packages which support ng-add:");
    System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(ngAddPkgs));
    f.setBoolean(app, true);
  }

  private static boolean hasNgAddSchematic(@NotNull String schematicsCollection) throws IOException {
    try (JsonReader reader = new JsonReader(new StringReader(schematicsCollection))) {
      return AngularCliSchematicsRegistryServiceImpl.hasNgAddSchematic(reader);
    }
  }

  private static void setUpApplication() {
    try {
      String[] candidates = ReflectionUtil.getField(PlatformTestCase.class, null, String[].class, "PREFIX_CANDIDATES");
      if (candidates != null) {
        candidates[0] = "WebStorm";
      }
      File tmpPath = FileUtil.createTempDirectory("ng-add-gen", null, true);
      System.out.println("Using temporary configuration folder: " + tmpPath.toString());
      System.setProperty(PathManager.PROPERTY_PLUGINS_PATH, tmpPath.toString() + "/plugins");
      System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, tmpPath.toString() + "/system");
      System.setProperty(PathManager.PROPERTY_CONFIG_PATH, tmpPath.toString() + "/config");
      IdeaTestApplication.getInstance();
    } catch (Throwable t) {
      //ignore
    }
  }

}
