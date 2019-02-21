package com.ableneo.liferay.portal.setup.core;

/*-
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2018 mimacom ag
 * Modified work Copyright (C) 2018 - 2020 ableneo Slovensko s.r.o.
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

import com.ableneo.liferay.portal.setup.core.util.CustomFieldSettingUtil;
import com.ableneo.liferay.portal.setup.domain.CustomFieldSetting;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.ListTypeConstants;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.OrganizationConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;

public final class SetupOrganizations {

    public static final String DESCRIPTION = "Created by setup module.";
    private static final Log LOG = LogFactoryUtil.getLog(SetupOrganizations.class);
    private final SetupContext setupContext;

    public SetupOrganizations(SetupContext setupContext) {
        this.setupContext = setupContext;
    }

    public void setupOrganizations(
            final List<com.ableneo.liferay.portal.setup.domain.Organization> organizations,
            final Organization parentOrg, final Group parentGroup) {
        final long userId = getRunAsUserId();

        for (com.ableneo.liferay.portal.setup.domain.Organization organization : organizations) {
            try {
                Organization liferayOrg = null;
                Group liferayGroup = null;
                long groupId = -1;
                try {
                    Organization org =
                            OrganizationLocalServiceUtil.getOrganization(getRunInCompanyId(), organization.getName());
                    liferayGroup = org.getGroup();
                    groupId = org.getGroupId();
                    liferayOrg = org;
                    LOG.info("Setup: Organization " + organization.getName()
                            + " already exist in system, not creating...");

                } catch (PortalException | SystemException e) {
                    LOG.debug("Organization does not exist.", e);
                }

                if (groupId == -1) {
                    LOG.info(
                            "Setup: Organization " + organization.getName() + " does not exist in system, creating...");

                    long defaultUserId = UserLocalServiceUtil.getDefaultUserId(getRunInCompanyId());
                    Organization newOrganization = OrganizationLocalServiceUtil.addOrganization(defaultUserId,
                            OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID, organization.getName(),
                            OrganizationConstants.TYPE_REGULAR_ORGANIZATION, 0, 0,
                            ListTypeConstants.ORGANIZATION_STATUS_DEFAULT, DESCRIPTION, false, new ServiceContext());
                    addOrganizationUser(newOrganization, UserLocalServiceUtil.getUser(defaultUserId));
                    liferayOrg = newOrganization;
                    liferayGroup = liferayOrg.getGroup();
                    groupId = newOrganization.getGroupId();
                    LOG.info("New Organization created. Group ID: " + groupId);

                }

                if (parentOrg != null && liferayOrg != null && organization.isMaintainOrganizationHierarchy()) {
                    liferayOrg.setParentOrganizationId(parentOrg.getOrganizationId());
                    OrganizationLocalServiceUtil.updateOrganization(liferayOrg);
                } else if (liferayOrg != null && organization.isMaintainOrganizationHierarchy()) {
                    liferayOrg.setParentOrganizationId(0);
                    OrganizationLocalServiceUtil.updateOrganization(liferayOrg);
                }

                setCustomFields(userId, groupId, getRunInCompanyId(), organization, liferayOrg);
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

                    (new SetupDocumentFolders(setupContext.clone())).setupDocumentFolders(orgSite);
                    LOG.info("Document Folders setting finished.");

                    (new SetupDocuments(setupContext.clone())).setupSiteDocuments(orgSite);
                    LOG.info("Documents setting finished.");

                    (new SetupPages(setupContext.clone())).setupSitePages(orgSite);
                    LOG.info("Organization Pages setting finished.");

                    (new SetupWebFolders(setupContext.clone())).setupWebFolders(orgSite);
                    LOG.info("Web folders setting finished.");

                    (new SetupCategorization(setupContext.clone())).setupVocabularies(orgSite);
                    LOG.info("Organization Categories setting finished.");

                    (new SetupArticles(setupContext.clone())).setupSiteArticles(orgSite);
                    LOG.info("Organization Articles setting finished.");

                    (new SetupSites(setupContext.clone())).setCustomFields(orgSite);
                    LOG.info("Organization site custom fields set up.");

                }

                List<com.ableneo.liferay.portal.setup.domain.Organization> orgs = organization.getOrganization();
                setupOrganizations(orgs, liferayOrg, liferayGroup);

            } catch (Exception e) {
                LOG.error("Error by setting up organization " + organization.getName(), e);
            }
        }

    }

    private void setCustomFields(final long runAsUserId, final long groupId, final long company,
            final com.ableneo.liferay.portal.setup.domain.Organization org, final Organization liferayOrg) {

        Class clazz = Organization.class;
        String resolverHint = "Resolving customized value for page " + org.getName() + " " + "failed for key %%key%% "
                + "and value %%value%%";
        for (CustomFieldSetting cfs : org.getCustomFieldSetting()) {
            String key = cfs.getKey();
            String value = cfs.getValue();
            CustomFieldSettingUtil.setExpandoValue(setupContext, resolverHint.replace("%%key%%", key).replace("%%value%%", value),
                    clazz, liferayOrg.getOrganizationId(), key, value);
        }
    }

    public void deleteOrganization(
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
                                LOG.info("Deleting Organisation" + organisation.getName());
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
                        Organization o = OrganizationLocalServiceUtil.getOrganization(getRunInCompanyId(), name);
                        OrganizationLocalServiceUtil.deleteOrganization(o);
                    } catch (Exception e) {
                        LOG.error("Error by deleting Organisation !", e);
                    }
                    LOG.info("Deleting Organisation " + name);
                }

                break;

            default:
                LOG.error("Unknown delete method : " + deleteMethod);
                break;
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

    public long getRunInCompanyId() {
        return setupContext.getRunInCompanyId();
    }

    public static void addOrganizationUsers(Organization organization, User... users) throws SystemException {
        for (int i = 0; i < users.length; i++) {
            addOrganizationUser(organization, users[i]);
        }
    }

    public static void addOrganizationUser(Organization organization, User user) throws SystemException {
        LOG.info("Adding user with screenName: " + user.getScreenName() + "to organization with name: "
                + organization.getName());
        OrganizationLocalServiceUtil.addUserOrganization(user.getUserId(), organization);
    }

    public long getRunAsUserId() {
        return setupContext.getRunAsUserId();
    }

}
