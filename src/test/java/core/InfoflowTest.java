package core;

import org.junit.Test;
import soot.Scene;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.options.Options;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InfoflowTest {
    @Test
    public void test1() {

        // Infoflow will set up soot configuration when set soot integration mode to 'CreateNewInstance' (by default)
        // Default soot configuration is in AbstractInfoflow's initializeSoot method.
        IInfoflowConfig sootConfig = (options, config) -> {
            options.set_ignore_classpath_errors(true);
            config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
            options.set_keep_line_number(true);
            options.set_prepend_classpath(true);
            options.set_src_prec(Options.src_prec_only_class);
            options.set_ignore_resolution_errors(true);
        };

        InfoflowConfiguration config = new InfoflowConfiguration();
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.RemoveSideEffectFreeCode);
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
        //infoflow.computeInfoflow();


    }
}
