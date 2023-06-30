package utils;

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

    public static List<DetectedResult> detectedResults(Infoflow infoflow, IInfoflowCFG iCFG) {
        InfoflowResults infoflowResults = infoflow.getResults();
        List<DetectedResult> results = new ArrayList<>();
        if (!infoflowResults.isEmpty()) {
            for (ResultSinkInfo sink : infoflowResults.getResults().keySet()) {
                String sinkSig = sink.getDefinition().toString();
                for (ResultSourceInfo source : infoflowResults.getResults().get(sink)) {
                    DetectedResult result = new DetectedResult();
                    result.setSinkSig(sinkSig);
                    result.setSourceSig(source.getDefinition().toString());
                    result.setPath(PathOptimization.resultPath(iCFG, source, sink));
                    result.setPathStm(PathOptimization.pathStm(source));
                    if (result.getPath().size() > 0) {
                        results.add(result);
                    }
                }
            }
        }
        return results;
    }
}
