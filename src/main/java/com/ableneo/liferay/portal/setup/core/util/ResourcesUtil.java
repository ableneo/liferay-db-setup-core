package com.ableneo.liferay.portal.setup.core.util;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.liferay.portal.kernel.util.FileUtil;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by ntrp on 5/15/17.
 */
public class ResourcesUtil {

    private ResourcesUtil() {}

    public static InputStream getFileStream(String path) {
        ClassLoader cl = ResourcesUtil.class.getClassLoader();
        InputStream fileStream = cl.getResourceAsStream(path);
        if (fileStream == null) {
            // fail safe if caller bundle provided
            Bundle callerBundle = SetupConfigurationThreadLocal.getCallerBundle();
            if (callerBundle == null) {
                throw new RuntimeException("Can not load file, does it exist? path:[" + path + "]");
            } else {
                URL url = callerBundle.getEntry(path);
                if (url != null) {
                    try {
                        fileStream = url.openStream();
                    } catch (IOException e) {
                        throw new RuntimeException("Error loading file from bundle:" + callerBundle.getSymbolicName() + ", path:[" + path + "]", e);
                    }
                } else {
                    throw new RuntimeException("Can not load file from bundle:" + callerBundle.getSymbolicName() + ", does it exist? path:[" + path + "]");
                }
            }
        }
        return fileStream;
    }

    public static byte[] getFileBytes(String path) throws IOException {
        return FileUtil.getBytes(getFileStream(path));
    }

    public static String getFileContent(String path) throws IOException {
        byte[] bytes = getFileBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
