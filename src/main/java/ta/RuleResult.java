package ta;

import java.util.Collections;
import java.util.List;

public class RuleResult {
    private String ruleName;
    private List<DetectedResult> result = Collections.emptyList();

    private int resultCount;

    public RuleResult() {
    }

    public RuleResult(String ruleName, List<DetectedResult> result) {
        this.ruleName = ruleName;
        this.result = result;
        this.resultCount = result.size();
    }

    public int getResultCount() {
        return resultCount;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public List<DetectedResult> getResult() {
        return result;
    }

    public void setResult(List<DetectedResult> result) {
        this.result = result;
    }
}
