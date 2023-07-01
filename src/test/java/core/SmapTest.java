package core;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import utils.SmapInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SmapTest {

    /**
     * SMAP
     * jsp_005fcustom_005fscript_005ffor_005foracle_jsp.java
     * JSP
     * *S JSP
     * *F
     * + 0 jsp_custom_script_for_oracle.jsp
     * jsp_custom_script_for_oracle.jsp
     * *L
     * 50,2:534,0
     * 54,407:29
     * 460:536,0
     * 462,66:538
     * 527:604,0
     * *E
     */

    //源文件行号 # 源文件代号,重复次数 : 目标文件开始行号,目标文件行号每次增加的数量
    //(InputStartLine # LineFileID , RepeatCount : OutputStartLine , OutputLineIncrement)
    @Test
    public void test1() {
        String smap = "SMAP\n" +
                "jsp_005fcustom_005fscript_005ffor_005foracle_jsp.java\n" +
                "JSP\n" +
                "*S JSP\n" +
                "*F\n" +
                "+ 0 jsp_custom_script_for_oracle.jsp\n" +
                "jsp_custom_script_for_oracle.jsp\n" +
                "*L\n" +
                "50,2:534,0\n" +
                "54,407:29\n" +
                "460:536,0\n" +
                "462,66:538\n" +
                "527:604,0\n" +
                "*E\n";
        System.out.println(smap);
    }

    @Test
    public void test2() throws FileNotFoundException {
        FileReader f = new FileReader("jsp_005fcustom_005fscript_005ffor_005foracle_jsp.class.smap");
        List<String> lineInfo = new ArrayList<>();
        boolean record = false;
        try {
            BufferedReader in = new BufferedReader
                    (f);
            String str;
            while ((str = in.readLine()) != null) {
                System.out.println(str);

                if (str.equals("*L")) {
                    record = true;
                    continue;
                }
                if (str.equals("*E")) {
                    break;
                }
                if (record) {
                    lineInfo.add(str);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(lineInfo);

    }

    private String readSourceDebug(String path) {
        ClassReader reader = null;
        try {
            reader = new ClassReader(new DataInputStream(new FileInputStream(path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClassNode cn = new ClassNode();
        reader.accept(cn, 0);
        return cn.sourceDebug;
    }

    private String getSourceJspPath(List<String> smap) {
        for (int i = 0; i < smap.size(); i++) {
            if (smap.get(i).equals("*F")) {
                return smap.get(i + 2);
            }
        }
        return null;
    }

    private List<String> getLineMapping(List<String> smap) {
        for (int i = 0; i < smap.size(); i++) {
            if (smap.get(i).equals("*L")) {
                return smap.subList(i + 1, smap.size() - 1);
            }
        }
        return Collections.emptyList();
    }


    @Test
    public void test3() throws IOException {
        String[] files = {
                "compiledjsp/org/apache/jsp/jsp_005fcustom_005fscript_005ffor_005foracle_jsp.class",
                "compiledjsp/org/apache/jsp/foo1/jsp_005fcustom_005fspy_005ffor_005fmysql_jsp.class",
        };
        List<SmapInfo> smapInfoList = new ArrayList<>();
        for (String file : files) {
            SmapInfo smapInfo = new SmapInfo();
            String s = readSourceDebug(file);
            System.out.println(s);
            List<String> smap = Arrays.stream(s.split("\n")).toList();
            smapInfo.setSourceFilePath(getSourceJspPath(smap));
            for (var m : getLineMapping(smap)) {
                smapInfo.addLineInfo(m);
            }
            System.out.println(smapInfo.getSourceFilePath());
            smapInfo.getMapping().forEach(m -> {
                System.out.println(m.getInputLine() + " | " + m.getOutputBeginLine() + " : " + m.getOutputEndLine());
            });
            System.out.println();
        }

        //SMAP
        //jsp_005fcustom_005fspy_005ffor_005fmysql_jsp.java
        //JSP
        //*S JSP
        //*F
        //+ 0 jsp_custom_spy_for_mysql.jsp
        //foo1/jsp_custom_spy_for_mysql.jsp
        //*L
        //31,2:344,0
        //34,294:16
        //327:346,0
        //329,55:348
        //383:403,0
        //*E
    }

    @Test
    public void test4() {

    }
}
