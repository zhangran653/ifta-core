package ta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.*;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.FlowDroidMemoryManager;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IAbstractionPathBuilder;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.globalTaints.GlobalTaintManager;
import soot.jimple.infoflow.handlers.PostAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler2;
import soot.jimple.infoflow.memory.FlowDroidMemoryWatcher;
import soot.jimple.infoflow.memory.FlowDroidTimeoutWatcher;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.memory.reasons.OutOfMemoryReason;
import soot.jimple.infoflow.memory.reasons.TimeoutReason;
import soot.jimple.infoflow.problems.BackwardsInfoflowProblem;
import soot.jimple.infoflow.problems.InfoflowProblem;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.results.InfoflowPerformanceData;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.PredecessorShorteningMode;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.gcSolver.GCSolverPeerGroup;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.infoflow.sourcesSinks.manager.DefaultSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.IOneSourceAtATimeManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ReuseableInfoflow extends Infoflow {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private int useCount = 0;
    private boolean throwExceptions;

    public void setThrowExceptions(boolean b) {
        this.throwExceptions = b;
    }

    private boolean hasUse() {
        return useCount > 0;
    }

    public ReuseableInfoflow() {
        super();
    }

    public ReuseableInfoflow(String androidPath, boolean forceAndroidJar) {
        super(androidPath, forceAndroidJar, new CachedBiDiICFGFactory());
    }

    public IInfoflowCFG getICFG() {
        return ((CachedBiDiICFGFactory) this.icfgFactory).getiCFG();
    }

    @Override
    public void computeInfoflow(String appPath, String libPath, Collection<String> entryPoints,
                                Collection<String> sources, Collection<String> sinks) {
        if (!hasUse()) {
            this.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(entryPoints),
                    new DefaultSourceSinkManager(sources, sinks));

        } else {
            logger.info("\n---- reuse soot and iCFG to compute info flow ----\n");
            var sourcesSinks = new DefaultSourceSinkManager(sources, sinks);
            final InfoflowPerformanceData performanceData = createPerformanceDataClass();
            try {
                // Clear the data from previous runs
                results = new InfoflowResults();
                results.setPerformanceData(performanceData);

                // Print and check our configuration
                config.printSummary();

                // Register a memory watcher
                if (memoryWatcher != null) {
                    memoryWatcher.clearSolvers();
                    memoryWatcher = null;
                }
                memoryWatcher = new FlowDroidMemoryWatcher(results, config.getMemoryThreshold());
                // No need to build the call graph but still keep the timestamp to trace performance.
                long beforeCallgraph = System.nanoTime();
                // Initialize the source sink manager
                sourcesSinks.initialize();

                if (config.isTaintAnalysisEnabled())
                    runTaintAnalysis(sourcesSinks, null, getICFG(), performanceData);

                // Gather performance data
                performanceData.setTotalRuntimeSeconds((int) Math.round((System.nanoTime() - beforeCallgraph) / 1E9));
                performanceData.updateMaxMemoryConsumption(getUsedMemory());
                logger.info(String.format("Data flow solver took %d seconds. Maximum memory consumption: %d MB",
                        performanceData.getTotalRuntimeSeconds(), performanceData.getMaxMemoryConsumption()));

                // Provide the handler with the final results
                for (ResultsAvailableHandler handler : onResultsAvailable)
                    handler.onResultsAvailable(getICFG(), results);

                // Write the Jimple files to disk if requested
                if (config.getWriteOutputFiles())
                    PackManager.v().writeOutput();
            } catch (Exception ex) {
                StringWriter stacktrace = new StringWriter();
                PrintWriter pw = new PrintWriter(stacktrace);
                ex.printStackTrace(pw);
                results.addException(ex.getClass().getName() + ": " + ex.getMessage() + "\n" + stacktrace.toString());
                logger.error("Exception during data flow analysis", ex);
            }
        }
        useCount += 1;

    }

    private int getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (int) Math.round((runtime.totalMemory() - runtime.freeMemory()) / 1E6);
    }

    private void runTaintAnalysis(final ISourceSinkManager sourcesSinks, final Set<String> additionalSeeds,
                                  IInfoflowCFG iCfg, InfoflowPerformanceData performanceData) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        logger.info("Starting Taint Analysis");

        // Make sure that we have a path builder factory
        if (pathBuilderFactory == null)
            pathBuilderFactory = new DefaultPathBuilderFactory(config.getPathConfiguration());

        // Check whether we need to run with one source at a time
        IOneSourceAtATimeManager oneSourceAtATime = config.getOneSourceAtATime() && sourcesSinks != null
                && sourcesSinks instanceof IOneSourceAtATimeManager ? (IOneSourceAtATimeManager) sourcesSinks : null;

        // Reset the current source
        if (oneSourceAtATime != null)
            oneSourceAtATime.resetCurrentSource();
        boolean hasMoreSources = oneSourceAtATime == null || oneSourceAtATime.hasNextSource();

        while (hasMoreSources) {
            // Fetch the next source
            if (oneSourceAtATime != null)
                oneSourceAtATime.nextSource();

            // Create the executor that takes care of the workers
            int numThreads = Runtime.getRuntime().availableProcessors();
            InterruptableExecutor executor = executorFactory.createExecutor(numThreads, true, config);
            executor.setThreadFactory(new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    Thread thrIFDS = new Thread(r);
                    thrIFDS.setDaemon(true);
                    thrIFDS.setName("FlowDroid");
                    return thrIFDS;
                }

            });

            // Initialize the memory manager
            IMemoryManager<Abstraction, Unit> memoryManager = createMemoryManager();

            // Initialize our infrastructure for global taints
            final Set<IInfoflowSolver> solvers = new HashSet<>();
            GlobalTaintManager globalTaintManager = new GlobalTaintManager(solvers);

            // Initialize the data flow manager
            manager = initializeInfoflowManager(sourcesSinks, iCfg, globalTaintManager);

            // Create the solver peer group
            solverPeerGroup = new GCSolverPeerGroup();

            // Initialize the alias analysis
            Abstraction zeroValue = null;
            IAliasingStrategy aliasingStrategy = createAliasAnalysis(sourcesSinks, iCfg, executor, memoryManager);
            IInfoflowSolver backwardSolver = aliasingStrategy.getSolver();
            if (backwardSolver != null) {
                zeroValue = backwardSolver.getTabulationProblem().createZeroValue();
                solvers.add(backwardSolver);
            }

            // Initialize the aliasing infrastructure
            Aliasing aliasing = createAliasController(aliasingStrategy);
            if (dummyMainMethod != null)
                aliasing.excludeMethodFromMustAlias(dummyMainMethod);
            manager.setAliasing(aliasing);

            // Initialize the data flow problem
            InfoflowProblem forwardProblem = new InfoflowProblem(manager, zeroValue, ruleManagerFactory);

            // We need to create the right data flow solver
            IInfoflowSolver forwardSolver = createForwardSolver(executor, forwardProblem);

            // Set the options
            manager.setForwardSolver(forwardSolver);
            if (aliasingStrategy.getSolver() != null)
                aliasingStrategy.getSolver().getTabulationProblem().getManager().setForwardSolver(forwardSolver);
            solvers.add(forwardSolver);

            memoryWatcher.addSolver((IMemoryBoundedSolver) forwardSolver);

            forwardSolver.setMemoryManager(memoryManager);
            // forwardSolver.setEnableMergePointChecking(true);

            forwardProblem.setTaintPropagationHandler(taintPropagationHandler);
            forwardProblem.setTaintWrapper(taintWrapper);
            if (nativeCallHandler != null)
                forwardProblem.setNativeCallHandler(nativeCallHandler);

            if (aliasingStrategy.getSolver() != null) {
                aliasingStrategy.getSolver().getTabulationProblem().setActivationUnitsToCallSites(forwardProblem);
            }

            // Start a thread for enforcing the timeout
            FlowDroidTimeoutWatcher timeoutWatcher = null;
            FlowDroidTimeoutWatcher pathTimeoutWatcher = null;
            if (config.getDataFlowTimeout() > 0) {
                timeoutWatcher = new FlowDroidTimeoutWatcher(config.getDataFlowTimeout(), results);
                timeoutWatcher.addSolver((IMemoryBoundedSolver) forwardSolver);
                if (aliasingStrategy.getSolver() != null)
                    timeoutWatcher.addSolver((IMemoryBoundedSolver) aliasingStrategy.getSolver());
                timeoutWatcher.start();
            }

            InterruptableExecutor resultExecutor = null;
            long beforePathReconstruction = 0;
            try {
                // Print our configuration
                if (config.getFlowSensitiveAliasing() && !aliasingStrategy.isFlowSensitive())
                    logger.warn("Trying to use a flow-sensitive aliasing with an "
                            + "aliasing strategy that does not support this feature");
                if (config.getFlowSensitiveAliasing()
                        && config.getSolverConfiguration().getMaxJoinPointAbstractions() > 0)
                    logger.warn("Running with limited join point abstractions can break context-"
                            + "sensitive path builders");

                // We have to look through the complete program to find
                // sources which are then taken as seeds.
                int sinkCount = 0;
                logger.info("Looking for sources and sinks...");

                for (SootMethod sm : getMethodsForSeeds(iCfg))
                    sinkCount += scanMethodForSourcesSinks(sourcesSinks, forwardProblem, sm);

                // We optionally also allow additional seeds to be specified
                if (additionalSeeds != null)
                    for (String meth : additionalSeeds) {
                        SootMethod m = Scene.v().getMethod(meth);
                        if (!m.hasActiveBody()) {
                            logger.warn("Seed method {} has no active body", m);
                            continue;
                        }
                        forwardProblem.addInitialSeeds(m.getActiveBody().getUnits().getFirst(),
                                Collections.singleton(forwardProblem.zeroValue()));
                    }

                // Report on the sources and sinks we have found
                if (!forwardProblem.hasInitialSeeds()) {
                    logger.error("No sources found, aborting analysis");
                    continue;
                }
                if (sinkCount == 0) {
                    logger.error("No sinks found, aborting analysis");
                    continue;
                }
                logger.info("Source lookup done, found {} sources and {} sinks.",
                        forwardProblem.getInitialSeeds().size(), sinkCount);

                // Update the performance statistics
                performanceData.setSourceCount(forwardProblem.getInitialSeeds().size());
                performanceData.setSinkCount(sinkCount);

                // Initialize the taint wrapper if we have one
                if (taintWrapper != null)
                    taintWrapper.initialize(manager);
                if (nativeCallHandler != null)
                    nativeCallHandler.initialize(manager);

                // Register the handler for interim results
                TaintPropagationResults propagationResults = forwardProblem.getResults();
                resultExecutor = executorFactory.createExecutor(numThreads, false, config);
                resultExecutor.setThreadFactory(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thrPath = new Thread(r);
                        thrPath.setDaemon(true);
                        thrPath.setName("FlowDroid Path Reconstruction");
                        return thrPath;
                    }
                });

                // Create the path builder
                final IAbstractionPathBuilder builder = createPathBuilder(resultExecutor);
//							final IAbstractionPathBuilder builder = new DebuggingPathBuilder(pathBuilderFactory, manager);

                // If we want incremental result reporting, we have to
                // initialize it before we start the taint tracking
                if (config.getIncrementalResultReporting())
                    initializeIncrementalResultReporting(propagationResults, builder);

                // Initialize the performance data
                if (performanceData.getTaintPropagationSeconds() < 0)
                    performanceData.setTaintPropagationSeconds(0);
                long beforeTaintPropagation = System.nanoTime();

                onBeforeTaintPropagation(forwardSolver, backwardSolver);
                forwardSolver.solve();

                // Not really nice, but sometimes Heros returns before all
                // executor tasks are actually done. This way, we give it a
                // chance to terminate gracefully before moving on.
                int terminateTries = 0;
                while (terminateTries < 10) {
                    if (executor.getActiveCount() != 0 || !executor.isTerminated()) {
                        terminateTries++;
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            logger.error("Could not wait for executor termination", e);
                        }
                    } else
                        break;
                }
                if (executor.getActiveCount() != 0 || !executor.isTerminated())
                    logger.error("Executor did not terminate gracefully");
                if (executor.getException() != null) {
                    throw new RuntimeException("An exception has occurred in an executor", executor.getException());
                }

                // Update performance statistics
                performanceData.updateMaxMemoryConsumption(getUsedMemory());
                int taintPropagationSeconds = (int) Math.round((System.nanoTime() - beforeTaintPropagation) / 1E9);
                performanceData.addTaintPropagationSeconds(taintPropagationSeconds);
                performanceData.addEdgePropagationCount(forwardSolver.getPropagationCount());
                if (backwardSolver != null)
                    performanceData.addEdgePropagationCount(backwardSolver.getPropagationCount());

                // Print taint wrapper statistics
                if (taintWrapper != null) {
                    logger.info("Taint wrapper hits: " + taintWrapper.getWrapperHits());
                    logger.info("Taint wrapper misses: " + taintWrapper.getWrapperMisses());
                }

                // Give derived classes a chance to do whatever they need before we remove stuff
                // from memory
                onTaintPropagationCompleted(forwardSolver, backwardSolver);

                // Get the result abstractions
                Set<AbstractionAtSink> res = propagationResults.getResults();
                propagationResults = null;

                // We need to prune access paths that are entailed by
                // another one
                removeEntailedAbstractions(res);

                // Shut down the native call handler
                if (nativeCallHandler != null)
                    nativeCallHandler.shutdown();

                logger.info(
                        "IFDS problem with {} forward and {} backward edges solved in {} seconds, processing {} results...",
                        forwardSolver.getPropagationCount(),
                        aliasingStrategy.getSolver() == null ? 0 : aliasingStrategy.getSolver().getPropagationCount(),
                        taintPropagationSeconds, res == null ? 0 : res.size());

                // Update the statistics
                {
                    ISolverTerminationReason reason = ((IMemoryBoundedSolver) forwardSolver).getTerminationReason();
                    if (reason != null) {
                        if (reason instanceof OutOfMemoryReason)
                            results.setTerminationState(
                                    results.getTerminationState() | InfoflowResults.TERMINATION_DATA_FLOW_OOM);
                        else if (reason instanceof TimeoutReason)
                            results.setTerminationState(
                                    results.getTerminationState() | InfoflowResults.TERMINATION_DATA_FLOW_TIMEOUT);
                    }
                }

                // Force a cleanup. Everything we need is reachable through
                // the results set, the other abstractions can be killed
                // now.
                performanceData.updateMaxMemoryConsumption(getUsedMemory());
                logger.info(String.format("Current memory consumption: %d MB", getUsedMemory()));

                if (timeoutWatcher != null)
                    timeoutWatcher.stop();
                memoryWatcher.removeSolver((IMemoryBoundedSolver) forwardSolver);
                forwardSolver.cleanup();
                forwardSolver = null;
                forwardProblem = null;

                solverPeerGroup = null;

                // Remove the alias analysis from memory
                aliasing = null;
                if (aliasingStrategy.getSolver() != null) {
                    aliasingStrategy.getSolver().terminate();
                    memoryWatcher.removeSolver((IMemoryBoundedSolver) aliasingStrategy.getSolver());
                }
                aliasingStrategy.cleanup();
                aliasingStrategy = null;

                if (config.getIncrementalResultReporting())
                    res = null;
                iCfg.purge();

                // Clean up the manager. Make sure to free objects, even if
                // the manager is still held by other objects
                if (manager != null)
                    manager.cleanup();
                manager = null;

                // Report the remaining memory consumption
                Runtime.getRuntime().gc();
                performanceData.updateMaxMemoryConsumption(getUsedMemory());
                logger.info(String.format("Memory consumption after cleanup: %d MB", getUsedMemory()));

                // Apply the timeout to path reconstruction
                if (config.getPathConfiguration().getPathReconstructionTimeout() > 0) {
                    pathTimeoutWatcher = new FlowDroidTimeoutWatcher(
                            config.getPathConfiguration().getPathReconstructionTimeout(), results);
                    pathTimeoutWatcher.addSolver(builder);
                    pathTimeoutWatcher.start();
                }
                beforePathReconstruction = System.nanoTime();

                // Do the normal result computation in the end unless we
                // have used incremental path building
                if (config.getIncrementalResultReporting()) {
                    // After the last intermediate result has been computed,
                    // we need to re-process those abstractions that
                    // received new neighbors in the meantime
                    builder.runIncrementalPathCompuation();

                    try {
                        resultExecutor.awaitCompletion();
                    } catch (InterruptedException e) {
                        logger.error("Could not wait for executor termination", e);
                    }
                } else {
                    memoryWatcher.addSolver(builder);
                    builder.computeTaintPaths(res);
                    res = null;

                    // Wait for the path builders to terminate
                    try {
                        // The path reconstruction should stop on time anyway. In case it doesn't, we
                        // make sure that we don't get stuck.
                        long pathTimeout = config.getPathConfiguration().getPathReconstructionTimeout();
                        if (pathTimeout > 0)
                            resultExecutor.awaitCompletion(pathTimeout + 20, TimeUnit.SECONDS);
                        else
                            resultExecutor.awaitCompletion();
                    } catch (InterruptedException e) {
                        logger.error("Could not wait for executor termination", e);
                    }

                    // Update the statistics
                    {
                        ISolverTerminationReason reason = builder.getTerminationReason();
                        if (reason != null) {
                            if (reason instanceof OutOfMemoryReason)
                                results.setTerminationState(results.getTerminationState()
                                        | InfoflowResults.TERMINATION_PATH_RECONSTRUCTION_OOM);
                            else if (reason instanceof TimeoutReason)
                                results.setTerminationState(results.getTerminationState()
                                        | InfoflowResults.TERMINATION_PATH_RECONSTRUCTION_TIMEOUT);
                        }
                    }

                    // Get the results once the path builder is done
                    this.results.addAll(builder.getResults());
                }
                resultExecutor.shutdown();

                // If the path builder was aborted, we warn the user
                if (builder.isKilled())
                    logger.warn("Path reconstruction aborted. The reported results may be incomplete. "
                            + "You might want to try again with sequential path processing enabled.");
            } finally {
                // Terminate the executor
                if (resultExecutor != null)
                    resultExecutor.shutdown();

                // Make sure to stop the watcher thread
                if (timeoutWatcher != null)
                    timeoutWatcher.stop();
                if (pathTimeoutWatcher != null)
                    pathTimeoutWatcher.stop();

                if (aliasingStrategy != null) {
                    IInfoflowSolver solver = aliasingStrategy.getSolver();
                    if (solver != null)
                        solver.terminate();
                }

                // Do we have any more sources?
                hasMoreSources = oneSourceAtATime != null && oneSourceAtATime.hasNextSource();

                // Shut down the memory watcher
                memoryWatcher.close();

                // Get rid of all the stuff that's still floating around in
                // memory
                forwardProblem = null;
                forwardSolver = null;
                if (manager != null)
                    manager.cleanup();
                manager = null;
            }

            // Make sure that we are in a sensible state even if we ran out
            // of memory before
            Runtime.getRuntime().gc();
            performanceData.updateMaxMemoryConsumption((int) getUsedMemory());
            performanceData.setPathReconstructionSeconds(
                    (int) Math.round((System.nanoTime() - beforePathReconstruction) / 1E9));

            logger.info(String.format("Memory consumption after path building: %d MB", getUsedMemory()));
            logger.info(String.format("Path reconstruction took %d seconds",
                    performanceData.getPathReconstructionSeconds()));
        }

        // Execute the post-processors
        for (PostAnalysisHandler handler : this.postProcessors)
            results = handler.onResultsAvailable(results, iCfg);

        if (results == null || results.isEmpty())
            logger.warn("No results found.");
        else if (logger.isInfoEnabled()) {
            for (ResultSinkInfo sink : results.getResults().keySet()) {
                logger.info("The sink {} in method {} was called with values from the following sources:", sink,
                        iCfg.getMethodOf(sink.getStmt()).getSignature());
                for (ResultSourceInfo source : results.getResults().get(sink)) {
                    logger.info("- {} in method {}", source, iCfg.getMethodOf(source.getStmt()).getSignature());
                    if (source.getPath() != null) {
                        logger.info("\ton Path: ");
                        for (Unit p : source.getPath()) {
                            logger.info("\t -> " + iCfg.getMethodOf(p));
                            logger.info("\t\t -> " + p);
                        }
                    }
                }
            }
        }
    }

    private IMemoryManager<Abstraction, Unit> createMemoryManager() {
        if (memoryManagerFactory == null)
            return null;

        FlowDroidMemoryManager.PathDataErasureMode erasureMode;
        if (config.getPathConfiguration().mustKeepStatements())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseNothing;
        else if (pathBuilderFactory.supportsPathReconstruction())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseNothing;
        else if (pathBuilderFactory.isContextSensitive())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.KeepOnlyContextData;
        else
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseAll;
        IMemoryManager<Abstraction, Unit> memoryManager = memoryManagerFactory.getMemoryManager(false, erasureMode);
        return memoryManager;
    }

    private IAliasingStrategy createAliasAnalysis(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
                                                  InterruptableExecutor executor, IMemoryManager<Abstraction, Unit> memoryManager) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        IAliasingStrategy aliasingStrategy;
        IInfoflowSolver backSolver = null;
        BackwardsInfoflowProblem backProblem = null;
        InfoflowManager backwardsManager = null;
        switch (getConfig().getAliasingAlgorithm()) {
            case FlowSensitive:
                Constructor<InfoflowManager> constructor = (Constructor<InfoflowManager>) InfoflowManager.class.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                backwardsManager = constructor.newInstance(config, null, new BackwardsInfoflowCFG(iCfg), sourcesSinks,
                        taintWrapper, hierarchy, manager.getAccessPathFactory(), manager.getGlobalTaintManager());
                backProblem = new BackwardsInfoflowProblem(backwardsManager);

                // We need to create the right data flow solver
                InfoflowConfiguration.SolverConfiguration solverConfig = config.getSolverConfiguration();
                backSolver = createDataFlowSolver(executor, backProblem, solverConfig);

                backSolver.setMemoryManager(memoryManager);
                backSolver.setPredecessorShorteningMode(
                        pathConfigToShorteningMode(manager.getConfig().getPathConfiguration()));
                // backSolver.setEnableMergePointChecking(true);
                backSolver.setMaxJoinPointAbstractions(solverConfig.getMaxJoinPointAbstractions());
                backSolver.setMaxCalleesPerCallSite(solverConfig.getMaxCalleesPerCallSite());
                backSolver.setMaxAbstractionPathLength(solverConfig.getMaxAbstractionPathLength());
                backSolver.setSolverId(false);
                backProblem.setTaintPropagationHandler(backwardsPropagationHandler);
                backProblem.setTaintWrapper(taintWrapper);
                if (nativeCallHandler != null)
                    backProblem.setNativeCallHandler(nativeCallHandler);

                memoryWatcher.addSolver((IMemoryBoundedSolver) backSolver);

                aliasingStrategy = new FlowSensitiveAliasStrategy(manager, backSolver);
                break;
            case PtsBased:
                backProblem = null;
                backSolver = null;
                aliasingStrategy = new PtsBasedAliasStrategy(manager);
                break;
            case None:
                backProblem = null;
                backSolver = null;
                aliasingStrategy = new NullAliasStrategy();
                break;
            case Lazy:
                backProblem = null;
                backSolver = null;
                aliasingStrategy = new LazyAliasingStrategy(manager);
                break;
            default:
                throw new RuntimeException("Unsupported aliasing algorithm");
        }
        return aliasingStrategy;
    }

    private IInfoflowSolver createForwardSolver(InterruptableExecutor executor, InfoflowProblem forwardProblem) {
        // Depending on the configured solver algorithm, we have to create a
        // different solver object
        IInfoflowSolver forwardSolver;
        InfoflowConfiguration.SolverConfiguration solverConfig = config.getSolverConfiguration();
        forwardSolver = createDataFlowSolver(executor, forwardProblem, solverConfig);

        // Configure the solver
        forwardSolver.setSolverId(true);
        forwardSolver
                .setPredecessorShorteningMode(pathConfigToShorteningMode(manager.getConfig().getPathConfiguration()));
        forwardSolver.setMaxJoinPointAbstractions(solverConfig.getMaxJoinPointAbstractions());
        forwardSolver.setMaxCalleesPerCallSite(solverConfig.getMaxCalleesPerCallSite());
        forwardSolver.setMaxAbstractionPathLength(solverConfig.getMaxAbstractionPathLength());

        return forwardSolver;
    }

    private int scanMethodForSourcesSinks(final ISourceSinkManager sourcesSinks, InfoflowProblem forwardProblem,
                                          SootMethod m) {
        if (getConfig().getLogSourcesAndSinks() && collectedSources == null) {
            collectedSources = new HashSet<>();
            collectedSinks = new HashSet<>();
        }

        int sinkCount = 0;
        if (m.hasActiveBody()) {
            // Check whether this is a system class we need to ignore
            if (!isValidSeedMethod(m))
                return sinkCount;

            // Look for a source in the method. Also look for sinks. If we
            // have no sink in the program, we don't need to perform any
            // analysis
            PatchingChain<Unit> units = m.getActiveBody().getUnits();
            for (Unit u : units) {
                Stmt s = (Stmt) u;
                if (sourcesSinks.getSourceInfo(s, manager) != null) {
                    forwardProblem.addInitialSeeds(u, Collections.singleton(forwardProblem.zeroValue()));
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSources.add(s);
                    logger.debug("Source found: {} in {}", u, m.getSignature());
                }
                if (sourcesSinks.getSinkInfo(s, manager, null) != null) {
                    sinkCount++;
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSinks.add(s);
                    logger.debug("Sink found: {} in {}", u, m.getSignature());
                }
            }

        }
        return sinkCount;
    }

    private void initializeIncrementalResultReporting(TaintPropagationResults propagationResults,
                                                      final IAbstractionPathBuilder builder) {
        // Create the path builder
        memoryWatcher.addSolver(builder);
        this.results = new InfoflowResults();
        propagationResults.addResultAvailableHandler(new TaintPropagationResults.OnTaintPropagationResultAdded() {

            @Override
            public boolean onResultAvailable(AbstractionAtSink abs) {
                builder.addResultAvailableHandler(new IAbstractionPathBuilder.OnPathBuilderResultAvailable() {

                    @Override
                    public void onResultAvailable(ResultSourceInfo source, ResultSinkInfo sink) {
                        // Notify our external handlers
                        for (ResultsAvailableHandler handler : onResultsAvailable) {
                            if (handler instanceof ResultsAvailableHandler2) {
                                ResultsAvailableHandler2 handler2 = (ResultsAvailableHandler2) handler;
                                handler2.onSingleResultAvailable(source, sink);
                            }
                        }
                        results.addResult(sink, source);
                    }

                });

                // Compute the result paths
                builder.computeTaintPaths(Collections.singleton(abs));
                return true;
            }

        });
    }

    private void removeEntailedAbstractions(Set<AbstractionAtSink> res) {
        for (Iterator<AbstractionAtSink> absAtSinkIt = res.iterator(); absAtSinkIt.hasNext(); ) {
            AbstractionAtSink curAbs = absAtSinkIt.next();
            for (AbstractionAtSink checkAbs : res) {
                if (checkAbs != curAbs && checkAbs.getSinkStmt() == curAbs.getSinkStmt()
                        && checkAbs.getAbstraction().isImplicit() == curAbs.getAbstraction().isImplicit()
                        && checkAbs.getAbstraction().getSourceContext() == curAbs.getAbstraction().getSourceContext()) {
                    if (checkAbs.getAbstraction().getAccessPath().entails(curAbs.getAbstraction().getAccessPath())) {
                        absAtSinkIt.remove();
                        break;
                    }
                }
            }
        }
    }

    private PredecessorShorteningMode pathConfigToShorteningMode(InfoflowConfiguration.PathConfiguration pathConfiguration) {
        if (pathBuilderFactory.supportsPathReconstruction()) {
            switch (pathConfiguration.getPathReconstructionMode()) {
                case Fast:
                    return PredecessorShorteningMode.ShortenIfEqual;
                case NoPaths:
                    return PredecessorShorteningMode.AlwaysShorten;
                case Precise:
                    return PredecessorShorteningMode.NeverShorten;
                default:
                    throw new RuntimeException("Unknown path reconstruction mode");
            }
        } else
            return PredecessorShorteningMode.AlwaysShorten;
    }
}
