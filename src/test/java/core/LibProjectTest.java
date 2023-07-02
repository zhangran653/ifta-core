package core;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import utils.FileUtility;

import java.nio.file.Path;

public class LibProjectTest {

    @Test
    public void test1() {
        //FileUtility.flatExtractJar("D:\\jsp-demo.zip", "libtest/");
        FileUtility.flatExtractJar("libtest/sa-compile.jar", "libtest/");
    }

    @Test
    public void test2(){
        System.out.println(FilenameUtils.removeExtension(Path.of("libtest/sa-compile.jar").toString()));
    }

}
