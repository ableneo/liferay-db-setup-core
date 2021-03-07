package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.ResolverUtil;
import com.ableneo.liferay.portal.setup.domain.DefinePermission;
import com.ableneo.liferay.portal.setup.domain.DefinePermissions;
import com.ableneo.liferay.portal.setup.domain.PermissionAction;
import com.liferay.portal.kernel.dao.orm.ObjectNotFoundException;
import com.liferay.portal.kernel.exception.NoSuchRoleException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.RequiredRoleException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import java.util.*;

public final class SetupRoles {
    private static final Log LOG = LogFactoryUtil.getLog(SetupRoles.class);

    public static final String SCOPE_INDIVIDUAL = "individual";
    public static final String SCOPE_SITE = "site";
    public static final String SCOPE_SITE_TEMPLATE = "site template";
    public static final String SCOPE_PORTAL = "portal";

    private SetupRoles() {}

    public static void setupRoles(final List<com.ableneo.liferay.portal.setup.domain.Role> roles) {
        for (com.ableneo.liferay.portal.setup.domain.Role role : roles) {
            try {
                long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                RoleLocalServiceUtil.getRole(companyId, role.getName());
                LOG.info(String.format("Setup: Role %1$s already exist, not creating...", role.getName()));
            } catch (NoSuchRoleException | ObjectNotFoundException e) {
                addRole(role);
            } catch (SystemException | PortalException e) {
                LOG.error("error while setting up roles", e);
            }
            addRolePermissions(role);
        }
    }

    private static void addRole(final com.ableneo.liferay.portal.setup.domain.Role role) {
        Map<Locale, String> localeTitleMap = new HashMap<>();
        localeTitleMap.put(Locale.ENGLISH, role.getName());

        try {
            int roleType = RoleConstants.TYPE_REGULAR;
            if (role.getType() != null) {
                if (role.getType().equals("site")) {
                    roleType = RoleConstants.TYPE_SITE;
                } else if (role.getType().equals("organization")) {
                    roleType = RoleConstants.TYPE_ORGANIZATION;
                }
            }

            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            long defaultUserId = UserLocalServiceUtil.getDefaultUserId(companyId);
            RoleLocalServiceUtil.addRole(
                defaultUserId,
                null,
                0,
                role.getName(),
                localeTitleMap,
                null,
                roleType,
                null,
                null
            );

            LOG.info(String.format("Setup: Role %1$s does not exist, adding...", role.getName()));
        } catch (PortalException | SystemException e) {
            LOG.error("error while adding up roles", e);
        }
    }

    public static void deleteRoles(
        final List<com.ableneo.liferay.portal.setup.domain.Role> roles,
        final String deleteMethod
    ) {
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        switch (deleteMethod) {
            case "excludeListed":
                Map<String, com.ableneo.liferay.portal.setup.domain.Role> toBeDeletedRoles = convertRoleListToHashMap(
                    roles
                );
                try {
                    for (Role role : RoleLocalServiceUtil.getRoles(-1, -1)) {
                        String name = role.getName();
                        if (!toBeDeletedRoles.containsKey(name)) {
                            try {
                                RoleLocalServiceUtil.deleteRole(RoleLocalServiceUtil.getRole(companyId, name));
                                LOG.info(String.format("Deleting Role %1$s", name));
                            } catch (Exception e) {
                                LOG.info(String.format("Skipping deletion fo system role %1$s", name));
                            }
                        }
                    }
                } catch (SystemException e) {
                    LOG.error("problem with deleting roles", e);
                }
                break;
            case "onlyListed":
                for (com.ableneo.liferay.portal.setup.domain.Role role : roles) {
                    String name = role.getName();
                    try {
                        RoleLocalServiceUtil.deleteRole(RoleLocalServiceUtil.getRole(companyId, name));
                        LOG.info(String.format("Deleting Role %1$s", name));
                    } catch (RequiredRoleException e) {
                        LOG.info(String.format("Skipping deletion fo system role %1$s", name));
                    } catch (PortalException | SystemException e) {
                        LOG.error("Unable to delete role.", e);
                    }
                }
                break;
            default:
                LOG.error(String.format("Unknown delete method : %1$s", deleteMethod));
                break;
        }
    }

    private static void addRolePermissions(com.ableneo.liferay.portal.setup.domain.Role role) {
        if (role.getDefinePermissions() != null) {
            String siteName = role.getSite();
            if (siteName != null && !siteName.equals("")) {
                LOG.warn(
                    String.format(
                        "Note, refering a site inside a role definition makes no sense and will be ignored! This is:%1$s; When doing so, it is necessary to refer a site!",
                        "attribute is intended to be used for refering assigning a site role to an Liferay object, such as a user!",
                        siteName
                    )
                );
            }
            DefinePermissions permissions = role.getDefinePermissions();
            if (permissions.getDefinePermission() != null && !permissions.getDefinePermission().isEmpty()) {
                for (DefinePermission permission : permissions.getDefinePermission()) {
                    String permissionName = permission.getDefinePermissionName();
                    String resourcePrimKey = "0";

                    long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                    if (permission.getElementPrimaryKey() != null) {
                        long groupId = SetupConfigurationThreadLocal.getRunInGroupId();
                        resourcePrimKey =
                            ResolverUtil.lookupAll(
                                groupId,
                                companyId,
                                permission.getElementPrimaryKey(),
                                String.format("Role %1$s permission name %2$s", role.getName(), permissionName)
                            );
                    }
                    String type = role.getType();
                    int scope = ResourceConstants.SCOPE_COMPANY;
                    if (type != null && type.equalsIgnoreCase(SCOPE_PORTAL)) {
                        scope = ResourceConstants.SCOPE_COMPANY;
                    }
                    if (type != null && type.equalsIgnoreCase(SCOPE_SITE)) {
                        scope = ResourceConstants.SCOPE_GROUP_TEMPLATE;
                    }

                    if (type != null && type.equalsIgnoreCase("organization")) {
                        scope = ResourceConstants.SCOPE_GROUP_TEMPLATE;
                    }
                    if (scope == ResourceConstants.SCOPE_COMPANY && !resourcePrimKey.equals("0")) {
                        // if we have a regular role which is set for a particular site, set the scope to group
                        scope = ResourceConstants.SCOPE_GROUP;
                    }
                    if (scope == ResourceConstants.SCOPE_COMPANY && resourcePrimKey.equals("0")) {
                        // if we have a regular role which does not have any resource key set, set the company id as
                        // resource key
                        resourcePrimKey = Long.toString(companyId);
                    }

                    // if defined override the define permission scope
                    if (permission.getScope() != null && !permission.getScope().equals("")) {
                        if (permission.getScope().equalsIgnoreCase(SCOPE_PORTAL)) {
                            scope = ResourceConstants.SCOPE_COMPANY;
                        } else if (permission.getScope().equalsIgnoreCase(SCOPE_SITE_TEMPLATE)) {
                            scope = ResourceConstants.SCOPE_GROUP_TEMPLATE;
                        } else if (permission.getScope().equalsIgnoreCase(SCOPE_SITE)) {
                            scope = ResourceConstants.SCOPE_GROUP;
                        } else if (permission.getScope().equalsIgnoreCase(SCOPE_INDIVIDUAL)) {
                            scope = ResourceConstants.SCOPE_INDIVIDUAL;
                        }
                    }

                    if (permission.getPermissionAction() != null && !permission.getPermissionAction().isEmpty()) {
                        ArrayList<String> listOfActions = new ArrayList<>();
                        for (PermissionAction pa : permission.getPermissionAction()) {
                            String actionname = pa.getActionName();
                            listOfActions.add(actionname);
                        }
                        String[] loa = new String[listOfActions.size()];
                        loa = listOfActions.toArray(loa);
                        try {
                            SetupPermissions.addPermission(role.getName(), permissionName, resourcePrimKey, scope, loa);
                        } catch (SystemException e) {
                            LOG.error(
                                String.format(
                                    "Error when defining permission %1$s for role %2$s",
                                    permissionName,
                                    role.getName()
                                ),
                                e
                            );
                        }
                    }
                }
            }
        }
    }

    private static Map<String, com.ableneo.liferay.portal.setup.domain.Role> convertRoleListToHashMap(
        final List<com.ableneo.liferay.portal.setup.domain.Role> objects
    ) {
        HashMap<String, com.ableneo.liferay.portal.setup.domain.Role> map = new HashMap<>();
        for (com.ableneo.liferay.portal.setup.domain.Role role : objects) {
            map.put(role.getName(), role);
        }
        return map;
    }
}
