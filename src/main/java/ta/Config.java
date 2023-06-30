package ta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;
import utils.PathOptimization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Config {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final String entryFormatter = "<%s: void _jspService(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>";

    private boolean autoAddEntry = true;

    private List<String> epoints = Collections.emptyList();

    private List<String> sources = Collections.emptyList();

    private List<String> sinks = Collections.emptyList();

    private String appPath;

    private List<String> libPaths = Collections.emptyList();

    private String libPath;

    private Set<String> excludes = Collections.emptySet();

    private int pathReconstructionTimeout = 180;

    private int maxPathLength = 75;

    private String project;

    private String jdk;

    private boolean autoDetect;

    private String tempDir;

    private List<Rule> rules;


    public static class Rule {
        private String name;
        private List<String> sources = Collections.emptyList();

        private List<String> sinks = Collections.emptyList();

        public List<String> getSources() {
            return sources;
        }

        public void setSources(List<String> sources) {
            this.sources = sources;
        }

        public List<String> getSinks() {
            return sinks;
        }

        public void setSinks(List<String> sinks) {
            this.sinks = sinks;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public String getProject() {
        return project;
    }

    public String getEntryFormatter() {
        return entryFormatter;
    }

    public boolean isAutoAddEntry() {
        return autoAddEntry;
    }

    public void setAutoAddJspEntry(boolean autoAddJspEntry) {
        this.autoAddEntry = autoAddJspEntry;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public boolean isAutoDetect() {
        return autoDetect;
    }

    public void setAutoDetect(boolean autoDetect) {
        this.autoDetect = autoDetect;
    }

    public void autoConfig() {
        if (autoDetect) {
            if (project == null || project.isBlank()) {
                throw new AssertionError("project must not be null or empty!");
            }
            File f = new File(project);
            if (!f.exists()) {
                throw new AssertionError("project not found!");
            }
            if (!f.isDirectory()) {
                throw new AssertionError("project is not a directory!");
            }

            // filter all .class file
            List<String> classFiles = PathOptimization.filterFile(project, new String[]{"**/*.class"});
            List<String> jarFiles = PathOptimization.filterFile(project, new String[]{"**/*.jar"});
            // add rt.jar to lib path
            libPath = String.join(File.pathSeparator, jarFiles.stream().map(j -> Paths.get(project, j).toString()).toList());
            libPath += File.pathSeparator + jdk;
            // copy all .class to temporary directory.
            Path tmpdir = PathOptimization.createTempdir();
            logger.info("create temp dir: {}", tmpdir);
            this.tempDir = tmpdir.toString();
            Set<String> copied = new HashSet<>();
            List<String> javaClasses = new ArrayList<>();
            try {
                for (var file : classFiles) {
                    String absPath = Paths.get(project, file).toString();
                    String md5 = PathOptimization.md5(absPath);
                    if (copied.contains(md5)) {
                        continue;
                    }
                    File o = new File(absPath);
                    String packageName = PathOptimization.classPackageName(absPath);
                    String className = PathOptimization.className(absPath);
                    if (packageName == null) {
                        continue;
                    }
                    if (className != null) {
                        javaClasses.add(className);
                    }
                    Path d = Paths.get(tmpdir.toString(), PathOptimization.packageToDirString(packageName), o.getName());
                    logger.info("copy {} to {}", o.getPath(), d);
                    PathOptimization.copy(o, d.toFile());
                    copied.add(md5);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            appPath = tempDir;
            addEntry();
            List<String> libClasses = scanLibClasses(libPath);
            libClasses.removeAll(javaClasses);
            excludes.addAll(libClasses);

        }
    }

    private List<String> scanLibClasses(String libPath) {
        String[] jarFiles = libPath.split(File.pathSeparator);
        List<String> result = new ArrayList<>();
        for (String jarName : jarFiles) {
            if (jarName.contains("rt.jar")) {
                continue;
            }
            try {
                JarFile jarFile = new JarFile(jarName);
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if ((!entryName.contains("$")) && entryName.endsWith(".class")) {
                        String className = entryName.substring(0, entryName.length() - ".class".length())
                                .replace("/", ".");
                        result.add(className);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public void scanLib() {
        List<String> realLibPath = new ArrayList<>();
        if (jdk != null && !jdk.isBlank()) {
            realLibPath.add(jdk);
        }
        for (var path : libPaths) {
            if (path.endsWith(".jar")) {
                realLibPath.add(path);
            } else {
                File file = new File(path);
                if (file.isDirectory()) {
                    realLibPath.addAll(Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getName().endsWith(".jar")).map(File::getPath).toList());
                }
            }
        }
        libPath = String.join(File.pathSeparator, realLibPath);
    }

    private void setUpSoot() {
        G.reset();
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_exclude(excludes.stream().toList());
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Collections.singletonList(tempDir));
        Options.v().set_whole_program(true);
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
    }

    public void addEntry() {
        if (autoAddEntry) {
            setUpSoot();
            List<String> javaClasses = PathOptimization.filterFile(tempDir, new String[]{"**/*.class"});
            for (var clazz : javaClasses) {
                String absPath = Paths.get(tempDir, clazz).toString();
                String fullClassName = PathOptimization.className(absPath);
                try {
                    SootClass sc = Scene.v().getSootClass(fullClassName);
                    if (sc.declaresMethodByName("doGet")) {
                        String sg = sc.getMethodByName("doGet").getSignature();
                        logger.info("add {} to entry points", sg);
                        epoints.add(sg);
                    }
                    if (sc.declaresMethodByName("doPost")) {
                        String sg = sc.getMethodByName("doPost").getSignature();
                        logger.info("add {} to entry points", sg);
                        epoints.add(sg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (clazz.endsWith("_jsp.class")) {
                    String entry = String.format(entryFormatter, fullClassName);
                    logger.info("add {} to entry points", entry);
                    epoints.add(entry);
                }

            }

        }
    }

    public String getLibPath() {
        return libPath;
    }

    public void setLibPath(String libPath) {
        this.libPath = libPath;
    }

    public int getPathReconstructionTimeout() {
        return pathReconstructionTimeout;
    }

    public void setPathReconstructionTimeout(int pathReconstructionTimeout) {
        this.pathReconstructionTimeout = pathReconstructionTimeout;
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

    public void setMaxPathLength(int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public List<String> getEpoints() {
        return epoints;
    }

    public void setEpoints(List<String> epoints) {
        this.epoints = epoints;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public List<String> getSinks() {
        return sinks;
    }

    public void setSinks(List<String> sinks) {
        this.sinks = sinks;
    }

    public String getAppPath() {
        return appPath;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }

    public List<String> getLibPaths() {
        return libPaths;
    }

    public void setLibPaths(List<String> libPaths) {
        this.libPaths = libPaths;
    }

    public void setAutoAddEntry(boolean autoAddEntry) {
        this.autoAddEntry = autoAddEntry;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(Set<String> excludes) {
        this.excludes = excludes;
    }

    public String getJdk() {
        return jdk;
    }

    public void setJdk(String jdk) {
        this.jdk = jdk;
    }
}
