package com.ableneo.liferay.portal.setup.core.util;

/*-
 * #%L
 * com.ableneo.liferay.db.setup.core
 * %%
 * Copyright (C) 2016 - 2021 ableneo s. r. o.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.liferay.portal.kernel.util.FileUtil;

/**
 * Created by ntrp on 5/15/17.
 */
public class ResourcesUtil {

    private ResourcesUtil() {}

    public static InputStream getFileStream(String path) {

        ClassLoader cl = ResourcesUtil.class.getClassLoader();
        InputStream is = cl.getResourceAsStream(path);
        if (is == null) {
        	throw new RuntimeException("Can not load file, does it exist? path:["+path+"]");
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
