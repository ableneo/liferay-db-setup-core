package com.ableneo.liferay.portal.setup;

import com.ableneo.liferay.portal.setup.core.SetupPermissions;
import com.ableneo.liferay.portal.setup.core.SetupServiceAccessPolicies;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public abstract class ValidSetupTestMocks {

    public static final Logger LOGGER = LoggerFactory.getLogger(BasicLiferaySetupTest.class);
    public static MockedStatic<SetupPermissions> setupPermissionsMockedStatic;
    public static MockedStatic<GroupLocalServiceUtil> groupLocalServiceUtilMockedStatic;
    public static MockedStatic<RoleLocalServiceUtil> roleLocalServiceUtilMockedStatic;
    public static MockedStatic<UserLocalServiceUtil> userLocalServiceUtilMockedStatic;
    public static MockedStatic<LocaleUtil> localeUtilMockedStatic;
    public static MockedStatic<SetupServiceAccessPolicies> setupServiceAccessPoliciesMockedStatic;

    @Mock
    public User liferayUser;

    @Mock
    public Group liferayGroup;

    @Mock
    public PermissionChecker permissionChecker;

    @Mock
    public PermissionCheckerFactory permissionCheckerFactory;

    public File validConfiguration;
    public File invalidConfiguration;
    public File validConfigurationTwoCompanies;
    public static MockedStatic<SetupConfigurationThreadLocal> setupConfigurationThreadLocalMockedStatic;

    @BeforeAll
    static void beforeAll() {
        setupPermissionsMockedStatic = Mockito.mockStatic(SetupPermissions.class);
        groupLocalServiceUtilMockedStatic = Mockito.mockStatic(GroupLocalServiceUtil.class);
        roleLocalServiceUtilMockedStatic = Mockito.mockStatic(RoleLocalServiceUtil.class);
        setupConfigurationThreadLocalMockedStatic = Mockito.mockStatic(SetupConfigurationThreadLocal.class);
        userLocalServiceUtilMockedStatic = Mockito.mockStatic(UserLocalServiceUtil.class);
        localeUtilMockedStatic = Mockito.mockStatic(LocaleUtil.class);
        setupServiceAccessPoliciesMockedStatic = Mockito.mockStatic(SetupServiceAccessPolicies.class);
        setupConfigurationThreadLocalMockedStatic.when(() -> SetupServiceAccessPolicies.setupServiceAccessPolicies(any())).thenCallRealMethod();
    }

    @AfterAll
    public static void afterAll() {
        setupPermissionsMockedStatic.close();
        groupLocalServiceUtilMockedStatic.close();
        roleLocalServiceUtilMockedStatic.close();
        setupConfigurationThreadLocalMockedStatic.close();
        userLocalServiceUtilMockedStatic.close();
        localeUtilMockedStatic.close();
        setupServiceAccessPoliciesMockedStatic.close();
    }

    @BeforeEach
    void setup() {
        try {
            validConfiguration = new File(MarshallUtilTest.class.getResource("/valid-configuration.xml").toURI());
            invalidConfiguration = new File(MarshallUtilTest.class.getResource("/invalid-configuration.xml").toURI());
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

}
