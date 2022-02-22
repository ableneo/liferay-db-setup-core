package com.ableneo.liferay.portal.setup;

import com.ableneo.liferay.portal.setup.core.SetupPermissions;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiferaySetupTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiferaySetupTest.class);
    private static MockedStatic<SetupPermissions> setupPermissionsMockedStatic;
    private static MockedStatic<GroupLocalServiceUtil> groupLocalServiceUtilMockedStatic;
    private static MockedStatic<RoleLocalServiceUtil> roleLocalServiceUtilMockedStatic;
    private static MockedStatic<UserLocalServiceUtil> userLocalServiceUtilMockedStatic;

    @Mock
    User liferayUser;

    @Mock
    Group liferayGroup;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private PermissionCheckerFactory permissionCheckerFactory;

    private File validConfiguration;
    private File validConfigurationTwoCompanies;
    private static MockedStatic<SetupConfigurationThreadLocal> setupConfigurationThreadLocalMockedStatic;

    @BeforeAll
    static void beforeAll() {
        setupPermissionsMockedStatic = Mockito.mockStatic(SetupPermissions.class);
        groupLocalServiceUtilMockedStatic = Mockito.mockStatic(GroupLocalServiceUtil.class);
        roleLocalServiceUtilMockedStatic = Mockito.mockStatic(RoleLocalServiceUtil.class);
        setupConfigurationThreadLocalMockedStatic = Mockito.mockStatic(SetupConfigurationThreadLocal.class);
        userLocalServiceUtilMockedStatic = Mockito.mockStatic(UserLocalServiceUtil.class);
    }

    @AfterAll
    public static void afterAll() {
        setupPermissionsMockedStatic.close();
        groupLocalServiceUtilMockedStatic.close();
        roleLocalServiceUtilMockedStatic.close();
        setupConfigurationThreadLocalMockedStatic.close();
        userLocalServiceUtilMockedStatic.close();
    }

    @BeforeEach
    void setup() {
        try {
            validConfiguration = new File(MarshallUtilTest.class.getResource("/valid-configuration.xml").toURI());
            validConfigurationTwoCompanies = new File(MarshallUtilTest.class.getResource("/valid-configuration-two-companies.xml").toURI());
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to parse configuration file", e);
        }

        try {
            when(UserLocalServiceUtil.getUserByEmailAddress(anyLong(), anyString())).thenReturn(liferayUser);
            when(GroupLocalServiceUtil.getGroup(anyLong(), anyString())).thenReturn(liferayGroup);
        } catch (PortalException e) {
            LOGGER.error("",e);
        }
        when(liferayGroup.getGroupId()).thenReturn(20l);
    }

    @Test
    void testIfCompanyIsProcessed() throws FileNotFoundException, PortalException {
        assertTrue(LiferaySetup.setup(validConfiguration));
        setupConfigurationThreadLocalMockedStatic.verify(() -> SetupConfigurationThreadLocal.configureThreadLocalContent("test@liferay.com", 1l, (Bundle) null), times(1));
        setupConfigurationThreadLocalMockedStatic.verify(() -> SetupConfigurationThreadLocal.setRunInGroupId(20l), times(1));
        setupConfigurationThreadLocalMockedStatic.clearInvocations();
    }

    @Test
    void testIfTwoCompaniesAreProcessed() throws FileNotFoundException, PortalException {
        assertTrue(LiferaySetup.setup(validConfigurationTwoCompanies));
        setupConfigurationThreadLocalMockedStatic.verify(() -> SetupConfigurationThreadLocal.configureThreadLocalContent("test@liferay.com", 1l, (Bundle) null), times(1));
        setupConfigurationThreadLocalMockedStatic.verify(() -> SetupConfigurationThreadLocal.configureThreadLocalContent("test@liferay.com", 2l, (Bundle) null), times(1));
        setupConfigurationThreadLocalMockedStatic.verify(() -> SetupConfigurationThreadLocal.setRunInGroupId(20l), times(2));
        setupConfigurationThreadLocalMockedStatic.clearInvocations();
    }
}

