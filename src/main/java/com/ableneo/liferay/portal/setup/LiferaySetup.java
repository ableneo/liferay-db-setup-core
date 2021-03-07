package com.ableneo.liferay.portal.setup;

import com.ableneo.liferay.portal.setup.core.SetupCustomFields;
import com.ableneo.liferay.portal.setup.core.SetupOrganizations;
import com.ableneo.liferay.portal.setup.core.SetupPages;
import com.ableneo.liferay.portal.setup.core.SetupPermissions;
import com.ableneo.liferay.portal.setup.core.SetupPortal;
import com.ableneo.liferay.portal.setup.core.SetupRoles;
import com.ableneo.liferay.portal.setup.core.SetupSites;
import com.ableneo.liferay.portal.setup.core.SetupUserGroups;
import com.ableneo.liferay.portal.setup.core.SetupUsers;
import com.ableneo.liferay.portal.setup.domain.Company;
import com.ableneo.liferay.portal.setup.domain.Configuration;
import com.ableneo.liferay.portal.setup.domain.CustomFields;
import com.ableneo.liferay.portal.setup.domain.ObjectsToBeDeleted;
import com.ableneo.liferay.portal.setup.domain.Organization;
import com.ableneo.liferay.portal.setup.domain.Setup;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.LocaleThreadLocal;
import com.liferay.portal.kernel.util.PortalUtil;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LiferaySetup {
    private static final Logger LOG = LoggerFactory.getLogger(LiferaySetup.class);

    private LiferaySetup() {}

    /**
     * Helper method that unmarshalls xml configuration from file.
     *
     * @param file file containing xml file that follows http://www.ableneo.com/liferay/setup schema
     * @return if setup was successfull
     * @throws FileNotFoundException when file was not found
     */
    public static boolean setup(final File file) throws FileNotFoundException {
        Setup setup = MarshallUtil.unmarshall(file);
        return setup(setup);
    }

    /**
     * Helper method that unmarshalls xml configuration from data stream.
     *
     * @param inputStream data containing xml file that follows http://www.ableneo.com/liferay/setup schema
     * @return if setup was successfull
     */
    public static boolean setup(final InputStream inputStream) {
        Setup setup = MarshallUtil.unmarshall(inputStream);
        return setup(setup);
    }

    /**
     * Main setup method that sets up the data configured with xml.
     *
     * @param setup Setup object, parsed from xml configuration
     * @return true if all was set up fine
     */
    public static boolean setup(final Setup setup) {
        if (setup == null) {
            throw new IllegalArgumentException(
                "Setup object cannot be null, without Setup object I cannot set up any data."
            );
        }

        Configuration configuration = setup.getConfiguration();
        String runAsUserEmail = configuration.getRunAsUserEmail();
        final String originalPrincipalName = PrincipalThreadLocal.getName();
        final PermissionChecker originalPermissionChecker = PermissionThreadLocal.getPermissionChecker();
        final Locale originalScopeGroupLocale = LocaleThreadLocal.getSiteDefaultLocale();

        try {
            // iterate over companies or choose default
            if (!configuration.getCompany().isEmpty()) {
                for (Company company : configuration.getCompany()) {
                    long companyId = company.getCompanyid() != null
                        ? company.getCompanyid()
                        : getCompanyIdFromCompanyWebId(company);
                    if (companyId == -1) {
                        LOG.error("Could not find company: {}", company);
                        continue;
                    }
                    SetupConfigurationThreadLocal.configureThreadLocalContent(runAsUserEmail, companyId);
                    executeSetupConfiguration(setup);

                    // iterate over group names or choose GUEST group for the company
                    if (company.getGroupName().isEmpty()) {
                        company.getGroupName().add(GroupConstants.GUEST);
                    }
                    for (String groupName : company.getGroupName()) {
                        setupGroup(setup, runAsUserEmail, companyId, groupName);
                    }
                }
            } else {
                setupGroup(
                    setup,
                    runAsUserEmail,
                    SetupConfigurationThreadLocal.getRunInCompanyId(),
                    GroupConstants.GUEST
                );
            }
        } catch (PortalException | RuntimeException e) {
            LOG.error("An error occured while executing the portal setup", e);
            return false;
        } finally {
            SetupConfigurationThreadLocal.cleanUp(
                originalPrincipalName,
                originalPermissionChecker,
                originalScopeGroupLocale
            );
        }
        return true;
    }

    private static void setupGroup(Setup setup, String runAsUserEmail, long companyId, String groupName)
        throws PortalException {
        Group group = GroupLocalServiceUtil.getGroup(companyId, groupName);
        SetupConfigurationThreadLocal.configureThreadLocalContent(
            runAsUserEmail,
            PortalUtil.getDefaultCompanyId(),
            group
        );
        setupPortalGroup(setup);
    }

    /**
     * Sets up data that require groupId.
     *
     * @param setup Setup object, parsed from xml configuration
     */
    private static void setupPortalGroup(Setup setup) {
        if (setup.getUsers() != null) {
            LOG.info("Setting up {} users", setup.getUsers().getUser().size());
            SetupUsers.setupUsers(setup.getUsers().getUser());
        }

        if (setup.getPageTemplates() != null) {
            SetupPages.setupPageTemplates(setup.getPageTemplates());
        }

        if (setup.getRoles() != null) {
            LOG.info("Setting up {} roles", setup.getRoles().getRole().size());
            SetupRoles.setupRoles(setup.getRoles().getRole());
        }

        LOG.info("Setup of portal groups finished");
    }

    private static long getCompanyIdFromCompanyWebId(Company company) {
        String companyWebId = null;
        try {
            companyWebId = company.getCompanywebid();
            return CompanyLocalServiceUtil.getCompanyByWebId(companyWebId).getCompanyId();
        } catch (PortalException | SystemException e) {
            LOG.error("Couldn't find company: {}", company, e);
        }
        return -1;
    }

    /**
     * Sets up data that does not require groupId.
     *
     * @param setup Setup object, parsed from xml configuration
     * @throws SystemException
     */
    private static void executeSetupConfiguration(final Setup setup) throws PortalException {
        if (setup.getDeleteLiferayObjects() != null) {
            LOG.info("Deleting {} object categories", setup.getDeleteLiferayObjects().getObjectsToBeDeleted().size());
            deleteObjects(setup.getDeleteLiferayObjects().getObjectsToBeDeleted());
        }
        if (setup.getPortal() != null) {
            LOG.info("Setting up Portal");
            SetupPortal.setupPortal(setup.getPortal());
        }
        if (setup.getCustomFields() != null) {
            LOG.info("Setting up {} custom fields", setup.getCustomFields().getField().size());
            SetupCustomFields.setupExpandoFields(setup.getCustomFields().getField());
        }
        if (setup.getOrganizations() != null) {
            LOG.info("Setting up {} organizations", setup.getOrganizations().getOrganization().size());
            SetupOrganizations.setupOrganizations(setup.getOrganizations().getOrganization(), null, null);
        }
        if (setup.getUserGroups() != null) {
            LOG.info("Setting up {} User Groups", setup.getUserGroups().getUserGroup().size());
            SetupUserGroups.setupUserGroups(setup.getUserGroups().getUserGroup());
        }
        if (setup.getResourcePermissions() != null) {
            LOG.info("Setting up {} resource permissions", setup.getResourcePermissions().getResource().size());
            SetupPermissions.setupPortletPermissions(setup.getResourcePermissions());
        }
        if (setup.getSites() != null) {
            LOG.info("Setting up {} sites", setup.getSites().getSite().size());
            SetupSites.setupSites(setup.getSites().getSite(), null);
        }
        LOG.info("Setup finished");
    }

    private static void deleteObjects(final List<ObjectsToBeDeleted> objectsToBeDeleted) {
        for (ObjectsToBeDeleted otbd : objectsToBeDeleted) {
            if (otbd.getRoles() != null) {
                List<com.ableneo.liferay.portal.setup.domain.Role> roles = otbd.getRoles().getRole();
                SetupRoles.deleteRoles(roles, otbd.getDeleteMethod());
            }
            if (otbd.getUsers() != null) {
                List<com.ableneo.liferay.portal.setup.domain.User> users = otbd.getUsers().getUser();
                SetupUsers.deleteUsers(users, otbd.getDeleteMethod());
            }
            if (otbd.getOrganizations() != null) {
                List<Organization> organizations = otbd.getOrganizations().getOrganization();
                SetupOrganizations.deleteOrganization(organizations, otbd.getDeleteMethod());
            }
            if (otbd.getCustomFields() != null) {
                List<CustomFields.Field> customFields = otbd.getCustomFields().getField();
                SetupCustomFields.deleteCustomFields(customFields, otbd.getDeleteMethod());
            }
            if (otbd.getSites() != null) {
                final List<Site> siteList = otbd.getSites().getSite();
                SetupSites.deleteSite(siteList, otbd.getDeleteMethod());
            }
        }
    }
}
