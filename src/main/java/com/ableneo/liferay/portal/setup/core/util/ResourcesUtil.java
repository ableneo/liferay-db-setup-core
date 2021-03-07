package com.ableneo.liferay.portal.setup.core.util;

import com.liferay.portal.kernel.util.FileUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by ntrp on 5/15/17.
 */
public class ResourcesUtil {

    private ResourcesUtil() {}

    public static InputStream getFileStream(String path) {
        ClassLoader cl = ResourcesUtil.class.getClassLoader();
        InputStream is = cl.getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("Can not load file, does it exist? path:[" + path + "]");
        }
        return cl.getResourceAsStream(path);
    }

    public static byte[] getFileBytes(String path) throws IOException {
        return FileUtil.getBytes(getFileStream(path));
    }

    public static String getFileContent(String path) throws IOException {
        byte[] bytes = getFileBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
