package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.domain.PermissionAction;
import com.ableneo.liferay.portal.setup.domain.ResourcePermissions;
import com.ableneo.liferay.portal.setup.domain.Role;
import com.ableneo.liferay.portal.setup.domain.RolePermissionType;
import com.ableneo.liferay.portal.setup.domain.RolePermissions;
import com.liferay.portal.kernel.exception.NestableException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.Resource;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ResourceLocalServiceUtil;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SetupPermissions {
    private static final String[] PERMISSION_RO = { ActionKeys.VIEW };
    private static final String[] PERMISSION_RW = { ActionKeys.VIEW, ActionKeys.UPDATE };
    private static final Log LOG = LogFactoryUtil.getLog(SetupPermissions.class);

    private SetupPermissions() {}

    public static void setupPortletPermissions(final ResourcePermissions resourcePermissions) {
        for (ResourcePermissions.Resource resource : resourcePermissions.getResource()) {
            deleteAllPortletPermissions(resource);

            Map<String, Set<String>> actionsPerRole = getActionsPerRole(resource);
            for (Map.Entry<String, Set<String>> actionsPerRoleEntry : actionsPerRole.entrySet()) {
                String roleName = actionsPerRoleEntry.getKey();
                try {
                    long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                    long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
                    final Set<String> actionStrings = actionsPerRoleEntry.getValue();
                    final String[] actionIds = actionStrings.toArray(new String[actionStrings.size()]);

                    /**
                     * Individual permission is needed even though we set
                     */
                    ResourcePermissionLocalServiceUtil.setResourcePermissions(
                        companyId,
                        resource.getResourceId(),
                        ResourceConstants.SCOPE_INDIVIDUAL,
                        String.valueOf(companyId),
                        roleId,
                        actionIds
                    );
                    ResourcePermissionLocalServiceUtil.setResourcePermissions(
                        companyId,
                        resource.getResourceId(),
                        ResourceConstants.SCOPE_COMPANY,
                        String.valueOf(companyId),
                        roleId,
                        actionIds
                    );
                    LOG.info(String.format("Set permission for role: %1$s for action ids: %2$s", roleName, actionIds));
                } catch (NestableException e) {
                    LOG.error(String.format("Could not set permission to resource :%1$s", resource.getResourceId()), e);
                }
            }
        }
    }

    /**
     * @param resource
     * @return mapping of role name to action ids for the resource
     */
    private static Map<String, Set<String>> getActionsPerRole(ResourcePermissions.Resource resource) {
        Map<String, Set<String>> result = new HashMap<>();

        for (ResourcePermissions.Resource.ActionId actionId : resource.getActionId()) {
            for (Role role : actionId.getRole()) {
                final String roleName = role.getName();
                result.computeIfAbsent(roleName, s -> new HashSet<>()).add(actionId.getName());
            }
        }

        return result;
    }

    public static void addReadRight(final String roleName, final String className, final String primaryKey) {
        addPermission(roleName, className, primaryKey, PERMISSION_RO);
    }

    public static void addReadWrightRight(final String roleName, final String className, final String primaryKey) {
        addPermission(roleName, className, primaryKey, PERMISSION_RW);
    }

    public static void removePermission(final long companyId, final String name, final String primKey)
        throws PortalException {
        ResourcePermissionLocalServiceUtil.deleteResourcePermissions(
            companyId,
            name,
            ResourceConstants.SCOPE_INDIVIDUAL,
            primKey
        );
    }

    public static void addPermission(String roleName, String name, String primaryKey, int scope, String[] permissions) {
        try {
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
            ResourcePermissionLocalServiceUtil.setResourcePermissions(
                companyId,
                name,
                scope,
                primaryKey,
                roleId,
                permissions
            );
        } catch (Exception ex) {
            LOG.error(
                "Error when adding permissions(" +
                Arrays.asList(permissions) +
                ") to role(" +
                roleName +
                "), name:(" +
                name +
                ")!",
                ex
            );
        }
    }

    public static void addPermission(
        final String roleName,
        final String className,
        final String primaryKey,
        final String[] permission
    ) {
        try {
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            com.liferay.portal.kernel.model.Role role = RoleLocalServiceUtil.getRole(companyId, roleName);
            if (role != null) {
                long roleId = role.getRoleId();
                ResourcePermissionLocalServiceUtil.setResourcePermissions(
                    companyId,
                    className,
                    ResourceConstants.SCOPE_INDIVIDUAL,
                    primaryKey,
                    roleId,
                    permission
                );
                LOG.info(" - [" + roleName + "] -> [" + primaryKey + "] ADD OK");
            } else {
                LOG.warn(" - CANNOT SET ROLE[" + roleName + "] to key[" + primaryKey + "]");
            }
        } catch (Exception ex) {
            LOG.warn(" - CANNOT SET ROLE[" + roleName + "] to key[" + primaryKey + "]");
        }
    }

    public static void removePermission(
        final String roleName,
        final String className,
        final String primaryKey,
        final String[] permission
    ) {
        try {
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            com.liferay.portal.kernel.model.Role role = RoleLocalServiceUtil.getRole(companyId, roleName);
            if (role != null) {
                long roleId = role.getRoleId();
                for (String permissionAction : permission) {
                    ResourcePermissionLocalServiceUtil.removeResourcePermission(
                        companyId,
                        className,
                        ResourceConstants.SCOPE_INDIVIDUAL,
                        primaryKey,
                        roleId,
                        permissionAction
                    );
                    LOG.info(" - [" + roleName + ":" + permissionAction + "] X [" + primaryKey + "] RM OK");
                }
            } else {
                LOG.warn(" - CANNOT REMOVE ROLE[" + roleName + "] FROM key[" + primaryKey + "]");
            }
        } catch (Exception ex) {
            LOG.warn(
                " - CANNOT REMOVE ROLE[" + roleName + "] to key[" + primaryKey + "], exception:" + ex.getMessage() + ";"
            );
        }
    }

    public static void addPermissionToPage(final Role role, final String primaryKey, final String[] actionKeys)
        throws PortalException {
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        long roleId = RoleLocalServiceUtil.getRole(companyId, role.getName()).getRoleId();
        ResourcePermissionLocalServiceUtil.setResourcePermissions(
            companyId,
            Layout.class.getName(),
            ResourceConstants.SCOPE_INDIVIDUAL,
            String.valueOf(primaryKey),
            roleId,
            actionKeys
        );
    }

    private static void deleteAllPortletPermissions(final ResourcePermissions.Resource resource) {
        try {
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            List<ResourcePermission> resourcePermissions = ResourcePermissionLocalServiceUtil.getResourcePermissions(
                companyId,
                resource.getResourceId(),
                ResourceConstants.SCOPE_COMPANY,
                String.valueOf(companyId)
            );
            for (ResourcePermission resourcePermission : resourcePermissions) {
                ResourcePermissionLocalServiceUtil.deleteResourcePermission(resourcePermission);
            }
        } catch (SystemException e) {
            LOG.error(String.format("could not delete permissions for resource :%1$s", resource.getResourceId()), e);
        }
    }

    public static void clearPagePermissions(final String primaryKey) throws PortalException {
        ResourcePermissionLocalServiceUtil.deleteResourcePermissions(
            SetupConfigurationThreadLocal.getRunInCompanyId(),
            Layout.class.getName(),
            ResourceConstants.SCOPE_INDIVIDUAL,
            String.valueOf(primaryKey)
        );
    }

    public static void updatePermission(
        final String locationHint,
        final long companyId,
        final long elementId,
        final Class clazz,
        final RolePermissions rolePermissions,
        final Map<String, List<String>> defaultPermissions
    ) {
        updatePermission(locationHint, companyId, elementId, clazz.getName(), rolePermissions, defaultPermissions);
    }

    public static void updatePermission(
        final String locationHint,
        final long companyId,
        final long elementId,
        final String className,
        final RolePermissions rolePermissions,
        final Map<String, List<String>> defaultPermissions
    ) {
        updatePermission(
            locationHint,
            companyId,
            Arrays.asList(new Long[] { elementId }),
            className,
            rolePermissions,
            defaultPermissions
        );
    }

    public static void updatePermission(
        final String locationHint,
        final long companyId,
        final List<Object> elementIdList,
        final Class clazz,
        final RolePermissions rolePermissions,
        final Map<String, List<String>> defaultPermissions
    ) {
        updatePermission(locationHint, companyId, elementIdList, clazz.getName(), rolePermissions, defaultPermissions);
    }

    public static void updatePermission(
        final String locationHint,
        final long companyId,
        final List<Object> elementIdList,
        final String className,
        final RolePermissions rolePermissions,
        final Map<String, List<String>> defaultPermissions
    ) {
        boolean useDefaultPermissions = false;
        if (rolePermissions != null) {
            List<String> actions = new ArrayList<>();
            if (rolePermissions.isClearPermissions()) {
                for (Object elementId : elementIdList) {
                    try {
                        SetupPermissions.removePermission(companyId, className, String.valueOf(elementId));
                    } catch (PortalException | SystemException e) {
                        LOG.error(
                            String.format(
                                "Permissions for %1$s could not be cleared. (elId:%2$s)",
                                locationHint,
                                elementId
                            ),
                            e
                        );
                    }
                }
            }
            //             else {
            List<RolePermissionType> rmRolePermissionList = rolePermissions.getRemoveRolePermission();
            if (rmRolePermissionList != null) {
                for (RolePermissionType rp : rmRolePermissionList) {
                    actions.clear();
                    String roleName = rp.getRoleName();
                    List<PermissionAction> roleActions = rp.getPermissionAction();
                    for (PermissionAction pa : roleActions) {
                        String actionName = pa.getActionName();
                        actions.add(actionName);
                    }
                    for (Object elementId : elementIdList) {
                        try {
                            // we need to have 'NO_XXX_RIGHT', and not MISSING XXX_RIGHT; and
                            // LR optimizes out remove-action-right when there is no permission row
                            // to substract from.. so let there be a row.
                            SetupPermissions.addPermission(
                                roleName,
                                className,
                                String.valueOf(elementId),
                                actions.toArray(new String[actions.size()])
                            );
                        } catch (SystemException e) {
                            LOG.error(
                                "Permissions for " +
                                roleName +
                                " for " +
                                locationHint +
                                " " +
                                "could not be removed, step add. ",
                                e
                            );
                        } catch (NullPointerException e) {
                            LOG.error(
                                "Permissions for " +
                                roleName +
                                " for " +
                                locationHint +
                                " " +
                                "could not be removed, step add. " +
                                "Probably role not found! ",
                                e
                            );
                        }
                        try {
                            SetupPermissions.removePermission(
                                roleName,
                                className,
                                String.valueOf(elementId),
                                actions.toArray(new String[actions.size()])
                            );
                        } catch (SystemException e) {
                            LOG.error(
                                "Permissions for " +
                                roleName +
                                " for " +
                                locationHint +
                                " " +
                                "could not be removed, step rm. ",
                                e
                            );
                        } catch (NullPointerException e) {
                            LOG.error(
                                "Permissions for " +
                                roleName +
                                " for " +
                                locationHint +
                                " " +
                                "could not be removed, step rm. " +
                                "Probably role not found! ",
                                e
                            );
                        }
                    }
                }
            }
            //            }
            List<RolePermissionType> rolePermissionList = rolePermissions.getRolePermission();
            if (rolePermissionList != null) {
                for (RolePermissionType rp : rolePermissionList) {
                    actions.clear();
                    String roleName = rp.getRoleName();
                    List<PermissionAction> roleActions = rp.getPermissionAction();
                    for (PermissionAction pa : roleActions) {
                        String actionName = pa.getActionName();
                        actions.add(actionName);
                    }
                    for (Object elementId : elementIdList) {
                        try {
                            SetupPermissions.addPermission(
                                roleName,
                                className,
                                String.valueOf(elementId),
                                actions.toArray(new String[actions.size()])
                            );
                        } catch (SystemException e) {
                            LOG.error(
                                "Permissions for " + roleName + " for " + locationHint + " " + "could not be set. ",
                                e
                            );
                        } catch (NullPointerException e) {
                            LOG.error(
                                "Permissions for " +
                                roleName +
                                " for " +
                                locationHint +
                                " " +
                                "could not be set. " +
                                "Probably role not found! ",
                                e
                            );
                        }
                    }
                }
            } else {
                useDefaultPermissions = true;
            }
        } else {
            useDefaultPermissions = true;
        }
        if (useDefaultPermissions) {
            Set<String> roles = defaultPermissions.keySet();
            List<String> actions;
            for (String roleName : roles) {
                actions = defaultPermissions.get(roleName);
                for (Object elementId : elementIdList) {
                    try {
                        SetupPermissions.addPermission(
                            roleName,
                            className,
                            String.valueOf(elementId),
                            actions.toArray(new String[actions.size()])
                        );
                    } catch (SystemException e) {
                        LOG.error(
                            "Permissions for " + roleName + " for " + locationHint + " " + "could not be defaulted. ",
                            e
                        );
                    } catch (NullPointerException e) {
                        LOG.error(
                            "Permissions for " +
                            roleName +
                            " for " +
                            locationHint +
                            " " +
                            "could not be defaulted. " +
                            "Probably role not found! ",
                            e
                        );
                    }
                }
            }
        }
    }
}
