package utils;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;
import ta.ReuseableInfoflow;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IFFactory {

    public static ReuseableInfoflow buildReuseable(String appPath, List<String> excludes, int pathReconstructionTimeout, String callgraphAlgorithm) {
        // soot config
        IInfoflowConfig sootConfig = (options, config) -> {
            options.set_ignore_classpath_errors(true);
            options.set_keep_line_number(true);
            options.set_no_bodies_for_excluded(true);
            options.set_prepend_classpath(true);
            options.set_src_prec(Options.src_prec_only_class);
            options.set_ignore_resolution_errors(true);
            options.set_exclude(excludes);
            options.set_process_dir(Arrays.asList(appPath.split(File.pathSeparator)));
            options.set_keep_offset(true);
            options.set_allow_phantom_refs(true);
        };

        // infoflow config
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
        config.getPathConfiguration().setPathReconstructionTimeout(pathReconstructionTimeout);
        // CHA may find more results but could be false positive.
        if (callgraphAlgorithm == null || callgraphAlgorithm.isBlank()) {
            config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA);
        } else if (callgraphAlgorithm.equals("CHA")) {
            config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA);
        } else if (callgraphAlgorithm.equals("SPARK")) {
            config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);
        } else if (callgraphAlgorithm.equals("VTA")) {
            config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.VTA);
        } else if (callgraphAlgorithm.equals("RTA")) {
            config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.RTA);
        } else if (callgraphAlgorithm.equals("GEOM")) {
            config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.GEOM);
        } else {
            throw new RuntimeException("callgraph algorithm " + callgraphAlgorithm + " not supported!");
        }

        soot.G.reset();
        ReuseableInfoflow infoflow = new ReuseableInfoflow("", false);
        infoflow.setConfig(config);
        infoflow.setSootConfig(sootConfig);

        EasyTaintWrapper wrapper;
        try {
            ClassPathResource classPathResource = new ClassPathResource("EasyTaintWrapperSource.txt");
            wrapper = new EasyTaintWrapper(classPathResource.getInputStream());
            infoflow.setTaintWrapper(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return infoflow;

    }

    public static ReuseableInfoflow buildReuseable(String appPath, List<String> excludes, int pathReconstructionTimeout) {
        return buildReuseable(appPath, excludes, pathReconstructionTimeout, null);
    }

    public static ReuseableInfoflow buildReuseable(String appPath, List<String> excludes, String callgraphAlgorithm) {
        return buildReuseable(appPath, excludes, 180, callgraphAlgorithm);
    }

    public static ReuseableInfoflow buildReuseable(String appPath, List<String> excludes) {
        return buildReuseable(appPath, excludes, 180, null);
    }

    public static ReuseableInfoflow buildReuseable() {
        return buildReuseable("", Collections.emptyList(), 180, null);
    }
}
