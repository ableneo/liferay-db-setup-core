package com.ableneo.liferay.portal.setup;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2019 ableneo s. r. o.
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
import java.util.List;
import java.util.Objects;

import com.ableneo.liferay.portal.setup.core.*;
import com.ableneo.liferay.portal.setup.domain.*;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;

public final class LiferaySetup {

    private static final Log LOG = LogFactoryUtil.getLog(LiferaySetup.class);

    private LiferaySetup() {}

    /**
     * @param file configuration file
     * @return true if all was set up fine
     * @throws FileNotFoundException if provided configuration file could not be found
     */
    public static boolean setup(final File file) throws FileNotFoundException {
        Setup setup = MarshallUtil.unmarshall(file);
        return setup(setup);
    }

    /**
     *
     * @param setup configuration of db setup runner
     * @return true if all was set up fine
     */
    public static boolean setup(final Setup setup) {

        Configuration configuration = setup.getConfiguration();
        String runAsUserEmail = configuration.getRunAsUserEmail();
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
                    } else {
                        SetupConfigurationThreadLocal.setRunInCompanyId(companyId);
                    }
                    configureThreadPermission(runAsUserEmail, companyId);
                    executeSetupConfiguration(setup);

                    // iterate over group names or choose default group for the company
                    if (company.getGroupName().isEmpty()) {
                        company.getGroupName().add(GroupConstants.GUEST);
                    }
                    for (String groupName : company.getGroupName()) {
                        long groupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
                        SetupConfigurationThreadLocal.setRunInGroupId(groupId);
                        setupPortalGroup(setup);
                    }
                }
            } else {
                configureThreadPermission(runAsUserEmail, PortalUtil.getDefaultCompanyId());
                executeSetupConfiguration(setup);
                setupPortalGroup(setup);
            }
        } catch (Exception e) {
            LOG.error("An error occured while executing the portal setup ", e);
            return false;
        } finally {
            PrincipalThreadLocal.setName(principalName);
            PermissionThreadLocal.setPermissionChecker(permissionChecker);
            SetupConfigurationThreadLocal.clear(); // in case it'll be run multiple times in the same thread
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

    private static void configureThreadPermission(String runAsUserEmail, long companyId) throws Exception {
        if (runAsUserEmail == null || runAsUserEmail.isEmpty()) {
            SetupConfigurationThreadLocal.setRunAsUserId(UserLocalServiceUtil.getDefaultUserId(companyId));
            SetupConfigurationThreadLocal.setRunInCompanyId(companyId);
            setAdminPermissionCheckerForThread();
            LOG.info("Using default administrator.");
        } else {
            User user = UserLocalServiceUtil.getUserByEmailAddress(PortalUtil.getDefaultCompanyId(), runAsUserEmail);
            SetupConfigurationThreadLocal.setRunAsUserId(user.getUserId());
            SetupConfigurationThreadLocal.setRunInCompanyId(companyId);
            PrincipalThreadLocal.setName(user.getUserId());
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

    private static void executeSetupConfiguration(final Setup setup) throws SystemException {
        if (setup.getDeleteLiferayObjects() != null) {
            LOG.info("Deleting : " + setup.getDeleteLiferayObjects().getObjectsToBeDeleted().size() + " objects");
            deleteObjects(setup.getDeleteLiferayObjects().getObjectsToBeDeleted());
        }

        if (setup.getCustomFields() != null) {
            LOG.info("Setting up " + setup.getCustomFields().getField().size() + " custom fields");
            SetupCustomFields.setupExpandoFields(setup.getCustomFields().getField());
        }

        if (setup.getRoles() != null) {
            LOG.info("Setting up " + setup.getRoles().getRole().size() + " roles");
            SetupRoles.setupRoles(setup.getRoles().getRole());
        }
        if (setup.getUsers() != null) {
            LOG.info("Setting up " + setup.getUsers().getUser().size() + " users");
            SetupUsers.setupUsers(setup.getUsers().getUser());
        }

        if (setup.getOrganizations() != null) {
            LOG.info("Setting up " + setup.getOrganizations().getOrganization().size() + " organizations");
            SetupOrganizations.setupOrganizations(setup.getOrganizations().getOrganization(), null, null);
        }

        if (setup.getUserGroups() != null) {
            LOG.info("Setting up " + setup.getUserGroups().getUserGroup().size() + " User Groups");
            SetupUserGroups.setupUserGroups(setup.getUserGroups().getUserGroup());
        }

        if (setup.getResourcePermissions() != null) {
            LOG.info("Setting up " + setup.getResourcePermissions().getResource().size() + " roles");
            SetupPermissions.setupPortletPermissions(setup.getResourcePermissions());
        }

        if (setup.getSites() != null) {
            LOG.info("Setting up " + setup.getSites().getSite().size() + " sites");
            SetupSites.setupSites(setup.getSites().getSite(), null);
        }

        LOG.info("Setup finished");
    }

    private static void setupPortalGroup(Setup setup) throws SystemException {
        if (setup.getUsers() != null) {
            LOG.info("Setting up " + setup.getUsers().getUser().size() + " users");
            SetupUsers.setupUsers(setup.getUsers().getUser());
        }

        if (setup.getPageTemplates() != null) {
            SetupPages.setupPageTemplates(setup.getPageTemplates());
        }

        if (setup.getRoles() != null) {
            LOG.info("Setting up " + setup.getRoles().getRole().size() + " roles");
            SetupRoles.setupRoles(setup.getRoles().getRole());
        }

        LOG.info("Setup of portal groups finished");
    }

    /**
     * Initializes permission checker for Liferay Admin. Used to grant access to
     * custom fields.
     *
     * @throws Exception if cannot set permission checker
     */
    private static void setAdminPermissionCheckerForThread() throws Exception {
        User adminUser = getAdminUser();
        SetupConfigurationThreadLocal.setRunAsUserId(Objects.requireNonNull(adminUser).getUserId());
        PrincipalThreadLocal.setName(adminUser.getUserId());
        PermissionChecker permissionChecker;
        try {
            permissionChecker = PermissionCheckerFactoryUtil.create(adminUser);
        } catch (Exception e) {
            throw new Exception("Cannot obtain permission checker for Liferay Administrator user", e);
        }
        PermissionThreadLocal.setPermissionChecker(permissionChecker);
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
        }
    }

    /**
     * Returns Liferay user, that has Administrator role assigned.
     *
     * @return Liferay {@link com.ableneo.liferay.portal.setup.domain.User}
     *         instance, if no user is found, returns null
     * @throws Exception if cannot obtain permission checker
     */
    private static User getAdminUser() throws Exception {

        try {
            Role adminRole = RoleLocalServiceUtil.getRole(SetupConfigurationThreadLocal.getRunInCompanyId(),
                    RoleConstants.ADMINISTRATOR);
            List<User> adminUsers = UserLocalServiceUtil.getRoleUsers(adminRole.getRoleId());

            if (adminUsers == null || adminUsers.isEmpty()) {
                return null;
            }
            return adminUsers.get(0);

        } catch (PortalException | SystemException e) {
            throw new Exception("Cannot obtain Liferay role for role name: " + RoleConstants.ADMINISTRATOR, e);
        }
    }

}
