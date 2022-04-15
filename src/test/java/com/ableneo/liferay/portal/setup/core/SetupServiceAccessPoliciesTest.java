package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.ValidSetupTestMocks;
import com.ableneo.liferay.portal.setup.LiferaySetup;
import com.liferay.portal.kernel.exception.PortalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class SetupServiceAccessPoliciesTest extends ValidSetupTestMocks {

    @Test
    void testServiceAccessPolicyIsProcessed() throws FileNotFoundException, PortalException {
        assertTrue(LiferaySetup.setup(validConfiguration));
        setupServiceAccessPoliciesMockedStatic.verify(() -> SetupServiceAccessPolicies.addSAPEntry(any(), any()), times(1));
        setupServiceAccessPoliciesMockedStatic.clearInvocations();
        setupConfigurationThreadLocalMockedStatic.clearInvocations();
    }
}
