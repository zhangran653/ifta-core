package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Test;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;
import ta.Config;
import ta.DetectedResult;
import ta.ReuseableInfoflow;
import ta.RuleResult;
import utils.ClassPathResource;
import utils.IFFactory;
import utils.PathOptimization;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ReuseableInfoflowTest {

    @Test
    public void test1() throws IOException {

        Gson gson = new Gson();
        Config c = gson.fromJson(new FileReader("config.json"), Config.class);
        String appPath = c.getAppPath();
        List<String> lp = c.getLibPaths();
        List<String> epoints = c.getEpoints();
        List<String> sources = c.getSources();
        List<String> sinks = c.getSinks();
        Set<String> excludes = c.getExcludes();


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
            options.set_exclude(excludes.stream().toList());
            options.set_keep_offset(true);
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
        config.getPathConfiguration().setMaxPathLength(75);
        config.getPathConfiguration().setMaxPathsPerAbstraction(15);

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
                        result.setPath(PathOptimization.resultPath(infoflow.getICFG(), source, sink));
                        result.setPathStm(PathOptimization.pathStm(source));
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

    @Test
    public void test2() throws FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        // 命令注入
        Config c = gson.fromJson(new FileReader("config2.json"), Config.class);
        c.autoConfig();
        String appPath = c.getAppPath();
        List<String> epoints = c.getEpoints();
        List<String> sources = c.getSources();
        List<String> sinks = c.getSinks();
        String libPath = c.getLibPath();

        ReuseableInfoflow infoflow = IFFactory.buildReuseable(appPath, c.getExcludes().stream().toList(), c.getPathReconstructionTimeout());

        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        List<DetectedResult> results = PathOptimization.detectedResults(infoflow, infoflow.getICFG(), c.getProject());
        System.out.println("命令注入 find " + results.size() + " results");
        try {
            String json = gson.toJson(results);
            Writer writer = new FileWriter("result2.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PathOptimization.deteleTempdir(c.getTempDir());
    }

    @Test
    public void test3() throws IOException {
        Path tmpdir = PathOptimization.createTempdir();
        System.out.println(tmpdir);
        String project = "/home/ran/Documents/work/thusa2/ifpc-testcase/WebGoat-5.0";
        File o = new File("/home/ran/Documents/work/thusa2/ifpc-testcase/WebGoat-5.0/WebContent/classes/org/owasp/webgoat/LessonSource.class");
        String relp = PathOptimization.removePrefix(o.getPath(), project);
        Path path = Paths.get(tmpdir.toString(), relp);
        PathOptimization.copy(o, path.toFile());
        PathOptimization.deteleTempdir(tmpdir.toString());
    }

    @Test
    public void test4() throws FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        // 路径操作
        Config c = gson.fromJson(new FileReader("config3.json"), Config.class);
        c.autoConfig();
        String appPath = c.getAppPath();
        List<String> epoints = c.getEpoints();
        List<String> sources = c.getSources();
        List<String> sinks = c.getSinks();
        String libPath = c.getLibPath();

        ReuseableInfoflow infoflow = IFFactory.buildReuseable(appPath, c.getExcludes().stream().toList());

        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        List<DetectedResult> results = PathOptimization.detectedResults(infoflow, infoflow.getICFG(), c.getProject());
        System.out.println("路径操作 find " + results.size() + " results");
        try {
            String json = gson.toJson(results);
            Writer writer = new FileWriter("result4.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PathOptimization.deteleTempdir(c.getTempDir());
    }

    @Test
    public void test5() throws FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        // sql注入
        Config c = gson.fromJson(new FileReader("config4.json"), Config.class);
        c.autoConfig();
        String appPath = c.getAppPath();
        List<String> epoints = c.getEpoints();
        List<String> sources = c.getSources();
        List<String> sinks = c.getSinks();
        String libPath = c.getLibPath();

        ReuseableInfoflow infoflow = IFFactory.buildReuseable(appPath, c.getExcludes().stream().toList());

        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        List<DetectedResult> results = PathOptimization.detectedResults(infoflow, infoflow.getICFG(), c.getProject());
        System.out.println("sql注入 find " + results.size() + " results");
        try {
            String json = gson.toJson(results);
            Writer writer = new FileWriter("result5.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PathOptimization.deteleTempdir(c.getTempDir());
    }


    @Test
    public void test6() throws FileNotFoundException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Config c = gson.fromJson(new FileReader("defaultconfig.json"), Config.class);

        if (c.isAutoDetect()) {
            c.autoConfig();
        }

        String appPath = c.getAppPath();
        List<String> epoints = c.getEpoints();
        String libPath = c.getLibPath();
        String project = c.getProject();

        ReuseableInfoflow infoflow = IFFactory.buildReuseable(appPath, c.getExcludes().stream().toList());

        List<RuleResult> ruleResult = new ArrayList<>();
        for (Config.Rule r : c.getRules()) {
            infoflow.computeInfoflow(appPath, libPath, epoints, r.getSources(), r.getSinks());
            List<DetectedResult> results = PathOptimization.detectedResults(infoflow, infoflow.getICFG(), project);
            ruleResult.add(new RuleResult(r.getName(), results));
        }
        try {
            String json = gson.toJson(ruleResult);
            Writer writer = new FileWriter("rule_result.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test7() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        ClassPathResource defaultConfig = new ClassPathResource("defaultconfig.json");
        Config c = gson.fromJson(new InputStreamReader(defaultConfig.getInputStream()), Config.class);

        c.setProject("test");
        c.setJdk("/home/ran/Documents/work/thusa2/ifpc-testcase/jdk/rt.jar");
        if (c.isAutoDetect()) {
            c.autoConfig();
        }

        String appPath = c.getAppPath();
        List<String> epoints = c.getEpoints();
        String libPath = c.getLibPath();
        String project = c.getProject();

        ReuseableInfoflow infoflow = IFFactory.buildReuseable(appPath, c.getExcludes().stream().toList(), c.getCallgraphAlgorithm());

        List<RuleResult> ruleResult = new ArrayList<>();
        for (Config.Rule r : c.getRules()) {
            infoflow.computeInfoflow(appPath, libPath, epoints, r.getSources(), r.getSinks());
            List<DetectedResult> results = PathOptimization.detectedResults(infoflow, infoflow.getICFG(), project);
            ruleResult.add(new RuleResult(r.getName(), results));
        }
        try {
            String json = gson.toJson(ruleResult);
            Writer writer = new FileWriter("rule_result.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PathOptimization.deteleTempdir(c.getTempDir());
    }


}
