package utils;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;

public class FileUtility {
    public static void extractJar(String zipFilePath, String destDirectory) {
        java.util.jar.JarFile jarfile = null;
        try {
            jarfile = new java.util.jar.JarFile(new java.io.File(zipFilePath));

            java.util.Enumeration<java.util.jar.JarEntry> enu = jarfile.entries();
            while (enu.hasMoreElements()) {
                java.util.jar.JarEntry je = enu.nextElement();
                java.io.File fl = new java.io.File(destDirectory, je.getName());
                if (!fl.exists()) {
                    fl.getParentFile().mkdirs();
                    fl = new java.io.File(destDirectory, je.getName());
                }
                if (je.isDirectory()) {
                    continue;
                }
                java.io.InputStream is = null;
                is = jarfile.getInputStream(je);
                java.io.FileOutputStream fo = null;
                fo = new java.io.FileOutputStream(fl);

                while (true) {
                    try {
                        if (!(is.available() > 0)) break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    fo.write(is.read());
                }
                fo.close();
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static void flatExtractJar(String zipFilePath, String destDirectory) {
        String baseName = FilenameUtils.removeExtension(Path.of(zipFilePath).getFileName().toString());
        java.util.jar.JarFile jarfile = null;
        try {
            jarfile = new java.util.jar.JarFile(new java.io.File(zipFilePath));

            java.util.Enumeration<java.util.jar.JarEntry> enu = jarfile.entries();
            while (enu.hasMoreElements()) {
                java.util.jar.JarEntry je = enu.nextElement();
                String name = null;
                if (je.getName().startsWith(baseName)) {
                    name = je.getName().replaceFirst(baseName, "");
                } else {
                    name = je.getName();
                }
                java.io.File fl = new java.io.File(destDirectory, name);
                if (!fl.exists()) {
                    fl.getParentFile().mkdirs();
                    fl = new java.io.File(destDirectory, name);
                }
                if (je.isDirectory()) {
                    continue;
                }
                java.io.InputStream is = null;
                is = jarfile.getInputStream(je);
                java.io.FileOutputStream fo = null;
                fo = new java.io.FileOutputStream(fl);

                while (true) {
                    try {
                        if (!(is.available() > 0)) break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    fo.write(is.read());
                }
                fo.close();
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
