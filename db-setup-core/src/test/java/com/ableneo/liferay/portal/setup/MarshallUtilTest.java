package com.ableneo.liferay.portal.setup;

/*-
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2019 Pawel Kruszewski
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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarshallUtilTest {

    private File validConfiguration;
    private File invalidConfiguration;

    @BeforeEach
    void setup() {
        try {
            validConfiguration =
                    new File(MarshallUtilTest.class.getResource(("/valid-configuration.xml")).toURI());

            invalidConfiguration =
                new File(MarshallUtilTest.class.getResource(("/invalid-configuration.xml")).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    void unmarshallInvalid() throws FileNotFoundException {
        assertNull(MarshallUtil.unmarshall(invalidConfiguration));
    }

    @Test
    void unmarshallValid() throws FileNotFoundException {
        assertNotNull(MarshallUtil.unmarshall(validConfiguration));
    }


    @Test
    void validateAgainstXSD() {
        assertTrue(MarshallUtil.validateAgainstXSD(validConfiguration));
        assertFalse(MarshallUtil.validateAgainstXSD(invalidConfiguration));
    }
}
