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
import utils.PathOptimization;

import java.io.*;
import java.util.*;

public class ReuseableInfoflowTest {

    @Test
    public void test1() throws IOException {

        Gson gson = new Gson();
        Config c = gson.fromJson(new FileReader("config.json"), Config.class);
        String appPath = c.getAppPath();
        List<String> lp = c.getLibPaths();
        List<String> epoints = c.getEpoints();
        List<String> sources =c.getSources();
        List<String> sinks = c.getSinks();
        List<String> excludes = c.getExcludes();


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
}
