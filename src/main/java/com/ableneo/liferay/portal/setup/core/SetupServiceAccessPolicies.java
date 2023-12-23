package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.domain.DeleteServiceAccessPolicy;
import com.ableneo.liferay.portal.setup.domain.ServiceAccessPolicies;
import com.ableneo.liferay.portal.setup.domain.ServiceAccessPolicy;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.security.service.access.policy.exception.NoSuchEntryException;
import com.liferay.portal.security.service.access.policy.model.SAPEntry;
import com.liferay.portal.security.service.access.policy.service.SAPEntryLocalServiceUtil;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupServiceAccessPolicies {

    protected static final Pattern ALL_WHITE_SPACE_CHARACTERS = Pattern.compile("\\W");
    private static final Logger LOG = LoggerFactory.getLogger(SetupCategorization.class);

    public static void setupServiceAccessPolicies(ServiceAccessPolicies serviceAccessPolicies) {
        final List<ServiceAccessPolicy> serviceAccessPolicyList = serviceAccessPolicies.getServiceAccessPolicy();

        final Long runInCompanyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        serviceAccessPolicyList.forEach(serviceAccessPolicyConfiguration -> {
            Map<Locale, String> titleMap = new HashMap<>();
            serviceAccessPolicyConfiguration
                .getTitle()
                .forEach(title -> {
                    titleMap.put(LocaleUtil.fromLanguageId(title.getLocale()), title.getText());
                });

            SAPEntry sapEntry = getSapEntry(runInCompanyId, serviceAccessPolicyConfiguration.getName());

            if (sapEntry == null) {
                addSAPEntry(serviceAccessPolicyConfiguration, titleMap);
            } else {
                sapEntry.setEnabled(serviceAccessPolicyConfiguration.isEnabled());
                sapEntry.setDefaultSAPEntry(serviceAccessPolicyConfiguration.isUnauthenticated());

                final String allowedServiceSignatures = cleanUpAllowedServiceSignatures(
                    serviceAccessPolicyConfiguration.getAllowedServiceSignatures()
                );
                sapEntry.setAllowedServiceSignatures(allowedServiceSignatures);
                if (!titleMap.isEmpty()) {
                    sapEntry.setTitleMap(titleMap);
                }
                updateSAPEntry(sapEntry);
            }
        });

        final List<DeleteServiceAccessPolicy> deleteServiceAccessPolicyList = serviceAccessPolicies.getDeleteServiceAccessPolicy();
        deleteServiceAccessPolicyList.forEach(deleteServiceAccessPolicy -> {
            final SAPEntry sapEntry = getSapEntry(runInCompanyId, deleteServiceAccessPolicy.getName());
            if (sapEntry != null) {
                deleteSAPEntry(sapEntry);
            } else {
                LOG.warn(
                    "Tried to delete SAP Entry {} in company {} but the entry haven't been found.",
                    deleteServiceAccessPolicy.getName(),
                    runInCompanyId
                );
            }
        });
    }

    public static String cleanUpAllowedServiceSignatures(String allowedServiceSignatures) {
        if (
            allowedServiceSignatures == null ||
            ALL_WHITE_SPACE_CHARACTERS.matcher(allowedServiceSignatures).replaceAll("").isEmpty()
        ) {
            return "";
        }
        final String normalizedMultilineConfiguration = new BufferedReader(new StringReader(allowedServiceSignatures))
            .lines()
            .filter(line -> !line.trim().isEmpty())
            .reduce("", (result, element) -> result.concat(element.trim() + "\n"));
        return normalizedMultilineConfiguration.substring(0, normalizedMultilineConfiguration.length() - 1);
    }

    private static void deleteSAPEntry(SAPEntry sapEntry) {
        try {
            SAPEntryLocalServiceUtil.deleteSAPEntry(sapEntry);
        } catch (PortalException e) {
            LOG.error(
                "Unexpected exception when trying to delete SAP Entry {} in company {}",
                sapEntry.getName(),
                sapEntry.getCompanyId(),
                e
            );
        }
    }

    protected static void updateSAPEntry(SAPEntry sapEntry) {
        SAPEntryLocalServiceUtil.updateSAPEntry(sapEntry);
    }

    protected static void addSAPEntry(
        ServiceAccessPolicy serviceAccessPolicyConfiguration,
        Map<Locale, String> titleMap
    ) {
        final ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(SetupConfigurationThreadLocal.getRunInCompanyId());
        final Long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();
        try {
            SAPEntryLocalServiceUtil.addSAPEntry(
                runAsUserId,
                serviceAccessPolicyConfiguration.getAllowedServiceSignatures(),
                serviceAccessPolicyConfiguration.isUnauthenticated(),
                serviceAccessPolicyConfiguration.isEnabled(),
                serviceAccessPolicyConfiguration.getName(),
                titleMap,
                serviceContext
            );
        } catch (PortalException e) {
            LOG.error(
                "Unexpected exception trying to add SAP Entry {} to company {} with a user {}",
                serviceAccessPolicyConfiguration.getName(),
                serviceContext.getCompanyId(),
                runAsUserId,
                e
            );
        }
    }

    protected static SAPEntry getSapEntry(Long companyId, String name) {
        try {
            return SAPEntryLocalServiceUtil.getSAPEntry(companyId, name);
        } catch (NoSuchEntryException e) {
            LOG.debug("SAP Entry {} not found in company {}", name, companyId);
        } catch (PortalException e) {
            LOG.error("Unexpected exception when trying to fetch SAP Entry {} from company {}", name, companyId);
        }

        return null;
    }
}
