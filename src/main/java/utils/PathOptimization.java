package utils;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.LineNumberTag;
import ta.DetectedResult;
import ta.PathUnit;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

public class PathOptimization {

    public static List<PathUnit> resultPath(IInfoflowCFG icfg, ResultSourceInfo source, ResultSinkInfo sink) {
        return resultPath(icfg, source, sink, null);
    }

    public static List<PathUnit> resultPath(IInfoflowCFG icfg, ResultSourceInfo source, ResultSinkInfo sink, String projectDir) {
        ArrayList<PathUnit> path = new ArrayList<>();
        Stmt[] pathStmts = source.getPath();
        if (pathStmts == null) {
            pathStmts = new Stmt[]{source.getStmt(), sink.getStmt()};
        }
        for (Stmt p : pathStmts) {
            PathUnit unit = new PathUnit();
            SootMethod inFunction = icfg.getMethodOf(p);
            unit.setJavaClass(inFunction.getDeclaringClass().getName());
            unit.setFunction(inFunction.getSignature());
            unit.setJimpleStmt(p.toString());
            unit.setLine(p.getJavaSourceStartLineNumber());
            // java source file
            if (projectDir != null && !projectDir.isBlank()) {
                String file = locateSourceFile(projectDir, unit.getJavaClass());
                unit.setFile(file);
            }
            path.add(unit);
        }
        return path;
    }

    public static List<String> pathStm(ResultSourceInfo source) {
        return Arrays.stream(source.getPath()).map(String::valueOf).toList();
    }

    public static List<DetectedResult> detectedResults(Infoflow infoflow, IInfoflowCFG iCFG, String projectDir) {
        InfoflowResults infoflowResults = infoflow.getResults();
        List<DetectedResult> results = new ArrayList<>();
        if (!infoflowResults.isEmpty()) {
            for (ResultSinkInfo sink : infoflowResults.getResults().keySet()) {
                String sinkSig = sink.getDefinition().toString();
                for (ResultSourceInfo source : infoflowResults.getResults().get(sink)) {
                    DetectedResult result = new DetectedResult();
                    result.setSinkSig(sinkSig);
                    result.setSourceSig(source.getDefinition().toString());
                    result.setPath(PathOptimization.resultPath(iCFG, source, sink, projectDir));
                    result.setPathStm(PathOptimization.pathStm(source));
                    if (result.getPath().size() > 0) {
                        results.add(result);
                    }
                }
            }
        }
        return results;
    }

    public static List<DetectedResult> detectedResults(Infoflow infoflow, IInfoflowCFG iCFG) {
        return detectedResults(infoflow, iCFG, null);
    }

    public static List<String> filterFile(String baseDir, String[] patterns) {
        DirectoryScanner scanner = new DirectoryScanner();
        //new String[]{"**/*.java"}
        scanner.setIncludes(patterns);
        scanner.setBasedir(baseDir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return Arrays.asList(scanner.getIncludedFiles());
    }

    public static Path createTempdir() {
        try {
            return Files.createTempDirectory("").toFile().toPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deteleTempdir(String path) {
        String tmpDirsLocation = System.getProperty("java.io.tmpdir");
        if (path.startsWith(tmpDirsLocation)) {
            try {
                FileUtils.deleteDirectory(new File(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String md5(String filePath) {
        byte[] data;
        try {
            data = Files.readAllBytes(Paths.get(filePath));
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            return new BigInteger(1, hash).toString(16);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    public static void copy(File original, File copied) {
        try {
            FileUtils.copyFile(original, copied);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String removePrefix(String s, String prefix) {
        if (s != null && s.startsWith(prefix)) {
            return s.split(prefix, 2)[1];
        }
        return s;
    }

    public static String className(String filePath) {
        ClassParser classParser = new ClassParser(filePath);
        JavaClass javaClass;
        try {
            javaClass = classParser.parse();
            return javaClass.getClassName();
        } catch (IOException | ClassFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String classPackageName(String filePath) {
        ClassParser classParser = new ClassParser(filePath);
        JavaClass javaClass;
        try {
            javaClass = classParser.parse();
            return javaClass.getPackageName();
        } catch (IOException | ClassFormatException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String packageToDirString(String packageName) {
        return packageName.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
    }

    public static String locateSourceFile(String projectDir, String javaClass) {
        if (javaClass.contains("$")) {
            javaClass = javaClass.substring(0, javaClass.indexOf("$"));
        }
        String path = packageToDirString(javaClass) + ".java";
        List<String> sourceFile = filterFile(projectDir, new String[]{"**/" + path});
        if (sourceFile.isEmpty()) {
            return null;
        }
        return sourceFile.get(0);
    }


}
