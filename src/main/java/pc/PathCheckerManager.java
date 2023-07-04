package pc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ta.ReuseableInfoflow;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PathCheckerManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PathCheckerManager() {
    }

    public static PathCheckerManager buildPathCheckerManager() {
        return new PathCheckerManager();
    }

    public final Function<ReuseableInfoflow, PathCheckResult> defaultChecker = (i) -> {
        // TODO use ReuseableInfoflow to get results,get icfg and do other operations.
        logger.warn("##################This is the default path checker.################");

        return null;
    };
    // TODO add more path checker here if needed.

    public final Map<String, Function<ReuseableInfoflow, PathCheckResult>> namedCheckers = new HashMap<>() {{
        put("DEFAULT", defaultChecker);
    }};

    public List<Function<ReuseableInfoflow, PathCheckResult>> pathCheckerList(String checkerNames) {
        if (checkerNames == null || checkerNames.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(checkerNames.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(namedCheckers::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
