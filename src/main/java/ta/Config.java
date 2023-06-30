package ta;

import java.io.File;
import java.util.*;

public class Config {
    private List<String> epoints = Collections.emptyList();

    private List<String> sources = Collections.emptyList();

    private List<String> sinks = Collections.emptyList();

    private String appPath;

    private List<String> libPaths = Collections.emptyList();

    private String libPath;

    private List<String> excludes = Collections.emptyList();

    private int pathReconstructionTimeout = 180;

    private int maxPathLength = 75;

    public void scanLib() {
        List<String> realLibPath = new ArrayList<>();
        for (var path : libPaths) {
            if (path.endsWith(".jar")) {
                realLibPath.add(path);
            } else {
                File file = new File(path);
                if (file.isDirectory()) {
                    realLibPath.addAll(Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getName().endsWith(".jar")).map(File::getPath).toList());
                }
            }
        }
        libPath = String.join(File.pathSeparator, realLibPath);
    }


    public String getLibPath() {
        return libPath;
    }

    public void setLibPath(String libPath) {
        this.libPath = libPath;
    }

    public int getPathReconstructionTimeout() {
        return pathReconstructionTimeout;
    }

    public void setPathReconstructionTimeout(int pathReconstructionTimeout) {
        this.pathReconstructionTimeout = pathReconstructionTimeout;
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

    public void setMaxPathLength(int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public List<String> getEpoints() {
        return epoints;
    }

    public void setEpoints(List<String> epoints) {
        this.epoints = epoints;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public List<String> getSinks() {
        return sinks;
    }

    public void setSinks(List<String> sinks) {
        this.sinks = sinks;
    }

    public String getAppPath() {
        return appPath;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }

    public List<String> getLibPaths() {
        return libPaths;
    }

    public void setLibPaths(List<String> libPaths) {
        this.libPaths = libPaths;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
}
