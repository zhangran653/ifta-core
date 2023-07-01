package utils;

import java.util.ArrayList;
import java.util.List;

public class SmapInfo {

    private String sourceFilePath;

    private List<LineInfo> lineInfoList;

    List<LineInfo.LineMapping> lineMappings;

    public void addLineInfo(LineInfo lineInfo) {
        if (lineInfoList == null) {
            lineInfoList = new ArrayList<>();
        }
        lineInfoList.add(lineInfo);
    }

    public void addLineInfo(String lineString) {
        String[] l = lineString.split(":");
        String[] input = l[0].split(",");
        String[] output = l[1].split(",");
        LineInfo lineInfo = new LineInfo();
        if (input.length == 1) {
            lineInfo.setInputStartLine(Integer.valueOf(input[0]));
        } else {
            lineInfo.setInputStartLine(Integer.valueOf(input[0]));
            lineInfo.setRepeatCount(Integer.valueOf(input[1]));
        }

        if (output.length == 1) {
            lineInfo.setOutputStartLine(Integer.valueOf(output[0]));
        } else {
            lineInfo.setOutputStartLine(Integer.valueOf(output[0]));
            lineInfo.setOutputLineIncrement(Integer.valueOf(output[1]));
        }

        addLineInfo(lineInfo);
    }

    private void cal() {
        lineInfoList.forEach(LineInfo::mapLine);
    }

    public List<LineInfo.LineMapping> getMapping() {
        if (lineMappings == null) {
            cal();
            List<LineInfo.LineMapping> list = new ArrayList<>();
            for (var l : lineInfoList) {
                list.addAll(l.getLineMapping());
            }
            lineMappings = list;
            return lineMappings;
        }
        return lineMappings;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public List<LineInfo> getLineInfoList() {
        return lineInfoList;
    }

    public void setLineInfoList(List<LineInfo> lineInfoList) {
        this.lineInfoList = lineInfoList;
    }


    public static class LineInfo {
        //源文件行号 # 源文件代号,重复次数 : 目标文件开始行号,目标文件行号每次增加的数量
        //(InputStartLine # LineFileID , RepeatCount : OutputStartLine , OutputLineIncrement)

        private Integer inputStartLine;
        private Integer repeatCount;
        private Integer outputStartLine;
        private Integer outputLineIncrement;

        private List<LineMapping> lineMapping = new ArrayList<>();

        public List<LineMapping> getLineMapping() {
            return lineMapping;
        }

        public void setLineMapping(List<LineMapping> lineMapping) {
            this.lineMapping = lineMapping;
        }

        public void mapLine() {
            if (repeatCount == null) {
                repeatCount = 1;
            }
            if (outputLineIncrement == null) {
                outputLineIncrement = 0;
            }
            for (int repeat = 0; repeat < repeatCount; repeat++) {
                LineMapping m = new LineMapping();
                m.setInputLine(inputStartLine + repeat);
                int startInc = outputLineIncrement == 0 ? repeat : repeat * outputLineIncrement;
                m.setOutputBeginLine(outputStartLine + startInc);
                int endInc = outputLineIncrement == 0 ? 0 : outputLineIncrement - 1;
                m.setOutputEndLine(outputStartLine + startInc + endInc);
                lineMapping.add(m);
            }

        }

        public static class LineMapping {
            private Integer inputLine;
            private Integer outputBeginLine;
            private Integer outputEndLine;

            public Integer getInputLine() {
                return inputLine;
            }

            public void setInputLine(Integer inputLine) {
                this.inputLine = inputLine;
            }

            public Integer getOutputBeginLine() {
                return outputBeginLine;
            }

            public void setOutputBeginLine(Integer outputBeginLine) {
                this.outputBeginLine = outputBeginLine;
            }

            public Integer getOutputEndLine() {
                return outputEndLine;
            }

            public void setOutputEndLine(Integer outputEndLine) {
                this.outputEndLine = outputEndLine;
            }
        }

        public Integer getInputStartLine() {
            return inputStartLine;
        }

        public void setInputStartLine(Integer inputStartLine) {
            this.inputStartLine = inputStartLine;
        }

        public Integer getRepeatCount() {
            return repeatCount;
        }

        public void setRepeatCount(Integer repeatCount) {
            this.repeatCount = repeatCount;
        }

        public Integer getOutputStartLine() {
            return outputStartLine;
        }

        public void setOutputStartLine(Integer outputStartLine) {
            this.outputStartLine = outputStartLine;
        }

        public Integer getOutputLineIncrement() {
            return outputLineIncrement;
        }

        public void setOutputLineIncrement(Integer outputLineIncrement) {
            this.outputLineIncrement = outputLineIncrement;
        }
    }

}
