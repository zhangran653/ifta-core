package ta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import utils.ClassPathResource;
import utils.IFFactory;
import utils.PathOptimization;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AnalysisExecutor {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final ClassPathResource defaultConfig = new ClassPathResource("defaultconfig.json");

    private Config config;

    private boolean useDefault;

    private ReuseableInfoflow infoflow;

    private String output = "./result.json";

    private List<RuleResult> ruleResult;

    private boolean writeOutput;

    private boolean trackSourceFile;

    public boolean isTrackSourceFile() {
        return trackSourceFile;
    }

    public Config getConfig() {
        return config;
    }

    public boolean isUseDefault() {
        return useDefault;
    }

    public ReuseableInfoflow getInfoflow() {
        return infoflow;
    }

    public String getOutput() {
        return output;
    }

    public List<RuleResult> getRuleResult() {
        return ruleResult;
    }

    public boolean isWriteOutput() {
        return writeOutput;
    }

    public static AnalysisExecutor newInstance() {
        return new AnalysisExecutor();
    }

    public AnalysisExecutor withDefaultConfig() {
        try {
            this.config = gson.fromJson(new InputStreamReader(defaultConfig.getInputStream()), Config.class);
            this.useDefault = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public AnalysisExecutor withConfig(String configFilePath) {
        try {
            this.config = gson.fromJson(new FileReader(configFilePath), Config.class);
            this.useDefault = false;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public AnalysisExecutor setProject(String project) {
        this.config.setProject(project);
        return this;
    }

    public AnalysisExecutor setJDK(String jdk) {
        this.config.setJdk(jdk);
        return this;
    }

    public AnalysisExecutor setOutput(String output) {
        this.output = output;
        return this;
    }

    public AnalysisExecutor setTimeout(int timeout) {
        this.config.setPathReconstructionTimeout(timeout);
        return this;
    }

    public AnalysisExecutor writeOutput(boolean writeOutput) {
        this.writeOutput = writeOutput;
        return this;
    }

    public AnalysisExecutor setCallGraphAlgorithm(String callGraphAlgorithm) {
        this.config.setCallgraphAlgorithm(callGraphAlgorithm);
        return this;
    }

    public AnalysisExecutor trackSourceFile(boolean trackSourceFile) {
        this.trackSourceFile = trackSourceFile;
        return this;
    }

    public AnalysisExecutor setEntrySelector(String entrySelector){
        this.config.setEntrySelector(entrySelector);
        return this;
    }


    public AnalysisExecutor analysis() {
        if (config == null) {
            throw new AssertionError("config must be set before analysis.");
        }
        if (config.getProject() == null || config.getProject().isBlank()) {
            throw new AssertionError("project must be set before analysis.");
        }
        if (config.getRules() == null || config.getRules().isEmpty()) {
            throw new AssertionError("rules must not be empty.");
        }
        config.autoConfig();
        if (!useDefault) {
            List<String> realLibPath = new ArrayList<>();
            for (var path : config.getLibPaths()) {
                if (path.endsWith(".jar")) {
                    realLibPath.add(path);
                } else {
                    File file = new File(path);
                    if (file.isDirectory()) {
                        realLibPath.addAll(Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getName().endsWith(".jar")).map(File::getPath).toList());
                    }
                }
            }
            config.setLibPath(String.join(File.pathSeparator, realLibPath));
            if (config.getJdk() != null && !config.getJdk().isBlank()) {
                config.setLibPath(config.getLibPath() + File.pathSeparator + config.getJdk());
            }
        }
        String appPath = config.getAppPath();
        List<String> epoints = config.getEpoints();
        String libPath = config.getLibPath();
        String project = config.getProject();
        String tempdir = config.getTempDir();
        List<String> excludes = config.getExcludes().stream().toList();
        int timeout = config.getPathReconstructionTimeout();
        String callGraphAlgorithm = config.getCallgraphAlgorithm();

        infoflow = IFFactory.buildReuseable(appPath, excludes, timeout, callGraphAlgorithm);

        List<RuleResult> ruleResult = new ArrayList<>();
        for (Config.Rule r : config.getRules()) {
            infoflow.computeInfoflow(appPath, libPath, epoints, r.getSources(), r.getSinks());
            List<DetectedResult> results = PathOptimization.detectedResults(infoflow, infoflow.getICFG(), project, trackSourceFile);
            ruleResult.add(new RuleResult(r.getName(), results));
        }
        this.ruleResult = ruleResult;
        if (writeOutput) {
            try {
                String json = gson.toJson(ruleResult);
                Writer writer = new FileWriter(output);
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PathOptimization.deteleTempdir(config.getTempDir());
        return this;
    }

}
