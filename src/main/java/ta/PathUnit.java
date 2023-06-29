package ta;

public class PathUnit {
    private String hint;
    private String file;

    private String javaClass;
    private String function;
    private String jimpleStmt;
    private String javaStmt;
    private String jspStmt;
    private int line;

    public String getJavaClass() {
        return javaClass;
    }

    public void setJavaClass(String javaClass) {
        this.javaClass = javaClass;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getJimpleStmt() {
        return jimpleStmt;
    }

    public void setJimpleStmt(String jimpleStmt) {
        this.jimpleStmt = jimpleStmt;
    }

    public String getJavaStmt() {
        return javaStmt;
    }

    public void setJavaStmt(String javaStmt) {
        this.javaStmt = javaStmt;
    }

    public String getJspStmt() {
        return jspStmt;
    }

    public void setJspStmt(String jspStmt) {
        this.jspStmt = jspStmt;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }
}