package core;

import org.junit.Test;
import ta.AnalysisExecutor;

public class AnalysisExecutorTest {

    @Test
    public void test1() {
        // Project to be analysis. Can be directory path, .jar file or .zip file path.
        String project = "/home/ran/Documents/work/thusa2/ifpc-testcase/WebGoat-5.0";
        // jdk path for the project. can be omitted if configuration file contains it or "libPath" of config includes it.
        String jdk = "/home/ran/Documents/work/thusa2/ifpc-testcase/jdk/rt.jar";
        String result = "test_result.json";
        AnalysisExecutor analysisExecutor = AnalysisExecutor
                .newInstance()
                .withDefaultConfig()
                .setProject(project)
                .setJDK(jdk)
                // default is "SPARK"
                .setCallGraphAlgorithm("SPARK")
                // Track source file and calculate line number of jsp. Default false.
                .trackSourceFile(true)
                // Path reconstruction time out.
                .setTimeout(180)
                // write detect result to file.
                .writeOutput(true)
                .setOutput(result)
                .analysis();

        // detect result.
        var ruleResult = analysisExecutor.getRuleResult();
        System.out.println(ruleResult);
    }

    @Test
    public void test2() {
        // User defined config.
        String configPath = "config4.json";
        String result = "test_result.json";
        AnalysisExecutor analysisExecutor = AnalysisExecutor
                .newInstance()
                .withConfig(configPath)
                .setCallGraphAlgorithm("SPARK")
                // Track source file and calculate line number of jsp. Default false.
                .trackSourceFile(true)
                .writeOutput(true)
                .setOutput(result)
                .analysis();

        // detect result.
        var ruleResult = analysisExecutor.getRuleResult();
        System.out.println(ruleResult);
    }

    @Test
    public void test3() {
        // TODO Make entry point selector configurable.
    }

    @Test
    public void test4(){
        // test WebGoat
        String project = "/home/ran/Documents/work/thusa2/testprojects/WebGoat-5.0";
        String jdk = "jdk/rt.jar";
        String result = "result/webgoat_result.json";
        AnalysisExecutor analysisExecutor = AnalysisExecutor
                .newInstance()
                .withDefaultConfig()
                .setProject(project)
                .setJDK(jdk)
                // default is "SPARK"
                .setCallGraphAlgorithm("SPARK")
                // Track source file and calculate line number of jsp. Default false.
                .trackSourceFile(true)
                // Path reconstruction time out.
                .setTimeout(180)
                // write detect result to file.
                .writeOutput(true)
                .setOutput(result)
                .analysis();

    }

    @Test
    public void test5(){
        // test jsp
        String project = "/home/ran/Documents/work/thusa2/testprojects/jsp-demo";
        String jdk = "jdk/rt.jar";
        String result = "result/jsp_result.json";
        AnalysisExecutor analysisExecutor = AnalysisExecutor
                .newInstance()
                .withDefaultConfig()
                .setProject(project)
                .setJDK(jdk)
                // default is "SPARK"
                .setCallGraphAlgorithm("SPARK")
                // Track source file and calculate line number of jsp. Default false.
                .trackSourceFile(true)
                // Path reconstruction time out.
                .setTimeout(180)
                // write detect result to file.
                .writeOutput(true)
                .setOutput(result)
                .analysis();
    }

    @Test
    public void test6(){
        // test zip
        String project = "/home/ran/Documents/work/thusa2/testprojects/jsp-demo.zip";
        String jdk = "jdk/rt.jar";
        String result = "result/zip_result.json";
        AnalysisExecutor analysisExecutor = AnalysisExecutor
                .newInstance()
                .withDefaultConfig()
                .setProject(project)
                .setJDK(jdk)
                // default is "SPARK"
                .setCallGraphAlgorithm("SPARK")
                // Track source file and calculate line number of jsp. Default false.
                .trackSourceFile(true)
                // Path reconstruction time out.
                .setTimeout(180)
                // write detect result to file.
                .writeOutput(true)
                .setOutput(result)
                .analysis();
    }

    @Test
    public void test7(){
        // test jar
        String project = "/home/ran/Documents/work/thusa2/testprojects/rm-broker.jar";
        String jdk = "jdk/rt.jar";
        String result = "result/jar_result.json";
        AnalysisExecutor analysisExecutor = AnalysisExecutor
                .newInstance()
                .withDefaultConfig()
                .setProject(project)
                .setJDK(jdk)
                // default is "SPARK"
                .setCallGraphAlgorithm("CHA")
                // Track source file and calculate line number of jsp. Default false.
                .trackSourceFile(true)
                // Path reconstruction time out.
                .setTimeout(180)
                // write detect result to file.
                .writeOutput(true)
                .setOutput(result)
                .analysis();
    }
}
