package com.ableneo.liferay.portal.setup.core.util;

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

import static com.ableneo.liferay.portal.setup.core.util.ResolverUtil.CLASS_ID_BY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ResolverUtilTest {

    @Test
    void missingAnyTag() {
        final String valueWithoutTags = "test test";
        assertEquals(valueWithoutTags, ResolverUtil.getClassIdByName(valueWithoutTags, "doing test"));
    }

    @Test
    void missingClosingTagInLookupArticleWithArticleId() {
        String valueWithWrongClosingTag = String.format("test %s123$} test", ResolverUtil.ARTICLE_BY_ART_ID);
        assertEquals(
            valueWithWrongClosingTag,
            ResolverUtil.lookupArticleWithArticleId(
                valueWithWrongClosingTag,
                String.format("testing: %s", valueWithWrongClosingTag),
                1l,
                1l,
                1
            )
        );
    }

    @Test
    void missingClosingTagInGetClassIdByName() {
        String valueWithWrongClosingTag = String.format(
            "test %scom.liferay.portal.kernel.model.Layout$} test",
            CLASS_ID_BY_NAME
        );
        assertEquals(
            valueWithWrongClosingTag,
            ResolverUtil.getClassIdByName(
                valueWithWrongClosingTag,
                String.format("testing: %s", valueWithWrongClosingTag)
            )
        );
    }
}
