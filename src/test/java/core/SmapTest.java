package core;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
}
