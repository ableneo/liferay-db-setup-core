package com.ableneo.liferay.portal.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetupConfigurationThreadLocalTest {
    private static MockedStatic<PortalUtil> portalUtilMockedStatic;
    private static MockedStatic<GroupLocalServiceUtil> groupLocalServiceUtilMockedStatic;
    @Mock
    Group group;

    @BeforeAll
    public static void initStaticMocks() {
        portalUtilMockedStatic = Mockito.mockStatic(PortalUtil.class);
        groupLocalServiceUtilMockedStatic = Mockito.mockStatic(GroupLocalServiceUtil.class);
    }

    @AfterAll
    public static void afterAll() {
        portalUtilMockedStatic.close();
        groupLocalServiceUtilMockedStatic.close();
    }

    @Test
    void getRunInCompanyId() throws PortalException {
        Long groupId = 888l;
        final long companyId = 123l;
        Mockito.when(PortalUtil.getDefaultCompanyId()).thenReturn(companyId);
        Mockito.when(GroupLocalServiceUtil.getGroup(anyLong(), anyString())).thenReturn(group);
        when(group.getGroupId()).thenReturn(groupId);
        assertEquals(companyId, PortalUtil.getDefaultCompanyId());
        assertEquals(groupId, SetupConfigurationThreadLocal.getRunInGroupId());
    }

    @Test
    void setRunInCompanyId() {
        SetupConfigurationThreadLocal.setRunAsUserId(1l);
        assertEquals(Long.valueOf(1l), SetupConfigurationThreadLocal.getRunAsUserId());
    }
}
