package com.ableneo.liferay.portal.setup.core;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Original work Copyright (C) 2016 - 2018 mimacom ag
 * Modified work Copyright (C) 2018 - 2020 ableneo, s. r. o.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.CustomFieldSettingUtil;
import com.ableneo.liferay.portal.setup.domain.CustomFieldSetting;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;

public final class SetupOrganizations {

    private static final Log LOG = LogFactoryUtil.getLog(SetupOrganizations.class);

    private SetupOrganizations() {

    }

    public static void setupOrganizations(
            final List<com.ableneo.liferay.portal.setup.domain.Organization> organizations,
            final Organization parentOrg, final Group parentGroup) {

        for (com.ableneo.liferay.portal.setup.domain.Organization organization : organizations) {
            try {
                Organization liferayOrg = null;
                Group liferayGroup = null;
                long groupId = -1;
                long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                try {
                    Organization org = OrganizationLocalServiceUtil.getOrganization(companyId, organization.getName());
                    liferayGroup = org.getGroup();
                    groupId = org.getGroupId();
                    liferayOrg = org;
                    LOG.info(String.format("Setup: Organization %1$s already exist in system, not creating...",
                            organization.getName()));

                } catch (PortalException | SystemException e) {
                    LOG.debug("Organization does not exist.", e);
                }

                if (groupId == -1) {
                    LOG.info(String.format("Setup: Organization %1$s does not exist in system, creating...",
                            organization.getName()));

                    Organization newOrganization =
                            OrganizationLocalServiceUtil.addOrganization(SetupConfigurationThreadLocal.getRunAsUserId(),
                                    OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID, organization.getName(),
                                    "organization", 0, 0, ListTypeConstants.ORGANIZATION_STATUS_DEFAULT,
                                    "Created by setup module.", false, new ServiceContext());
                    addOrganizationUser(newOrganization,
                            UserLocalServiceUtil.getUser(SetupConfigurationThreadLocal.getRunAsUserId()));
                    liferayOrg = newOrganization;
                    liferayGroup = liferayOrg.getGroup();
                    groupId = newOrganization.getGroupId();

                    LOG.info(String.format("New Organization created. Group ID: %1$s", groupId));
                }

                if (parentOrg != null && liferayOrg != null && organization.isMaintainOrganizationHierarchy()) {
                    liferayOrg.setParentOrganizationId(parentOrg.getOrganizationId());
                    OrganizationLocalServiceUtil.updateOrganization(liferayOrg);
                } else if (liferayOrg != null && organization.isMaintainOrganizationHierarchy()) {
                    liferayOrg.setParentOrganizationId(0);
                    OrganizationLocalServiceUtil.updateOrganization(liferayOrg);
                }

                setCustomFields(groupId, organization, liferayOrg);
                LOG.info("Organization custom fields set up.");

                Site orgSite = organization.getSite();

                if (orgSite == null) {
                    LOG.info("Organization has no site defined. All is set.");
                } else if (orgSite.isDefault() || orgSite.getName() == null || orgSite.getName().isEmpty()) {
                    LOG.error("It is not possible to set global or default within organization. Skipping site setup.");
                } else {
                    LOG.info("Setting up site for organization.");
                    liferayGroup.setSite(true);
                    liferayGroup.setName(orgSite.getName());
                    GroupLocalServiceUtil.updateGroup(liferayGroup);
                    liferayGroup = liferayOrg.getGroup();

                    if (liferayGroup != null && orgSite.getSiteFriendlyUrl() != null
                            && !orgSite.getSiteFriendlyUrl().isEmpty()) {
                        liferayGroup.setFriendlyURL(orgSite.getSiteFriendlyUrl());
                        GroupLocalServiceUtil.updateGroup(liferayGroup);
                        liferayGroup = liferayOrg.getGroup();
                    }

                    if (parentGroup != null && liferayGroup != null && orgSite.isMaintainSiteHierarchy()) {
                        liferayGroup.setParentGroupId(parentGroup.getGroupId());
                        GroupLocalServiceUtil.updateGroup(liferayGroup);
                    } else if (liferayGroup != null && orgSite.isMaintainSiteHierarchy()) {
                        liferayGroup.setParentGroupId(0);
                        GroupLocalServiceUtil.updateGroup(liferayGroup);
                    }

                    LOG.info("Setting organization site content...");

                    SetupDocumentFolders.setupDocumentFolders(orgSite, groupId);
                    LOG.info("Document Folders setting finished.");

                    SetupDocuments.setupSiteDocuments(orgSite, groupId);
                    LOG.info("Documents setting finished.");

                    SetupPages.setupSitePages(orgSite, groupId);
                    LOG.info("Organization Pages setting finished.");

                    SetupWebFolders.setupWebFolders(orgSite, groupId);
                    LOG.info("Web folders setting finished.");

                    SetupCategorization.setupVocabularies(orgSite.getVocabulary(), groupId);
                    LOG.info("Organization Categories setting finished.");

                    SetupArticles.setupSiteArticles(orgSite.getArticle(), orgSite.getAdt(), orgSite.getDdlRecordset(),
                            groupId);
                    LOG.info("Organization Articles setting finished.");

                    SetupSites.setCustomFields(groupId, orgSite.getCustomFieldSetting());
                    LOG.info("Organization site custom fields set up.");

                }

                List<com.ableneo.liferay.portal.setup.domain.Organization> orgs = organization.getOrganization();
                setupOrganizations(orgs, liferayOrg, liferayGroup);

            } catch (Exception e) {
                LOG.error(String.format("Error by setting up organization %1$s", organization.getName()), e);
            }
        }

    }

    private static void setCustomFields(final long groupId,
            final com.ableneo.liferay.portal.setup.domain.Organization org, final Organization liferayOrg) {

        Class clazz = Organization.class;
        String resolverHint = "Resolving customized value for page " + org.getName() + " " + "failed for key %%key%% "
                + "and value %%value%%";
        for (CustomFieldSetting cfs : org.getCustomFieldSetting()) {
            String key = cfs.getKey();
            String value = cfs.getValue();
            long company = SetupConfigurationThreadLocal.getRunInCompanyId();
            CustomFieldSettingUtil.setExpandoValue(resolverHint.replace("%%key%%", key).replace("%%value%%", value),
                    groupId, company, clazz, liferayOrg.getOrganizationId(), key, value);
        }
    }

    public static void deleteOrganization(
            final List<com.ableneo.liferay.portal.setup.domain.Organization> organizations, final String deleteMethod) {

        switch (deleteMethod) {
            case "excludeListed":
                Map<String, com.ableneo.liferay.portal.setup.domain.Organization> toBeDeletedOrganisations =
                        convertOrganisationListToHashMap(organizations);
                try {
                    for (Organization organisation : OrganizationLocalServiceUtil.getOrganizations(-1, -1)) {
                        if (!toBeDeletedOrganisations.containsKey(organisation.getName())) {
                            try {
                                OrganizationLocalServiceUtil.deleteOrganization(organisation.getOrganizationId());
                                LOG.info(String.format("Deleting Organisation %1$s", organisation.getName()));
                            } catch (Exception e) {
                                LOG.error("Error by deleting Organisation !", e);
                            }
                        }
                    }
                } catch (SystemException e) {
                    LOG.error("Error by retrieving organisations!", e);
                }
                break;

            case "onlyListed":
                for (com.ableneo.liferay.portal.setup.domain.Organization organisation : organizations) {
                    String name = organisation.getName();
                    try {
                        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                        Organization o = OrganizationLocalServiceUtil.getOrganization(companyId, name);
                        OrganizationLocalServiceUtil.deleteOrganization(o);
                    } catch (Exception e) {
                        LOG.error("Error by deleting Organisation !", e);
                    }
                    LOG.info(String.format("Deleting Organisation %1$s", name));
                }

                break;

            default:
                LOG.error(String.format("Unknown delete method : %1$s", deleteMethod));
                break;
        }
    }

    public static void addOrganizationUser(Organization organization, User user) {
        LOG.info(String.format("Adding user with screenName: %1$sto organization with name: %2$s", user.getScreenName(),
                organization.getName()));
        OrganizationLocalServiceUtil.addUserOrganization(user.getUserId(), organization);
    }

    public static void addOrganizationUsers(Organization organization, User... users) {
        for (int i = 0; i < users.length; i++) {
            addOrganizationUser(organization, users[i]);
        }
    }

    private static Map<String, com.ableneo.liferay.portal.setup.domain.Organization> convertOrganisationListToHashMap(
            final List<com.ableneo.liferay.portal.setup.domain.Organization> objects) {

        HashMap<String, com.ableneo.liferay.portal.setup.domain.Organization> map = new HashMap<>();
        for (com.ableneo.liferay.portal.setup.domain.Organization organization : objects) {
            map.put(organization.getName(), organization);
        }
        return map;
    }

}
