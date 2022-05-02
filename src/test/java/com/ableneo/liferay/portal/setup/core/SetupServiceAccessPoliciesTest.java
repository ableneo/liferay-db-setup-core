package com.ableneo.liferay.portal.setup.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import com.ableneo.liferay.portal.setup.LiferaySetup;
import com.ableneo.liferay.portal.setup.ValidSetupTestMocks;
import com.liferay.portal.kernel.exception.PortalException;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetupServiceAccessPoliciesTest extends ValidSetupTestMocks {

    @Test
    void testServiceAccessPolicyIsProcessed() throws FileNotFoundException, PortalException {
        assertTrue(LiferaySetup.setup(validConfiguration));
        setupServiceAccessPoliciesMockedStatic.verify(
            () -> SetupServiceAccessPolicies.addSAPEntry(any(), any()),
            times(1)
        );
        setupConfigurationThreadLocalMockedStatic.clearInvocations();
        setupServiceAccessPoliciesMockedStatic.clearInvocations();
    }

    @Test
    void cleanUpAllowedServiceSignatures_nullConfiguration() {
        assertNull(SetupServiceAccessPolicies.cleanUpAllowedServiceSignatures(null));
        setupServiceAccessPoliciesMockedStatic.clearInvocations();
    }

    @Test
    void cleanUpAllowedServiceSignatures_emptyConfiguration() {
        assertEquals("", SetupServiceAccessPolicies.cleanUpAllowedServiceSignatures(""));
        assertEquals("", SetupServiceAccessPolicies.cleanUpAllowedServiceSignatures("   \n \n"));
        setupServiceAccessPoliciesMockedStatic.clearInvocations();
    }

    @Test
    void cleanUpAllowedServiceSignatures_multiLineConfiguration() {
        assertEquals(
            "testClass\ntestClass2",
            SetupServiceAccessPolicies.cleanUpAllowedServiceSignatures("   testClass\n      testClass2")
        );
        assertEquals(
            "testClass\ntestClass2",
            SetupServiceAccessPolicies.cleanUpAllowedServiceSignatures("testClass\n      testClass2\n")
        );
        assertEquals(
            "testClass\ntestClass2",
            SetupServiceAccessPolicies.cleanUpAllowedServiceSignatures("testClass\n      testClass2\n\n")
        );
        setupServiceAccessPoliciesMockedStatic.clearInvocations();
    }
}
