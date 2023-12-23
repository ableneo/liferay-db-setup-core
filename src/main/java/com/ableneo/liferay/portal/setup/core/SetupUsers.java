package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.CustomFieldSettingUtil;
import com.ableneo.liferay.portal.setup.domain.CustomFieldSetting;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SetupUsers {
    private static final Log LOG = LogFactoryUtil.getLog(SetupUsers.class);
    private static final int DEFAULT_BIRTHDAY_YEAR = 1970;

    private SetupUsers() {}

    public static void setupUsers(final List<com.ableneo.liferay.portal.setup.domain.User> users) {
        for (com.ableneo.liferay.portal.setup.domain.User user : users) {
            User liferayUser = null;
            long runInCompanyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            try {
                liferayUser = UserLocalServiceUtil.getUserByEmailAddress(runInCompanyId, user.getEmailAddress());
                LOG.info(String.format("User %1$s already exist, not creating...", liferayUser.getEmailAddress()));
            } catch (NoSuchUserException e) {
                liferayUser = addUser(user);
            } catch (Exception e) {
                LOG.error(String.format("Error by retrieving user %1$s", user.getEmailAddress()));
            }

            if (null != liferayUser) {
                addUserToOrganizations(user, liferayUser);
                addRolesToUser(user, liferayUser);
                addGroupsToUser(user, liferayUser);
                if (user.getCustomFieldSetting() != null && !user.getCustomFieldSetting().isEmpty()) {
                    setCustomFields(SetupConfigurationThreadLocal.getRunInGroupId(), runInCompanyId, liferayUser, user);
                }
            } else {
                LOG.warn(String.format("Could not create user with screenName '%1$s'", user.getScreenName()));
            }
        }
    }

    private static void setCustomFields(
        final long groupId,
        final long company,
        final User liferayUser,
        final com.ableneo.liferay.portal.setup.domain.User user
    ) {
        Class clazz = liferayUser.getClass();
        for (CustomFieldSetting cfs : user.getCustomFieldSetting()) {
            String resolverHint =
                "Custom value for user " +
                user.getScreenName() +
                ", " +
                user.getEmailAddress() +
                "" +
                " Key " +
                cfs.getKey() +
                ", value " +
                cfs.getValue();
            CustomFieldSettingUtil.setExpandoValue(
                resolverHint,
                groupId,
                company,
                clazz,
                liferayUser.getUserId(),
                cfs.getKey(),
                cfs.getValue()
            );
        }
    }

    private static User addUser(final com.ableneo.liferay.portal.setup.domain.User setupUser) {
        LOG.info(String.format("User %1$s not exists, creating...", setupUser.getEmailAddress()));

        User liferayUser = null;
        long creatorUserId = 0;
        boolean autoPassword = false;
        String password1 = setupUser.getPassword();
        String password2 = setupUser.getPassword();
        boolean autoScreenName = false;
        boolean male = true;
        String emailAddress = setupUser.getEmailAddress();
        long facebookId = 0;
        String jobTitle = StringPool.BLANK;
        String openId = StringPool.BLANK;
        Locale locale = Locale.US;
        String middleName = StringPool.BLANK;
        int prefixId = 0;
        int suffixId = 0;
        int birthdayMonth = Calendar.JANUARY;
        int birthdayDay = 1;
        int birthdayYear = DEFAULT_BIRTHDAY_YEAR;
        long[] groupIds = new long[] {};
        long[] roleIds = new long[] {};
        long[] organizationIds = new long[] {};
        long[] userGroupIds = null;
        boolean sendEmail = false;
        ServiceContext serviceContext = new ServiceContext();

        try {
            liferayUser =
                UserLocalServiceUtil.addUser(
                    creatorUserId,
                    SetupConfigurationThreadLocal.getRunInCompanyId(),
                    autoPassword,
                    password1,
                    password2,
                    autoScreenName,
                    setupUser.getScreenName(),
                    emailAddress,
                    facebookId,
                    openId,
                    locale,
                    setupUser.getFirstName(),
                    middleName,
                    setupUser.getLastName(),
                    prefixId,
                    suffixId,
                    male,
                    birthdayMonth,
                    birthdayDay,
                    birthdayYear,
                    jobTitle,
                    groupIds,
                    organizationIds,
                    roleIds,
                    userGroupIds,
                    sendEmail,
                    serviceContext
                );
            LOG.info(String.format("User %1$s created", setupUser.getEmailAddress()));
        } catch (Exception ex) {
            LOG.error(String.format("Error by adding user %1$s", setupUser.getEmailAddress()), ex);
        }

        return liferayUser;
    }

    private static void addUserToOrganizations(
        final com.ableneo.liferay.portal.setup.domain.User setupUser,
        final User liferayUser
    ) {
        try {
            for (com.ableneo.liferay.portal.setup.domain.Organization organization : setupUser.getOrganization()) {
                Organization liferayOrganization = OrganizationLocalServiceUtil.getOrganization(
                    SetupConfigurationThreadLocal.getRunInCompanyId(),
                    organization.getName()
                );
                UserLocalServiceUtil.addOrganizationUsers(
                    liferayOrganization.getOrganizationId(),
                    new long[] { liferayUser.getUserId() }
                );
                LOG.info(
                    String.format(
                        "Adding user %1$s to Organization %2$s",
                        setupUser.getEmailAddress(),
                        liferayOrganization.getName()
                    )
                );
            }
        } catch (PortalException | SystemException e) {
            LOG.error("cannot add users");
        }
    }

    private static void addGroupsToUser(com.ableneo.liferay.portal.setup.domain.User setupUser, User liferayUser) {
        for (com.ableneo.liferay.portal.setup.domain.UserGroup setupGroup : setupUser.getUserGroup()) {
            try {
                long runInCompanyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                //                Group group = GroupLocalServiceUtil.getGroup(runInCompanyId, setupGroup.getName());
                UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(runInCompanyId, setupGroup.getName());
                UserGroupLocalServiceUtil.addUserUserGroup(liferayUser.getUserId(), userGroup);
                LOG.info(
                    String.format("Added user(%1$s) to group(%2$s)", setupUser.getEmailAddress(), setupGroup.getName())
                );
            } catch (PortalException | SystemException e) {
                LOG.error(
                    String.format(
                        "Error in adding user(%1$s) to group(%2$s)",
                        setupUser.getEmailAddress(),
                        setupGroup.getName()
                    ),
                    e
                );
            }
        }
    }

    private static void addRolesToUser(
        final com.ableneo.liferay.portal.setup.domain.User setupUser,
        final User liferayUser
    ) {
        try {
            for (com.ableneo.liferay.portal.setup.domain.Role userRole : setupUser.getRole()) {
                long runInCompanyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                Role role = RoleLocalServiceUtil.getRole(runInCompanyId, userRole.getName());
                long[] roleIds = { role.getRoleId() };
                String roleType = userRole.getType();
                switch (roleType) {
                    case "portal":
                        RoleLocalServiceUtil.addUserRoles(liferayUser.getUserId(), roleIds);
                        LOG.info(
                            String.format(
                                "Adding regular role %1$s to user %2$s",
                                userRole.getName(),
                                liferayUser.getEmailAddress()
                            )
                        );
                        break;
                    case "site":
                    case "organization":
                        Group group = GroupLocalServiceUtil.getGroup(runInCompanyId, userRole.getSite());
                        UserGroupRoleLocalServiceUtil.addUserGroupRoles(
                            liferayUser.getUserId(),
                            group.getGroupId(),
                            roleIds
                        );

                        LOG.info(
                            "Adding " +
                            roleType +
                            " role " +
                            userRole.getName() +
                            " to user " +
                            liferayUser.getEmailAddress()
                        );
                        break;
                    default:
                        LOG.error(String.format("unknown role type %1$s", roleType));
                        break;
                }
            }
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Error in adding roles to user %1$s", setupUser.getEmailAddress()), e);
        }
    }

    /**
     * by this method, all users will be deleted from liferay, excluding those
     * listed in the setup.xml. from security reasons, no administrators, or
     * default users are deleted
     */
    public static void deleteUsers(
        final List<com.ableneo.liferay.portal.setup.domain.User> users,
        final String deleteMethod
    ) {
        switch (deleteMethod) {
            case "excludeListed":
                Map<String, com.ableneo.liferay.portal.setup.domain.User> usersMap = convertUserListToHashMap(users);
                List<User> allUsers = UserLocalServiceUtil.getUsers(-1, -1);
                for (User user : allUsers) {
                    if (
                        usersMap.containsKey(user.getEmailAddress()) ||
                        user.isDefaultUser() ||
                        PortalUtil.isOmniadmin(user.getUserId())
                    ) {
                        LOG.info(String.format("Skipping deletion of system user %1$s", user.getEmailAddress()));
                    } else {
                        deteleUser(user);
                    }
                }

                break;
            case "onlyListed":
                for (com.ableneo.liferay.portal.setup.domain.User user : users) {
                    try {
                        String email = user.getEmailAddress();
                        User u = UserLocalServiceUtil.getUserByEmailAddress(
                            SetupConfigurationThreadLocal.getRunInCompanyId(),
                            email
                        );
                        UserLocalServiceUtil.deleteUser(u);

                        LOG.info(String.format("Deleting User %1$s", email));
                    } catch (PortalException e) {
                        LOG.error("Unable to delete user.", e);
                    }
                }
                break;
            default:
                LOG.error(String.format("Unknown delete method : %1$s", deleteMethod));
                break;
        }
    }

    private static void deteleUser(User user) {
        try {
            UserLocalServiceUtil.deleteUser(user.getUserId());
            LOG.info(String.format("Deleted User %1$s", user.getEmailAddress()));
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Unable to delete user: %1$s", user.getEmailAddresses()), e);
        }
    }

    private static Map<String, com.ableneo.liferay.portal.setup.domain.User> convertUserListToHashMap(
        final List<com.ableneo.liferay.portal.setup.domain.User> objects
    ) {
        HashMap<String, com.ableneo.liferay.portal.setup.domain.User> map = new HashMap<>();
        for (com.ableneo.liferay.portal.setup.domain.User user : objects) {
            map.put(user.getEmailAddress(), user);
        }
        return map;
    }
}
