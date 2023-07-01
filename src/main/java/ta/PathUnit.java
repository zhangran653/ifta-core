package ta;

public class PathUnit {
    private String hint;
    private String file;

    private String jspFile;
    private String javaClass;
    private String function;
    private String jimpleStmt;
    private String javaStmt;
    private String jspStmt;
    private int line;

    private Integer jspLine;

    public String getJspFile() {
        return jspFile;
    }

    public void setJspFile(String jspFile) {
        this.jspFile = jspFile;
    }

    public Integer getJspLine() {
        return jspLine;
    }

    public void setJspLine(int jspLine) {
        this.jspLine = jspLine;
    }

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