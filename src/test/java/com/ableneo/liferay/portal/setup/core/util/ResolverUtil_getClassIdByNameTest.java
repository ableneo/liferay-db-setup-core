package com.ableneo.liferay.portal.setup.core.util;

import static com.ableneo.liferay.portal.setup.core.util.ResolverUtil.CLASS_ID_BY_NAME;
import static com.ableneo.liferay.portal.setup.core.util.ResolverUtil.CLOSING_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolverUtil_getClassIdByNameTest {

    @Mock
    ClassNameLocalServiceUtil classNameLocalServiceUtil;

    @Test
    void happyDayScenario1() {
        Mockito.mockStatic(ClassNameLocalServiceUtil.class);
        Mockito.when(ClassNameLocalServiceUtil.getClassNameId("com.liferay.portal.kernel.model.Layout")).thenReturn(7l);
        assertEquals(
            "test 7 test",
            ResolverUtil.getClassIdByName(
                String.format("test %scom.liferay.portal.kernel.model.Layout%s test", CLASS_ID_BY_NAME, CLOSING_TAG),
                "test1"
            )
        );
    }
}
