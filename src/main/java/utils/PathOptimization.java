package utils;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.LineNumberTag;
import ta.PathUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PathOptimization {

    public static List<PathUnit> resultPath(IInfoflowCFG icfg, ResultSourceInfo source, ResultSinkInfo sink) {
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
            path.add(unit);

        }
        return path;
    }

    public static List<String> pathStm(ResultSourceInfo source) {
        return Arrays.stream(source.getPath()).map(String::valueOf).toList();
    }
}
