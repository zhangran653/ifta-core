package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import heros.solver.Pair;
import org.junit.Test;
import soot.*;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class InfoflowTest {
    @Test
    public void test1() throws IOException {

        Gson gson = new Gson();
        Map m = gson.fromJson(new FileReader("config.json"), HashMap.class);
        String appPath = (String)m.get("appPath");
        List<String> lp = (ArrayList<String>)m.get("libPaths");
        List<String> epoints =   (ArrayList<String>)m.get("epoints");
        List<String> sources =   (ArrayList<String>)m.get("sources");
        List<String> sinks =   (ArrayList<String>)m.get("sources");
        List<String> excludes =   (ArrayList<String>)m.get("excludes");


        List<String> realLibPath = new ArrayList<>();
        for (var path : lp) {
            if (path.endsWith(".jar")) {
                realLibPath.add(path);
            } else {
                File file = new File(path);
                if (file.isDirectory()) {
                    realLibPath.addAll(
                            Arrays.stream(Objects.requireNonNull(file.listFiles()))
                                    .filter(f -> f.getName().endsWith(".jar"))
                                    .map(File::getPath)
                                    .toList());
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

        Infoflow infoflow = new Infoflow();
        infoflow.setConfig(config);
        infoflow.setSootConfig(sootConfig);

        EasyTaintWrapper easyTaintWrapper = EasyTaintWrapper.getDefault();
        infoflow.setTaintWrapper(easyTaintWrapper);

        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        InfoflowResults results = infoflow.getResults();

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        try {
            String json = gson.toJson(new OutputInfoflowResults(results));
            Writer writer = new FileWriter("result.json");
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



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
