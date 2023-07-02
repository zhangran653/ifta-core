package ta;

import java.util.List;

@FunctionalInterface
public interface EntrySelector {

    List<String> select(String classFilePath);

}
