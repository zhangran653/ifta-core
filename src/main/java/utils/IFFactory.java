package utils;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;
import ta.Config;
import ta.ReuseableInfoflow;

import java.io.IOException;

public class IFFactory {

    public static ReuseableInfoflow buildReuseable(Config userConfig) {
        // soot config
        IInfoflowConfig sootConfig = (options, config) -> {
            options.set_ignore_classpath_errors(true);
            config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
            options.set_keep_line_number(true);
            options.set_prepend_classpath(true);
            options.set_src_prec(Options.src_prec_only_class);
            options.set_ignore_resolution_errors(true);
            options.set_exclude(userConfig.getExcludes());
            options.set_keep_offset(true);
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
        config.getPathConfiguration().setPathReconstructionTimeout(userConfig.getPathReconstructionTimeout());
        config.getPathConfiguration().setMaxPathLength(userConfig.getMaxPathLength());
        //
        ReuseableInfoflow infoflow = new ReuseableInfoflow("", false);
        infoflow.setConfig(config);
        infoflow.setSootConfig(sootConfig);

        EasyTaintWrapper easyTaintWrapper = null;
        try {
            easyTaintWrapper = EasyTaintWrapper.getDefault();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        infoflow.setTaintWrapper(easyTaintWrapper);

        return infoflow;

    }
}
