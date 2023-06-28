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
    public void test1(){


        IInfoflowConfig infoflowConfig = (options, config) -> {

            List<String> includeList = new LinkedList<>();
            options.set_include(includeList);
            // options.set_exclude(excludeList);
            //options.set_no_bodies_for_excluded(true);
            //options.set_allow_phantom_refs(true);
            options.set_ignore_classpath_errors(true);
            options.set_output_format(Options.output_format_jimple);
            //options.set_output_format(Options.output_format_none);
            config.setWriteOutputFiles(true);
            config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
            options.set_keep_line_number(true);
            options.set_prepend_classpath(true);
            options.set_src_prec(Options.src_prec_only_class);
            options.setPhaseOption("jb", "use-original-names:true");
            options.setPhaseOption("cg","all-reachable:true");
            options.setPhaseOption("cg", "types-for-invoke:true");

        };

        InfoflowConfiguration config = new InfoflowConfiguration();
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setImplicitFlowMode(InfoflowConfiguration.ImplicitFlowMode.ArrayAccesses);

        Infoflow infoflow = new Infoflow();
        infoflow.setConfig(config);
        infoflow.setSootConfig(infoflowConfig);






    }
}
