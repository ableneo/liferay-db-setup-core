package com.ableneo.liferay.portal.setup.core.util;

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
