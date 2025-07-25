package com.ableneo.liferay.portal.setup;

import com.liferay.petra.lang.CentralizedThreadLocal;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.LocaleThreadLocal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SetupConfigurationThreadLocal {

    private static final Logger LOG = LoggerFactory.getLogger(SetupConfigurationThreadLocal.class);

    private static final ThreadLocal<Long> _runAsUserId = new CentralizedThreadLocal<>(
        SetupConfigurationThreadLocal.class + "._runAsUserId",
        () -> {
            try {
                return UserLocalServiceUtil.getDefaultUserId(PortalUtil.getDefaultCompanyId());
            } catch (PortalException e) {
                LOG.error("Failed to get default user id", e);
            }
            return null;
        }
    );

    private static final ThreadLocal<Long> _runInCompanyId = new CentralizedThreadLocal<>(
        SetupConfigurationThreadLocal.class + "._runInCompanyId",
        PortalUtil::getDefaultCompanyId
    );

    private static final ThreadLocal<Long> _runInGroupId = new CentralizedThreadLocal<>(
        SetupConfigurationThreadLocal.class + "._runInGroupId",
        () -> {
            try {
                return GroupLocalServiceUtil.getGroup(
                    PortalUtil.getDefaultCompanyId(),
                    GroupConstants.GUEST
                ).getGroupId();
            } catch (PortalException e) {
                LOG.error("Failed to get Guest group id for default company", e);
            }
            return null;
        }
    );

    private static final ThreadLocal<Bundle> _callerBundle = new CentralizedThreadLocal<>(
        SetupConfigurationThreadLocal.class + "._callerBundle",
        null
    );

    private SetupConfigurationThreadLocal() {}

    public static Long getRunAsUserId() {
        return _runAsUserId.get();
    }

    public static void setRunAsUserId(Long runAsUserId) {
        _runAsUserId.set(runAsUserId);
    }

    public static Long getRunInCompanyId() {
        return _runInCompanyId.get();
    }

    public static void setRunInCompanyId(Long runInCompanyId) {
        _runInCompanyId.set(runInCompanyId);
    }

    public static Long getRunInGroupId() {
        return _runInGroupId.get();
    }

    public static void setRunInGroupId(Long runInGroupId) {
        _runInGroupId.set(runInGroupId);
    }

    public static Bundle getCallerBundle() {
        return _callerBundle.get();
    }

    public static void setCallerBundle(Bundle callerBundle) {
        _callerBundle.set(callerBundle);
    }

    public static void cleanUp(
        String originalPrincipalName,
        PermissionChecker originalPermissionChecker,
        Locale originalScopeGroupLocale
    ) {
        _runInCompanyId.remove();
        _runAsUserId.remove();
        _runInGroupId.remove();
        _callerBundle.remove();

        PrincipalThreadLocal.setName(originalPrincipalName);
        PermissionThreadLocal.setPermissionChecker(originalPermissionChecker);
        LocaleThreadLocal.setSiteDefaultLocale(originalScopeGroupLocale);
    }

    /**
     * Sets up mandatory data used by different setup tools: userId, companyId. Prepares needed Liferay security
     * configuration based on userId that it retrieves from runAsUserEmail configuration. This secured that all
     * Liferay service methods will authorize data modification method calls.
     *
     * @param runAsUserEmail email of a user that will be used to setup configured data
     * @param companyId id of a company that will host data to be set up
     * @param group group model of a group that is a target for a particular setup data configuration
     * @throws PortalException user was not found, breaks the setup execution, we presume that if user email was
     *                         provided it is important to set up data as the user e.g. for easier cleanup
     */
    static void configureThreadLocalContent(String runAsUserEmail, long companyId, Group group) throws PortalException {
        Objects.requireNonNull(group);
        configureGroupExecutionContext(group);
        configureThreadLocalContent(runAsUserEmail, companyId, (Bundle) null);
    }

    public static void configureGroupExecutionContext(Group group) {
        Objects.requireNonNull(group);
        setRunInGroupId(group.getGroupId());
        LocaleThreadLocal.setDefaultLocale(Locale.forLanguageTag(group.getDefaultLanguageId().replace('_', '-')));
    }

    /**
     * Sets up mandatory data used by different setup tools: userId, companyId. Prepares needed Liferay security
     * configuration based on userId that it retrieves from runAsUserEmail configuration. This secured that all
     * Liferay service methods will authorize data modification method calls.
     *
     * @param runAsUserEmail email of a user that will be used to setup configured data
     * @param companyId id of a company that will host data to be set up
     * @throws PortalException user was not found, breaks the setup execution, we presume that if user email was
     *                         provided it is important to set up data as the user e.g. for easier cleanup
     */
    static void configureThreadLocalContent(String runAsUserEmail, long companyId, Bundle callerBundle)
        throws PortalException {
        if (Validator.isBlank(runAsUserEmail)) {
            setRunInCompanyId(companyId);
            setCallerBundle(callerBundle);
            SetupConfigurationThreadLocal.setRandomAdminPermissionCheckerForThread();
            LOG.info("Using default administrator.");
        } else {
            User user = UserLocalServiceUtil.getUserByEmailAddress(companyId, runAsUserEmail);
            setRunAsUserId(user.getUserId());
            setRunInCompanyId(companyId);
            setCallerBundle(callerBundle);
            PrincipalThreadLocal.setName(user.getUserId());
            PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(user);
            PermissionThreadLocal.setPermissionChecker(permissionChecker);

            LOG.info("Execute setup module as user {}", runAsUserEmail);
        }
    }

    /**
     * Initializes permission checker for Liferay Admin. Used to grant access to
     * custom fields.
     */
    private static void setRandomAdminPermissionCheckerForThread() {
        User adminUser = getRandomAdminUser();
        SetupConfigurationThreadLocal.setRunAsUserId(Objects.requireNonNull(adminUser).getUserId());
        PrincipalThreadLocal.setName(adminUser.getUserId());
        PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(adminUser);
        PermissionThreadLocal.setPermissionChecker(permissionChecker);
    }

    /**
     * Returns Liferay user, that has Administrator role assigned.
     *
     * @return Liferay {@link com.ableneo.liferay.portal.setup.domain.User}
     * @throws IllegalStateException if no user is found
     */
    private static User getRandomAdminUser() {
        final String administratorRoleName = RoleConstants.ADMINISTRATOR;
        try {
            final Long runInCompanyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            Role adminRole = RoleLocalServiceUtil.getRole(runInCompanyId, administratorRoleName);
            List<User> adminUsers = UserLocalServiceUtil.getRoleUsers(adminRole.getRoleId());

            if (adminUsers == null || adminUsers.isEmpty()) {
                throw new IllegalStateException(
                    "No user with " + administratorRoleName + " role found for company: " + runInCompanyId
                );
            }
            return adminUsers.get(0);
        } catch (PortalException | SystemException e) {
            throw new IllegalStateException("Cannot obtain Liferay role for role name: " + administratorRoleName, e);
        }
    }
}
