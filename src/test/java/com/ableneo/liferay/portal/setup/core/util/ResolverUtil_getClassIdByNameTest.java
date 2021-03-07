package com.ableneo.liferay.portal.setup.core.util;

/*-
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2019 ableneo s. r. o.
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

import static com.ableneo.liferay.portal.setup.core.util.ResolverUtil.CLASS_ID_BY_NAME;
import static com.ableneo.liferay.portal.setup.core.util.ResolverUtil.CLOSING_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.jupiter.api.Test;

class ResolverUtil_getClassIdByNameTest {
    @Mocked
    ClassNameLocalServiceUtil classNameLocalServiceUtil;

    @Test
    void happyDayScenario1() {
        new Expectations(ClassNameLocalServiceUtil.class) {

            {
                classNameLocalServiceUtil.getClassNameId("com.liferay.portal.kernel.model.Layout");
                result = 7l;
            }
        };
        assertEquals(
            "test 7 test",
            ResolverUtil.getClassIdByName(
                String.format("test %scom.liferay.portal.kernel.model.Layout%s test", CLASS_ID_BY_NAME, CLOSING_TAG),
                "test1"
            )
        );
    }
}
