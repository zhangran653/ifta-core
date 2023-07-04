package pc;

import org.junit.Test;
import ta.AnalysisExecutor;

public class PathCheckTest {

    @Test
    public void test1() {
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
                //TODO set default path checker
                .setPathChecker("default")
                .analysis();
    }
}
