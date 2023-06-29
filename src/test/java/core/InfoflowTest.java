package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import heros.solver.Pair;
import org.junit.Test;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.codeOptimization.DeadCodeEliminator;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import ta.CachedBiDiICFGFactory;
import ta.DetectedResult;
import ta.PathUnit;
import ta.ReuseableInfoflow;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class InfoflowTest {
    @Test
    public void test1() throws IOException {

        Gson gson = new Gson();
        Map m = gson.fromJson(new FileReader("config.json"), HashMap.class);
        String appPath = (String) m.get("appPath");
        List<String> lp = (ArrayList<String>) m.get("libPaths");
        List<String> epoints = (ArrayList<String>) m.get("epoints");
        List<String> sources = (ArrayList<String>) m.get("sources");
        List<String> sinks = (ArrayList<String>) m.get("sources");
        List<String> excludes = (ArrayList<String>) m.get("excludes");


        List<String> realLibPath = new ArrayList<>();
        for (var path : lp) {
            if (path.endsWith(".jar")) {
                realLibPath.add(path);
            } else {
                File file = new File(path);
                if (file.isDirectory()) {
                    realLibPath.addAll(Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getName().endsWith(".jar")).map(File::getPath).toList());
                }
            }
        }
        String libPath = String.join(File.pathSeparator, realLibPath);

        // Infoflow will set up soot configuration when set soot integration mode to 'CreateNewInstance' (by default)
        // Default soot configuration is in AbstractInfoflow's initializeSoot method.
        IInfoflowConfig sootConfig = (options, config) -> {
            options.set_ignore_classpath_errors(true);
            config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
            options.set_keep_line_number(true);
            options.set_prepend_classpath(true);
            options.set_src_prec(Options.src_prec_only_class);
            options.set_ignore_resolution_errors(true);
            options.set_exclude(excludes);
        };

        InfoflowConfiguration config = new InfoflowConfiguration();
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.PropagateConstants);
        config.setImplicitFlowMode(InfoflowConfiguration.ImplicitFlowMode.ArrayAccesses);
        config.setEnableReflection(false);
        config.setEnableLineNumbers(true);
        /**
         * private int maxCallStackSize = 30;
         * private int maxPathLength = 75;
         * private int maxPathsPerAbstraction = 15;
         * private long pathReconstructionTimeout = 0;
         * private int pathReconstructionBatchSize = 5;
         */
        config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        config.getPathConfiguration().setPathReconstructionTimeout(180);
        config.getPathConfiguration().setMaxPathLength(25);

        //
        CachedBiDiICFGFactory cachedBiDiICFGFactory = new CachedBiDiICFGFactory();
        Infoflow infoflow = new Infoflow("", false, cachedBiDiICFGFactory);
        infoflow.setConfig(config);
        infoflow.setSootConfig(sootConfig);

        EasyTaintWrapper easyTaintWrapper = EasyTaintWrapper.getDefault();
        infoflow.setTaintWrapper(easyTaintWrapper);

        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        InfoflowResults infoflowResults = infoflow.getResults();


        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        try {
            String json = gson.toJson(new OutputInfoflowResults(infoflowResults));
            Writer writer = new FileWriter("result.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<DetectedResult> results = new ArrayList<>();
        if (!infoflowResults.isEmpty()) {
            for (ResultSinkInfo sink : infoflowResults.getResults().keySet()) {
                String sinkSig = sink.getDefinition().toString();
                for (ResultSourceInfo source : infoflow.getResults().getResults().get(sink)) {
                    DetectedResult result = new DetectedResult();
                    result.setSinkSig(sinkSig);
                    result.setSourceSig(source.getDefinition().toString());
                    result.setPath(resultPath(cachedBiDiICFGFactory.getiCFG(), source, sink));
                    if (result.getPath().size() > 0) {
                        results.add(result);
                    }
                }
            }
        }

        try {
            String json = gson.toJson(results);
            Writer writer = new FileWriter("result2.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void test2() throws IOException, NoSuchFieldException, IllegalAccessException {

        Gson gson = new Gson();
        Map m = gson.fromJson(new FileReader("config.json"), HashMap.class);
        String appPath = (String) m.get("appPath");
        List<String> lp = (ArrayList<String>) m.get("libPaths");
        List<String> epoints = (ArrayList<String>) m.get("epoints");
        List<String> sources = (ArrayList<String>) m.get("sources");
        List<String> sinks = (ArrayList<String>) m.get("sources");
        List<String> excludes = (ArrayList<String>) m.get("excludes");


        List<String> realLibPath = new ArrayList<>();
        for (var path : lp) {
            if (path.endsWith(".jar")) {
                realLibPath.add(path);
            } else {
                File file = new File(path);
                if (file.isDirectory()) {
                    realLibPath.addAll(Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getName().endsWith(".jar")).map(File::getPath).toList());
                }
            }
        }
        String libPath = String.join(File.pathSeparator, realLibPath);

        InfoflowConfiguration config = new InfoflowConfiguration();
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.PropagateConstants);
        config.setImplicitFlowMode(InfoflowConfiguration.ImplicitFlowMode.ArrayAccesses);
        config.setEnableReflection(false);
        config.setEnableLineNumbers(true);
        config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        /**
         * Initialize soot manually if set soot integration mode to 'UseExistingCallgraph'
         */
        //
        config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.UseExistingInstance);


        /**
         * private int maxCallStackSize = 30;
         * private int maxPathLength = 75;
         * private int maxPathsPerAbstraction = 15;
         * private long pathReconstructionTimeout = 0;
         * private int pathReconstructionBatchSize = 5;
         */
        config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        config.getPathConfiguration().setPathReconstructionTimeout(180);
        config.getPathConfiguration().setMaxPathLength(25);

        //
        CachedBiDiICFGFactory cachedBiDiICFGFactory = new CachedBiDiICFGFactory();
        Infoflow infoflow = new Infoflow("", false, cachedBiDiICFGFactory);
        infoflow.setConfig(config);

        EasyTaintWrapper easyTaintWrapper = EasyTaintWrapper.getDefault();
        infoflow.setTaintWrapper(easyTaintWrapper);

        initializeSoot(config, appPath, libPath, epoints, excludes);
        constructCallgraph(config);

        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        InfoflowResults infoflowResults = infoflow.getResults();


        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        List<DetectedResult> results = new ArrayList<>();
        if (!infoflowResults.isEmpty()) {
            for (ResultSinkInfo sink : infoflowResults.getResults().keySet()) {
                String sinkSig = sink.getDefinition().toString();
                for (ResultSourceInfo source : infoflow.getResults().getResults().get(sink)) {
                    DetectedResult result = new DetectedResult();
                    result.setSinkSig(sinkSig);
                    result.setSourceSig(source.getDefinition().toString());
                    result.setPath(resultPath(cachedBiDiICFGFactory.getiCFG(), source, sink));
                    if (result.getPath().size() > 0) {
                        results.add(result);
                    }
                }
            }
        }

        try {
            String json = gson.toJson(results);
            Writer writer = new FileWriter("result3.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Test
    public void test3() throws IOException {

        Gson gson = new Gson();
        Map m = gson.fromJson(new FileReader("config.json"), HashMap.class);
        String appPath = (String) m.get("appPath");
        List<String> lp = (ArrayList<String>) m.get("libPaths");
        List<String> epoints = (ArrayList<String>) m.get("epoints");
        List<String> sources = (ArrayList<String>) m.get("sources");
        List<String> sinks = (ArrayList<String>) m.get("sources");
        List<String> excludes = (ArrayList<String>) m.get("excludes");


        List<String> realLibPath = new ArrayList<>();
        for (var path : lp) {
            if (path.endsWith(".jar")) {
                realLibPath.add(path);
            } else {
                File file = new File(path);
                if (file.isDirectory()) {
                    realLibPath.addAll(Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getName().endsWith(".jar")).map(File::getPath).toList());
                }
            }
        }
        String libPath = String.join(File.pathSeparator, realLibPath);

        // Infoflow will set up soot configuration when set soot integration mode to 'CreateNewInstance' (by default)
        // Default soot configuration is in AbstractInfoflow's initializeSoot method.
        IInfoflowConfig sootConfig = (options, config) -> {
            options.set_ignore_classpath_errors(true);
            config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
            options.set_keep_line_number(true);
            options.set_prepend_classpath(true);
            options.set_src_prec(Options.src_prec_only_class);
            options.set_ignore_resolution_errors(true);
            options.set_exclude(excludes);
        };

        InfoflowConfiguration config = new InfoflowConfiguration();
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.PropagateConstants);
        config.setImplicitFlowMode(InfoflowConfiguration.ImplicitFlowMode.ArrayAccesses);
        config.setEnableReflection(true);
        config.setEnableLineNumbers(true);
        /**
         * private int maxCallStackSize = 30;
         * private int maxPathLength = 75;
         * private int maxPathsPerAbstraction = 15;
         * private long pathReconstructionTimeout = 0;
         * private int pathReconstructionBatchSize = 5;
         */
        config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        config.getPathConfiguration().setPathReconstructionTimeout(180);
        config.getPathConfiguration().setMaxPathLength(25);

        //
        ReuseableInfoflow infoflow = new ReuseableInfoflow("", false);
        infoflow.setConfig(config);
        infoflow.setSootConfig(sootConfig);

        EasyTaintWrapper easyTaintWrapper = EasyTaintWrapper.getDefault();
        infoflow.setTaintWrapper(easyTaintWrapper);
        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        for (int i = 0; i < 3; i++) {
            infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
            InfoflowResults infoflowResults = infoflow.getResults();

            List<DetectedResult> results = new ArrayList<>();
            if (!infoflowResults.isEmpty()) {
                for (ResultSinkInfo sink : infoflowResults.getResults().keySet()) {
                    String sinkSig = sink.getDefinition().toString();
                    for (ResultSourceInfo source : infoflow.getResults().getResults().get(sink)) {
                        DetectedResult result = new DetectedResult();
                        result.setSinkSig(sinkSig);
                        result.setSourceSig(source.getDefinition().toString());
                        result.setPath(resultPath(infoflow.getICFG(), source, sink));
                        if (result.getPath().size() > 0) {
                            results.add(result);
                        }
                    }
                }
            }

            try {
                String json = gson.toJson(results);
                Writer writer = new FileWriter("result3-" + i + ".json");
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void initializeSoot(InfoflowConfiguration config, String appPath, String libPath, List<String> epoints, List<String> excludes) {
        // reset Soot:
        System.out.println("Resetting Soot...");
        soot.G.reset();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        if (config.getWriteOutputFiles())
            Options.v().set_output_format(Options.output_format_jimple);
        else
            Options.v().set_output_format(Options.output_format_none);

        // We only need to distinguish between application and library classes
        // if we use the OnTheFly ICFG
        if (config.getCallgraphAlgorithm() == InfoflowConfiguration.CallgraphAlgorithm.OnDemand) {
            Options.v().set_soot_classpath(libPath);
            if (appPath != null) {
                List<String> processDirs = new LinkedList<String>();
                for (String ap : appPath.split(File.pathSeparator))
                    processDirs.add(ap);
                Options.v().set_process_dir(processDirs);
            }
        } else
            Options.v().set_soot_classpath(appendClasspath(appPath, libPath));

        // do not merge variables (causes problems with PointsToSets)
        Options.v().setPhaseOption("jb.ulp", "off");
        Options.v().set_src_prec(Options.src_prec_java);

        // Build call graph
        // Configure the callgraph algorithm
        switch (config.getCallgraphAlgorithm()) {
            case AutomaticSelection:
                // If we analyze a distinct entry point which is not static,
                // SPARK fails due to the missing allocation site and we fall
                // back to CHA.
                setSparkOptions();
                break;
            case CHA:
                setChaOptions();
                break;
            case RTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "rta:true");
                Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
                Options.v().setPhaseOption("cg.spark", "string-constants:true");
                break;
            case VTA:
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "vta:true");
                Options.v().setPhaseOption("cg.spark", "string-constants:true");
                break;
            case SPARK:
                setSparkOptions();
                break;
            case GEOM:
                setSparkOptions();
                setGeomPtaSpecificOptions();
                break;
            case OnDemand:
                // nothing to set here
                break;
            default:
                throw new RuntimeException("Invalid callgraph algorithm");
        }

        // Specify additional options required for the callgraph
        if (config.getCallgraphAlgorithm() != InfoflowConfiguration.CallgraphAlgorithm.OnDemand) {
            Options.v().set_whole_program(true);
            Options.v().setPhaseOption("cg", "trim-clinit:false");
            if (config.getEnableReflection())
                Options.v().setPhaseOption("cg", "types-for-invoke:true");
        }

        // Initialize soot. Additional settings.
        Options.v().set_ignore_classpath_errors(true);

        Options.v().set_whole_program(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_exclude(excludes);
        Options.v().setPhaseOption("cg", "types-for-invoke:true");
        // load all entryPoint classes with their bodies
        IEntryPointCreator entryPointCreator = new DefaultEntryPointCreator(epoints);
        Collection<String> classes = entryPointCreator.getRequiredClasses();
        for (String className : classes) {
            Scene.v().addBasicClass(className, SootClass.BODIES);
        }

        Scene.v().loadNecessaryClasses();
        System.out.println("Basic class loading done.");

        boolean hasClasses = false;
        for (String className : classes) {
            SootClass c = Scene.v().forceResolve(className, SootClass.BODIES);
            if (c != null) {
                c.setApplicationClass();
                if (!c.isPhantomClass() && !c.isPhantom())
                    hasClasses = true;
            }
        }
        if (!hasClasses) {
            throw new RuntimeException("Only phantom classes loaded, skipping analysis...");
        }
    }

    private void constructCallgraph(InfoflowConfiguration config) {
        // Allow the ICC manager to change the Soot Scene before we continue
        // TODO
        // Run the preprocessors
        // TODO

        // Patch the system libraries we need for callgraph construction
        LibraryClassPatcher patcher = getLibraryClassPatcher();
        patcher.patchLibraries();

        // To cope with broken APK files, we convert all classes that are still
        // dangling after resolution into phantoms
        for (SootClass sc : Scene.v().getClasses())
            if (sc.resolvingLevel() == SootClass.DANGLING) {
                sc.setResolvingLevel(SootClass.BODIES);
                sc.setPhantomClass();
            }

        // We explicitly select the packs we want to run for performance
        // reasons. Do not re-run the callgraph algorithm if the host
        // application already provides us with a CG.
        if (config.getCallgraphAlgorithm() != InfoflowConfiguration.CallgraphAlgorithm.OnDemand && !Scene.v().hasCallGraph()) {
            PackManager.v().getPack("wjpp").apply();
            PackManager.v().getPack("cg").apply();
        }

        // Run the afterprocessors
        // TODO

    }


    private LibraryClassPatcher getLibraryClassPatcher() {
        return new LibraryClassPatcher();
    }


    /**
     * Appends two elements to build a classpath
     *
     * @param appPath The first entry of the classpath
     * @param libPath The second entry of the classpath
     * @return The concatenated classpath
     */
    private String appendClasspath(String appPath, String libPath) {
        String s = (appPath != null && !appPath.isEmpty()) ? appPath : "";

        if (libPath != null && !libPath.isEmpty()) {
            if (!s.isEmpty())
                s += File.pathSeparator;
            s += libPath;
        }
        return s;
    }

    private void setSparkOptions() {
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
    }

    private void setChaOptions() {
        Options.v().setPhaseOption("cg.cha", "on");
    }

    private void setGeomPtaSpecificOptions() {
        Options.v().setPhaseOption("cg.spark", "geom-pta:true");

        // Those are default options, not sure whether removing them works.
        Options.v().setPhaseOption("cg.spark", "geom-encoding:Geom");
        Options.v().setPhaseOption("cg.spark", "geom-worklist:PQ");
    }

    public List<PathUnit> resultPath(IInfoflowCFG icfg, ResultSourceInfo source, ResultSinkInfo sink) {
        ArrayList<PathUnit> path = new ArrayList<>();
        Stmt[] pathStmts = source.getPath();
        if (pathStmts == null) {
            pathStmts = new Stmt[]{source.getStmt(), sink.getStmt()};
        }
        for (Stmt p : pathStmts) {
            PathUnit unit = new PathUnit();
            SootMethod inFunction = icfg.getMethodOf(p);
            String file = inFunction.getDeclaringClass().getName().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
            String filePath = file + ".class";
            File f = new File(filePath);
            String webClassFilePath = "WEB-INF" + File.separator + "classes" + File.separator + file + ".class";
            File webClassFile = new File(webClassFilePath);
            if (f.exists() || webClassFile.exists()) {

            }
            if (file.contains("$")) {
                file = file.substring(0, file.indexOf("$"));
            }
            file += ".java";
            unit.setFile(file);
            unit.setJavaClass(inFunction.getDeclaringClass().getName());
            unit.setFunction(inFunction.getSignature());
            unit.setJimpleStmt(p.toString());
            if (p.getTag("LineNumberTag") != null) {
                unit.setLine(((LineNumberTag) p.getTag("LineNumberTag")).getLineNumber());
            }

            path.add(unit);
        }
        return path;
    }

    static class OutputInfoflowResults {
        long alltaintsnumber;
        List<OutputSourceSinkInfo> taints;

        static class OutputSourceSinkInfo {
            String sourceInfo;
            String sinkInfo;
            List<String> path;
            long pathLength;

            public OutputSourceSinkInfo(Pair<ResultSinkInfo, ResultSourceInfo> p) {
                sourceInfo = p.getO2().toString();
                sinkInfo = p.getO1().toString();
                path = new ArrayList<>();
                for (var stmt : p.getO2().getPath()) {
                    path.add(stmt.toString());
                }
                pathLength = path.size();
            }
        }

        public OutputInfoflowResults(InfoflowResults infoflowResults) {
            this.taints = new ArrayList<>();
            for (var p : infoflowResults.getResults()) {
                this.taints.add(new OutputSourceSinkInfo(p));
            }
            this.alltaintsnumber = this.taints.size();
        }
    }
}
