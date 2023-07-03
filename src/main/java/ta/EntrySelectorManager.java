package ta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;
import utils.PathOptimization;

import java.util.*;
import java.util.stream.Collectors;

public class EntrySelectorManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, String> classNameCache = new HashMap<>();

    private final String entryFormatter = "<%s: void _jspService(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>";

    // TODO more annotation to be collected.
    private final HashSet<String> tags = new HashSet<>(
            List.of("Lorg/springframework/amqp/rabbit/annotation/RabbitHandler;",
                    "Lorg/springframework/web/bind/annotation/GetMapping;",
                    "Lorg/springframework/web/bind/annotation/PostMapping;",
                    "Lorg/springframework/web/bind/annotation/ExceptionHandler;"));
    EntrySelector jspServiceEntry = (classFilePath) -> {
        List<String> entries = new ArrayList<>();
        String fullClassName = className(classFilePath);
        try {
            SootClass sc = Scene.v().getSootClass(fullClassName);
            if (sc.declaresMethodByName("doGet")) {
                String sg = sc.getMethodByName("doGet").getSignature();
                logger.info("add {} to entry points", sg);
                entries.add(sg);
            }
            if (sc.declaresMethodByName("doPost")) {
                String sg = sc.getMethodByName("doPost").getSignature();
                logger.info("add {} to entry points", sg);
                entries.add(sg);
            }
            if (classFilePath.endsWith("_jsp.class")) {
                String entry = String.format(entryFormatter, fullClassName);
                logger.info("add {} to entry points", entry);
                entries.add(entry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return entries;

    };

    // TODO cache classFilePath and fullClassName
    EntrySelector annotationTagEntry = (classFilePath) -> {
        List<String> entries = new ArrayList<>();
        String fullClassName = className(classFilePath);
        try {
            SootClass sc = Scene.v().getSootClass(fullClassName);
            sc.getMethods().forEach(m -> {
                VisibilityAnnotationTag tag = (VisibilityAnnotationTag) m.getTag("VisibilityAnnotationTag");
                if (tag != null) {
                    for (AnnotationTag annotation : tag.getAnnotations()) {
                        if (tags.contains(annotation.getType())) {
                            logger.info("add {} to entry points", m.getSignature());
                            entries.add(m.getSignature());
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    };

    EntrySelector publicStaticOrMainEntry = (classFilePath) -> {
        List<String> entries = new ArrayList<>();
        String fullClassName = className(classFilePath);
        try {
            SootClass sc = Scene.v().getSootClass(fullClassName);
            sc.getMethods().forEach(m -> {
                if ((m.isPublic() && m.isStatic() && !m.isJavaLibraryMethod()) || m.isEntryMethod() || m.isMain()) {
                    logger.info("add {} to entry points", m.getSignature());
                    entries.add(m.getSignature());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    };

    public List<EntrySelector> selectors = List.of(jspServiceEntry, annotationTagEntry, publicStaticOrMainEntry);


    private final Map<String, EntrySelector> namedEntrySelector = new HashMap<>() {{
        put("JSPSERVICEENTRY", jspServiceEntry);
        put("ANNOTATIONTAGENTRY", annotationTagEntry);
        put("PUBLICSTATICORMAINENTRY", publicStaticOrMainEntry);
    }};


    public EntrySelectorManager(String processDir) {
        setSoot(processDir);
    }

    public EntrySelectorManager() {
    }

    public void setSoot(String processDir) {
        G.reset();
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Collections.singletonList(processDir));
        Options.v().set_whole_program(true);
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
    }

    public void setProcessorDir(String processorDir) {
        setSoot(processorDir);
    }

    public static EntrySelectorManager buildEntryManager(String processDir) {
        return new EntrySelectorManager(processDir);
    }

    public static EntrySelectorManager buildEntryManager() {
        return new EntrySelectorManager();
    }

    public List<EntrySelector> selectorList() {
        return selectors;
    }

    public List<EntrySelector> selectorList(String entryNames) {
        if (entryNames == null || entryNames.isBlank() || entryNames.equalsIgnoreCase("ALL")) {
            return selectors;
        }
        return Arrays.stream(entryNames.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(namedEntrySelector::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private String className(String classFilePath) {
        String className = classNameCache.get(classFilePath);
        if (className == null) {
            String fullClassName = PathOptimization.className(classFilePath);
            classNameCache.put(classFilePath, fullClassName);
            return fullClassName;
        }
        return className;
    }

}
