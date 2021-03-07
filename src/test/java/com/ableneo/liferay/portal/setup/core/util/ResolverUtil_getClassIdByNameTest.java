package com.ableneo.liferay.portal.setup.core.util;

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
