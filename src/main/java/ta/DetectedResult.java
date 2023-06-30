package ta;

import java.util.List;

public class DetectedResult {
    enum Level {
        LOW, MEDIUM, HIGH
    }
    private String description;
    private Level severity;
    private Level confidence;
    private String sourceSig;
    private String sinkSig;
    private List<PathUnit> path;

    private List<String> pathStm;

    public List<String> getPathStm() {
        return pathStm;
    }

    public void setPathStm(List<String> pathStm) {
        this.pathStm = pathStm;
    }

    public Level getSeverity() {
        return severity;
    }

    public void setSeverity(Level severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Level getConfidence() {
        return confidence;
    }

    public void setConfidence(Level confidence) {
        this.confidence = confidence;
    }

    public String getSourceSig() {
        return sourceSig;
    }

    public void setSourceSig(String sourceSig) {
        this.sourceSig = sourceSig;
    }

    public String getSinkSig() {
        return sinkSig;
    }

    public void setSinkSig(String sinkSig) {
        this.sinkSig = sinkSig;
    }

    public List<PathUnit> getPath() {
        return path;
    }

    public void setPath(List<PathUnit> path) {
        this.path = path;
    }
}