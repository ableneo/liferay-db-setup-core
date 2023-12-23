package com.ableneo.liferay.portal.setup;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import com.liferay.portal.kernel.exception.PortalException;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;

@ExtendWith(MockitoExtension.class)
class BasicLiferaySetupTest extends ValidSetupTestMocks {

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
