package com.ableneo.liferay.portal.setup.core.util;

import java.io.File;

public final class FilePathUtil {

    private FilePathUtil() {}

    public static String getExtension(final String fname) {
        String ext = "";
        File f = new File(fname);
        int pos = f.getName().indexOf('.');
        if (f.getName().indexOf('.') > -1) {
            ext = f.getName().substring(pos);
        }
        return ext;
    }

    public static String getPath(final String fname) {
        String path = "";
        int pos = fname.lastIndexOf('/');
        if (pos > -1) {
            path = fname.substring(0, pos);
        }

        return path;
    }

    public static String getFileName(final String fname) {
        File f = new File(fname);
        return f.getName();
    }
}
