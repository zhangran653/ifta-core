package ta;

import java.util.Collections;
import java.util.List;

public class Config {
    private List<String> epoints = Collections.emptyList();

    private List<String> sources = Collections.emptyList();

    private List<String> sinks = Collections.emptyList();

    private String appPath;

    private List<String> libPaths = Collections.emptyList();

    private List<String> excludes = Collections.emptyList();


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
