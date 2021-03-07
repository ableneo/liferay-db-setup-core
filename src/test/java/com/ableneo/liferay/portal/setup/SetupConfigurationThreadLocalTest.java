package com.ableneo.liferay.portal.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.jupiter.api.Test;

class SetupConfigurationThreadLocalTest {
    // Liferay code is full of static methods. JMockit supports static method mocks.
    // Mockit will not support static method mocking (read the discussions), PowerMock doesn't support junit5 yet.
    @Mocked
    PortalUtil portalUtil;

    @Mocked
    GroupLocalServiceUtil groupLocalServiceUtil;

    @Mocked
    Group group;

    @Test
    void getRunInCompanyId() throws PortalException {
        Long groupId = 888l;
        final long companyId = 123l;
        new Expectations(PortalUtil.class) {

            {
                portalUtil.getDefaultCompanyId();
                result = companyId;
                groupLocalServiceUtil.getGroup(withAny(1l), withAny("")); // any of provided type
                result = group;
                group.getGroupId();
                result = groupId;
            }
        };
        assertEquals(companyId, PortalUtil.getDefaultCompanyId());
        assertEquals(groupId, SetupConfigurationThreadLocal.getRunInGroupId());
    }

    @Test
    void setRunInCompanyId() {
        SetupConfigurationThreadLocal.setRunAsUserId(1l);
        assertEquals(new Long(1l), SetupConfigurationThreadLocal.getRunAsUserId());
    }
}
