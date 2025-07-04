package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.CustomFieldSettingUtil;
import com.ableneo.liferay.portal.setup.domain.CustomFieldSetting;
import com.ableneo.liferay.portal.setup.domain.Role;
import com.ableneo.liferay.portal.setup.domain.UserAsMember;
import com.ableneo.liferay.portal.setup.domain.UserGroup;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupUserGroups {

    private static final Logger LOG = LoggerFactory.getLogger(SetupUserGroups.class);

    private SetupUserGroups() {}

    public static void setupUserGroups(final List<UserGroup> userGroups) {
        final long userId = SetupConfigurationThreadLocal.getRunAsUserId();

        for (UserGroup userGroup : userGroups) {
            com.liferay.portal.kernel.model.UserGroup liferayUserGroup = null;
            long liferayUserGroupId = -1;
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            try {
                liferayUserGroup = UserGroupLocalServiceUtil.getUserGroup(companyId, userGroup.getName());
                liferayUserGroupId = liferayUserGroup.getUserGroupId();
            } catch (PortalException e) {
                LOG.info(
                    String.format("UserGroup does not exists, creating new one for name: %1$s", userGroup.getName())
                );
            }
            if (liferayUserGroupId == -1) {
                try {
                    liferayUserGroup = UserGroupLocalServiceUtil.addUserGroup(
                        userId,
                        companyId,
                        userGroup.getName(),
                        userGroup.getDescription(),
                        new ServiceContext()
                    );
                } catch (PortalException e) {
                    LOG.error(String.format("Can not create UserGroup with name: %1$s", userGroup.getName()), e);
                    continue;
                }
            }

            if (userGroup.getCustomFieldSetting() != null && !userGroup.getCustomFieldSetting().isEmpty()) {
                setCustomFields(liferayUserGroup, userGroup.getCustomFieldSetting(), userGroup);
            }

            if (!userGroup.getRole().isEmpty() && liferayUserGroup != null) {
                LOG.info("Setting Roles for UserGroup.");
                addRolesToUserGroup(userGroup, liferayUserGroup);
            }

            if (!userGroup.getUserAsMember().isEmpty() && liferayUserGroup != null) {
                LOG.info("Setting User Members.");
                addUsersToUserGroup(userGroup.getUserAsMember(), liferayUserGroup);
            }
        }
    }

    private static void addUsersToUserGroup(
        List<UserAsMember> usersAsMember,
        com.liferay.portal.kernel.model.UserGroup liferayUserGroup
    ) {
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        for (UserAsMember member : usersAsMember) {
            User user = UserLocalServiceUtil.fetchUserByScreenName(companyId, member.getScreenName());
            if (Objects.isNull(user)) {
                LOG.error(
                    String.format(
                        "Can not set user %1$s as member of UserGroup. User does not exists...",
                        member.getScreenName()
                    )
                );
                continue;
            }

            try {
                UserGroupLocalServiceUtil.addUserUserGroup(user.getUserId(), liferayUserGroup.getUserGroupId());
            } catch (PortalException e) {
                LOG.warn(
                    "Failed to setup user {} as member of userGroup {}",
                    user.getScreenName(),
                    liferayUserGroup.getName(),
                    e
                );
            }
            LOG.info(
                String.format(
                    "User %1$s successfully added as a member to UserGroup %2$s",
                    user.getScreenName(),
                    liferayUserGroup.getName()
                )
            );
        }
    }

    private static void setCustomFields(
        final com.liferay.portal.kernel.model.UserGroup liferayUserGroup,
        final List<CustomFieldSetting> customFieldSettings,
        final UserGroup userGroup
    ) {
        if (liferayUserGroup == null) {
            return;
        }

        Class clazz = com.liferay.portal.kernel.model.UserGroup.class;

        for (CustomFieldSetting cfs : customFieldSettings) {
            String resolverHint =
                "Custom value for userGroup " +
                userGroup.getName() +
                ", " +
                " Key " +
                cfs.getKey() +
                ", value " +
                cfs.getValue();
            long company = SetupConfigurationThreadLocal.getRunInCompanyId();
            CustomFieldSettingUtil.setExpandoValue(
                resolverHint,
                liferayUserGroup.getUserGroupId(),
                company,
                clazz,
                liferayUserGroup.getUserGroupId(),
                cfs.getKey(),
                cfs.getValue()
            );
        }
    }

    private static void addRolesToUserGroup(
        final UserGroup userGroup,
        final com.liferay.portal.kernel.model.UserGroup liferayUserGroup
    ) {
        try {
            for (Role role : userGroup.getRole()) {
                long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                com.liferay.portal.kernel.model.Role liferayRole = RoleLocalServiceUtil.getRole(
                    companyId,
                    role.getName()
                );
                String roleType = role.getType();
                switch (roleType) {
                    case RoleConstants.TYPE_REGULAR_LABEL:
                    case "portal":
                        GroupLocalServiceUtil.addRoleGroup(liferayRole.getRoleId(), liferayUserGroup.getGroupId());
                        LOG.info(
                            String.format(
                                "Adding role %1$s to userGroup %2$s",
                                liferayRole.getDescriptiveName(),
                                liferayUserGroup.getName()
                            )
                        );
                        break;
                    case "site":
                    case "organization":
                        LOG.error(
                            "Adding site or organization roles to UserGroup is not supported. Bind userGroups to Site Roles within the Site elemennt."
                        );
                        break;
                    default:
                        LOG.error(String.format("unknown role type %1$s", roleType));
                        break;
                }
            }
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Error in adding roles to userGroup %1$s", userGroup.getName()), e);
        }
    }
}
