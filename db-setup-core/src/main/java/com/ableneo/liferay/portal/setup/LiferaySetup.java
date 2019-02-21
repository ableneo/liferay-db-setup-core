package com.ableneo.liferay.portal.setup;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2018 mimacom ag
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
import java.util.Objects;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import com.ableneo.liferay.portal.setup.core.SetupContext;
import org.xml.sax.SAXException;

import com.ableneo.liferay.portal.setup.core.SetupCustomFields;
import com.ableneo.liferay.portal.setup.core.SetupOrganizations;
import com.ableneo.liferay.portal.setup.core.SetupPages;
import com.ableneo.liferay.portal.setup.core.SetupPermissions;
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
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalThreadLocal;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

public final class LiferaySetup {


    private static final Log LOG = LogFactoryUtil.getLog(LiferaySetup.class);
    private static SetupContext setupContext;

    public LiferaySetup(String adminRoleName, String defaultGroupName) {
        setupContext = new SetupContext(adminRoleName, defaultGroupName);
    }

    public boolean setup(final File file)
            throws FileNotFoundException, ParserConfigurationException, SAXException, JAXBException {
        Setup setup = MarshallUtil.unmarshall(file);
        return setup(setup);
    }

    public boolean setup(final Setup setup) {

        Configuration configuration = setup.getConfiguration();
        String runAsUserEmail = configuration.getRunasuser();
        final String principalName = PrincipalThreadLocal.getName();
        final PermissionChecker permissionChecker = PermissionThreadLocal.getPermissionChecker();

        try {
            // iterate over companies or choose default
            if (!configuration.getCompany().isEmpty()) {
                for (Company company : configuration.getCompany()) {
                    Long companyId = company.getCompanyid() != null
                            ? company.getCompanyid() : getCompanyIdFromCompanyWebId(company);
                    if (companyId == -1) {
                        continue;
                    }
                    configureThreadPermission(runAsUserEmail, companyId);
                    executeSetupConfiguration(setup);

                    // iterate over group names or choose GUEST group for the company
                    if (company.getGroupName().isEmpty()) {
                        company.getGroupName().add(GroupConstants.GUEST);
                    }
                    for (String groupName : company.getGroupName()) {
                        long groupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
                        setupPortalGroup(setup);
                    }
                }
            } else {
                configureThreadPermission(runAsUserEmail, PortalUtil.getDefaultCompanyId());
                executeSetupConfiguration(setup);

                long groupId = GroupLocalServiceUtil.getGroup(setupContext.getRunInGroupId(), GroupConstants.GUEST).getGroupId();
                setupContext.setRunInGroupId(groupId);
                setupPortalGroup(setup);
            }
        } catch (Exception e) {
            LOG.error("An error occured while executing the portal setup ", e);
            return false;

        } finally {
            PrincipalThreadLocal.setName(principalName);
            PermissionThreadLocal.setPermissionChecker(permissionChecker);
        }
        return true;
    }

    private static long getCompanyIdFromCompanyWebId(Company company) {
        String companyWebId = null;
        try {
            companyWebId = company.getCompanywebid();
            return CompanyLocalServiceUtil.getCompanyByWebId(companyWebId).getCompanyId();
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Couldn't find company: %1$s", company));
        }
        return -1;
    }

    private void configureThreadPermission(String runAsUserEmail, long companyId) throws Exception {
        if (runAsUserEmail == null || runAsUserEmail.isEmpty()) {
            setupContext.setRunAsUserId(UserLocalServiceUtil.getDefaultUserId(companyId));
            setupContext.setRunInCompanyId(companyId);
            setAdminPermissionCheckerForThread();
            LOG.info("Using default administrator.");
        } else {
            User user = UserLocalServiceUtil.getUserByEmailAddress(PortalUtil.getDefaultCompanyId(), runAsUserEmail);
            setupContext.setRunAsUserId(user.getUserId());
            setupContext.setRunInCompanyId(companyId);
            PrincipalThreadLocal.setName(setupContext.getRunAsUserId());
            PermissionChecker permissionChecker;
            try {
                permissionChecker = PermissionCheckerFactoryUtil.create(user);
            } catch (Exception e) {
                throw new LiferaySetupException(
                        "An error occured while trying to create permissionchecker for user: " + runAsUserEmail, e);
            }
            PermissionThreadLocal.setPermissionChecker(permissionChecker);

            LOG.info("Execute setup module as user " + runAsUserEmail);
        }
    }

    private void executeSetupConfiguration(final Setup setup) throws SystemException {
        if (setup.getDeleteLiferayObjects() != null) {
            LOG.info("Deleting : " + setup.getDeleteLiferayObjects().getObjectsToBeDeleted().size() + " objects");
            deleteObjects(setup.getDeleteLiferayObjects().getObjectsToBeDeleted());
        }

        if (setup.getCustomFields() != null) {
            LOG.info("Setting up " + setup.getCustomFields().getField().size() + " custom fields");
            (new SetupCustomFields(setupContext.clone())).setupExpandoFields(setup.getCustomFields().getField());
        }

        if (setup.getOrganizations() != null) {
            LOG.info("Setting up " + setup.getOrganizations().getOrganization().size() + " organizations");
            (new SetupOrganizations(setupContext.clone())).setupOrganizations(setup.getOrganizations().getOrganization(), null, null);
        }
        if (setup.getUserGroups() != null) {
            LOG.info("Setting up " + setup.getUserGroups().getUserGroup().size() + " User Groups");
            (new SetupUserGroups(setupContext.clone())).setupUserGroups(setup.getUserGroups().getUserGroup());
        }

        if (setup.getPortletPermissions() != null) {
            LOG.info("Setting up " + setup.getPortletPermissions().getPortlet().size() + " roles");
            (new SetupPermissions(setupContext.clone())).setupPortletPermissions(setup.getPortletPermissions());
        }

        if (setup.getSites() != null) {
            LOG.info("Setting up " + setup.getSites().getSite().size() + " sites");
            (new SetupSites(setupContext.clone())).setupSites(setup.getSites().getSite(), null);
        }


        LOG.info("Setup finished");
    }

    private void setupPortalGroup(Setup setup) throws SystemException {
        if (setup.getUsers() != null) {
            LOG.info("Setting up " + setup.getUsers().getUser().size() + " users");
            (new SetupUsers(setupContext.clone())).setupUsers(setup.getUsers().getUser());
        }

        if (setup.getPageTemplates() != null) {
            (new SetupPages(setupContext.clone())).setupPageTemplates(setup.getPageTemplates());
        }

        if (setup.getRoles() != null) {
            LOG.info("Setting up " + setup.getRoles().getRole().size() + " roles");
            (new SetupRoles(setupContext.clone())).setupRoles(setup.getRoles().getRole());
        }

        LOG.info("Setup of portal groups finished");
    }

    /**
     * Initializes permission checker for Liferay Admin. Used to grant access to
     * custom fields.
     *
     * @throws Exception if cannot set permission checker
     */
    private void setAdminPermissionCheckerForThread() throws Exception {
        com.liferay.portal.model.User adminUser = getAdminUser(setupContext.getRunInCompanyId());
        setupContext.setRunAsUserId(Objects.requireNonNull(adminUser).getUserId());
        PrincipalThreadLocal.setName(setupContext.getRunAsUserId());
        PermissionChecker permissionChecker;
        try {
            permissionChecker = PermissionCheckerFactoryUtil.create(adminUser);
        } catch (Exception e) {
            throw new Exception("Cannot obtain permission checker for Liferay Administrator user", e);
        }
        PermissionThreadLocal.setPermissionChecker(permissionChecker);
    }

    private void deleteObjects(final List<ObjectsToBeDeleted> objectsToBeDeleted) {

        for (ObjectsToBeDeleted otbd : objectsToBeDeleted) {

            if (otbd.getRoles() != null) {
                List<com.ableneo.liferay.portal.setup.domain.Role> roles = otbd.getRoles().getRole();
                (new SetupRoles(setupContext.clone())).deleteRoles(roles, otbd.getDeleteMethod());
            }

            if (otbd.getUsers() != null) {
                List<com.ableneo.liferay.portal.setup.domain.User> users = otbd.getUsers().getUser();
                (new SetupUsers(setupContext.clone())).deleteUsers(users, otbd.getDeleteMethod());
            }

            if (otbd.getOrganizations() != null) {
                List<Organization> organizations = otbd.getOrganizations().getOrganization();
                (new SetupOrganizations(setupContext.clone())).deleteOrganization(organizations, otbd.getDeleteMethod());
            }

            if (otbd.getCustomFields() != null) {
                List<CustomFields.Field> customFields = otbd.getCustomFields().getField();
                (new SetupCustomFields(setupContext.clone())).deleteCustomFields(customFields, otbd.getDeleteMethod());
            }
        }
    }

    /**
     * Returns Liferay user, that has Administrator role assigned.
     *
     * @param companyId company ID
     * @return Liferay {@link com.ableneo.liferay.portal.setup.domain.User}
     *         instance, if no user is found, returns null
     * @throws Exception if cannot obtain permission checker
     */
    private com.liferay.portal.model.User getAdminUser(final long companyId) throws Exception {

        try {
            com.liferay.portal.model.Role adminRole = RoleLocalServiceUtil.getRole(companyId, setupContext.getAdminRoleName());
            List<com.liferay.portal.model.User> adminUsers = UserLocalServiceUtil.getRoleUsers(adminRole.getRoleId());

            if (adminUsers == null || adminUsers.isEmpty()) {
                return null;
            }
            return adminUsers.get(0);

        } catch (PortalException | SystemException e) {
            throw new Exception("Cannot obtain Liferay role for role name: " + setupContext.getAdminRoleName(), e);
        }
    }

    public boolean setup(final InputStream inputStream)
            throws ParserConfigurationException, SAXException, JAXBException {
        Setup setup = MarshallUtil.unmarshall(inputStream);
        return setup(setup);
    }
}
