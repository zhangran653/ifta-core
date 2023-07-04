import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import ta.AnalysisExecutor;

public class Main {
    /**
     * usage: ta [-h] [-dc {true,false}] [-c CONFIG] [-p PROJECT] [-j JDK]
     *           [-t {true,false}] [-w {true,false}] [-o OUTPUT]
     *           [-cg {CHA,SPARK,VTA,RTA,GEOM}] [-to TIMEOUT]
     *
     * Run taint analysis of given project.
     *
     * named arguments:
     *   -h, --help             show this help message and exit
     *   -dc {true,false}, --defaultconfig {true,false}
     *                          Specify if use default config. (default: true)
     *   -c CONFIG, --config CONFIG
     *                          User defined config file path.
     *   -p PROJECT, --project PROJECT
     *                          Project to be analysis.  Can  be directory path, .
     *                          jar file or .zip file path.
     *   -j JDK, --jdk JDK      Jdk path  for  the  project.  can  be  omitted  if
     *                          configuration file  contains  it  or  "libPath" of
     *                          config includes it.
     *   -t {true,false}, --track {true,false}
     *                          Track source file  and  calculate  line  number of
     *                          jsp files. (default: false)
     *   -w {true,false}, --write {true,false}
     *                          Write detect result to file. (default: true)
     *   -o OUTPUT, --output OUTPUT
     *                          Out put file path.
     *   -cg {CHA,SPARK,VTA,RTA,GEOM}, --callgraph {CHA,SPARK,VTA,RTA,GEOM}
     *                          Call graph algorithm. (default: SPARK)
     *   -to TIMEOUT, --timeout TIMEOUT
     *                          Path reconstruction time out. (default: 180)
     * @param args
     */
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("ta").build()
                .defaultHelp(true)
                .description("Run taint analysis of given project.");
        parser.addArgument("-dc", "--defaultconfig")
                .choices("true", "false").setDefault("true")
                .help("Specify if use default config.");
        parser.addArgument("-c", "--config")
                .help("User defined config file path.");
        parser.addArgument("-p", "--project")
                .help("Project to be analysis. Can be directory path, .jar file or .zip file path.");
        parser.addArgument("-j","--jdk")
                .help("Jdk path for the project. can be omitted if configuration file contains it or \"libPath\" of config includes it.");
        parser.addArgument("-t", "--track")
                .choices("true", "false").setDefault("false")
                .help("Track source file and calculate line number of jsp files.");
        parser.addArgument("-w", "--write")
                .choices("true", "false").setDefault("true")
                .help("Write detect result to file.");
        parser.addArgument("-o", "--output")
                .help("Out put file path.");
        parser.addArgument("-cg", "--callgraph")
                .choices("CHA", "SPARK", "VTA", "RTA", "GEOM").setDefault("SPARK")
                .help("Call graph algorithm.");
        parser.addArgument("-to", "--timeout")
                .setDefault(180).help("Path reconstruction time out.");
        parser.addArgument("-es", "--entryselector")
                .help("entry selectors, choose from 'JspServiceEntry','AnnotationTagEntry','PublicStaticOrMainEntry'. Multiple selectors can be set with ',' in between. Default all");
        parser.addArgument("-pc", "--pathchecker")
                .help("path checkers. choose from 'default'. Multiple selectors can be set with ',' in between.");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        AnalysisExecutor analysisExecutor = AnalysisExecutor.newInstance();
        if (ns.getString("defaultconfig").equals("true")) {
            analysisExecutor.withDefaultConfig();
        } else {
            String config = ns.getString("config");
            analysisExecutor.withConfig(config);
        }
        String project = ns.getString("project");
        if (project != null) {
            analysisExecutor.setProject(project);
        }
        String jdk = ns.getString("jdk");
        if (jdk != null) {
            analysisExecutor.setJDK(jdk);
        }
        String track = ns.getString("track");
        if (track != null) {
            analysisExecutor.trackSourceFile(Boolean.parseBoolean(track));
        }
        String write = ns.getString("write");
        if (write != null) {
            analysisExecutor.writeOutput(Boolean.parseBoolean(write));
        }

        String ouput = ns.getString("output");
        if (ouput != null) {
            analysisExecutor.setOutput(ouput);
        }
        String callgraph = ns.getString("callgraph");
        if (callgraph != null) {
            analysisExecutor.setCallGraphAlgorithm(callgraph);
        }
        String timeout = ns.getString("timeout");
        if (timeout != null) {
            analysisExecutor.setTimeout(Integer.parseInt(timeout));
        }
        String es = ns.getString("entryselector");
        if (es != null) {
            analysisExecutor.setEntrySelector(es);
        }
        String pc = ns.getString("pathchecker");
        if (pc != null) {
            analysisExecutor.setPathChecker(pc);
        }

        analysisExecutor.analysis();

    }

}
