package com.ableneo.liferay.portal.setup.core;

/*
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ableneo.liferay.portal.setup.domain.PermissionAction;
import com.ableneo.liferay.portal.setup.domain.PortletPermissions;
import com.ableneo.liferay.portal.setup.domain.Role;
import com.ableneo.liferay.portal.setup.domain.RolePermission;
import com.ableneo.liferay.portal.setup.domain.RolePermissions;
import com.liferay.portal.kernel.exception.NestableException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;

public final class SetupPermissions {

    private static final String[] PERMISSION_RO = {ActionKeys.VIEW};
    private static final String[] PERMISSION_RW = {ActionKeys.VIEW, ActionKeys.UPDATE};
    private static final Log LOG = LogFactoryUtil.getLog(SetupPermissions.class);
    private final SetupContext setupContext;


    public SetupPermissions(SetupContext setupContext) {
        this.setupContext = setupContext;
    }

    public void setupPortletPermissions(final PortletPermissions portletPermissions) {

        for (PortletPermissions.Portlet portlet : portletPermissions.getPortlet()) {

            deleteAllPortletPermissions(portlet);

            Map<String, Set<String>> actionsPerRole = getActionsPerRole(portlet);
            for (Map.Entry<String, Set<String>> actionsEntry : actionsPerRole.entrySet()) {
                try {
                    long roleId = RoleLocalServiceUtil.getRole(setupContext.getRunInCompanyId(), actionsEntry.getKey())
                            .getRoleId();
                    final String[] actionIds =
                            actionsEntry.getValue().toArray(new String[actionsEntry.getValue().size()]);

                    ResourcePermissionLocalServiceUtil.setResourcePermissions(setupContext.getRunInCompanyId(),
                            portlet.getPortletId(), ResourceConstants.SCOPE_COMPANY,
                            String.valueOf(setupContext.getRunInCompanyId()), roleId, actionIds);
                    LOG.info(String.format("Set permission for role: %1$s for action ids: %2$s", actionsEntry.getKey(),
                            Arrays.toString(actionIds)));
                } catch (NestableException e) {
                    LOG.error(String.format("Could not set permission to portlet : %1$s", portlet.getPortletId()), e);
                }
            }
        }
    }

    private void deleteAllPortletPermissions(final PortletPermissions.Portlet portlet) {

        try {
            List<ResourcePermission> resourcePermissions = ResourcePermissionLocalServiceUtil.getResourcePermissions(
                    setupContext.getRunInCompanyId(), portlet.getPortletId(), ResourceConstants.SCOPE_COMPANY,
                    String.valueOf(setupContext.getRunInCompanyId()));
            for (ResourcePermission resourcePermission : resourcePermissions) {
                ResourcePermissionLocalServiceUtil.deleteResourcePermission(resourcePermission);
            }
        } catch (SystemException e) {
            LOG.error("could not delete permissions for portlet :" + portlet.getPortletId(), e);
        }
    }

    /**
     * @param portlet
     * @return mapping of role name to action ids for the portlet
     */
    private static Map<String, Set<String>> getActionsPerRole(PortletPermissions.Portlet portlet) {
        Map<String, Set<String>> result = new HashMap<>();

        for (PortletPermissions.Portlet.ActionId actionId : portlet.getActionId()) {
            for (Role role : actionId.getRole()) {
                final String roleName = role.getName();
                Set<String> actions = result.get(roleName);
                if (actions == null) {
                    actions = new HashSet<>();
                    result.put(roleName, actions);
                }
                actions.add(actionId.getName());
            }
        }

        return result;
    }

    public void addReadRight(final String roleName, final String className, final String primaryKey)
            throws SystemException, PortalException {

        addPermission(roleName, className, primaryKey, PERMISSION_RO);
    }

    public void addPermission(final String roleName, final String className, final String primaryKey,
            final String[] permission) throws SystemException, PortalException {
        try {
            long roleId = RoleLocalServiceUtil.getRole(setupContext.getRunInCompanyId(), roleName).getRoleId();
            ResourcePermissionLocalServiceUtil.setResourcePermissions(setupContext.getRunInCompanyId(), className,
                    ResourceConstants.SCOPE_INDIVIDUAL, primaryKey, roleId, permission);
        } catch (Exception ex) {
            LOG.error(ex);
        }
    }

    public void addReadWrightRight(final String roleName, final String className, final String primaryKey)
            throws SystemException, PortalException {

        addPermission(roleName, className, primaryKey, PERMISSION_RW);
    }

    public void addPermission(String roleName, String name, String primaryKey, int scope, String[] permission)
            throws SystemException, PortalException {
        try {
            long roleId = RoleLocalServiceUtil.getRole(setupContext.getRunInCompanyId(), roleName).getRoleId();
            ResourcePermissionLocalServiceUtil.setResourcePermissions(setupContext.getRunInCompanyId(), name, scope,
                    primaryKey, roleId, permission);
        } catch (Exception ex) {
            LOG.error("Error when adding role!", ex);
        }
    }

    public void updatePermission(final String locationHint, final long elementId,
            final Class clazz, final RolePermissions rolePermissions,
            final Map<String, List<String>> defaultPermissions) throws SystemException {
        boolean useDefaultPermissions;
        if (rolePermissions != null) {
            if (rolePermissions.isClearPermissions()) {
                clearPermissions(locationHint, elementId, clazz);
            }
            useDefaultPermissions = processRolePermissions(locationHint, elementId, clazz, rolePermissions);
        } else {
            useDefaultPermissions = true;
        }
        if (useDefaultPermissions) {
            setupDefaultPermissions(locationHint, elementId, clazz, defaultPermissions);
        }
    }

    private void clearPermissions(String locationHint, long elementId, Class clazz) {
        try {
            this.removeAllPermissions(clazz.getName(), Long.toString(elementId));
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Permissions for %1$s could not be cleared. ", locationHint), e);
        }
    }

    private boolean processRolePermissions(String locationHint, long elementId, Class clazz,
            RolePermissions rolePermissions) throws SystemException {
        List<RolePermission> rolePermissionList = rolePermissions.getRolePermission();
        Set<String> clearPermissionsForRoles = new HashSet<>();
        if (rolePermissionList != null) {
            for (RolePermission rolePermission : rolePermissionList) {
                if (rolePermission.isClearPermissions()) {
                    clearPermissionsForRoles.add(rolePermission.getRoleName());
                }
            }
            removeAllPermissionsPerRole(locationHint, elementId, clazz, clearPermissionsForRoles);
            for (RolePermission rolePermission : rolePermissionList) {
                processRolePermission(locationHint, elementId, clazz, rolePermission);
            }
        } else {
            return true;
        }
        return false;
    }

    private void setupDefaultPermissions(String locationHint, long elementId, Class clazz,
            Map<String, List<String>> defaultPermissions) {
        for (Map.Entry<String, List<String>> permissionsPerRoleEntry : defaultPermissions.entrySet()) {
            final String role = permissionsPerRoleEntry.getKey();
            try {
                addPermission(role, clazz.getName(), Long.toString(elementId), permissionsPerRoleEntry
                        .getValue().toArray(new String[permissionsPerRoleEntry.getValue().size()]));
            } catch (SystemException | PortalException e) {
                LOG.error(String.format("Permissions for %1$s for %2$s could not be set. ", role, locationHint), e);
            }
        }
    }

    public void removeAllPermissions(final String name, final String primKey)
            throws PortalException, SystemException {
        ResourcePermissionLocalServiceUtil.deleteResourcePermissions(setupContext.getRunInCompanyId(), name,
                ResourceConstants.SCOPE_INDIVIDUAL, primKey);
    }

    private void removeAllPermissionsPerRole(String locationHint, long elementId, Class clazz,
            Set roleNames) throws SystemException {
        final Map<String, List<String>> resourceRolePermissions = RoleLocalServiceUtil.getResourceRoles(setupContext.getRunInCompanyId(),
                clazz.getName(), ResourceConstants.SCOPE_INDIVIDUAL, Long.toString(elementId));
        for (Map.Entry<String, List<String>> resourceRolePermission : resourceRolePermissions.entrySet()) {
            if (roleNames.contains(resourceRolePermission.getKey())) {
                long roleId = 0;
                try {
                    final com.liferay.portal.model.Role role =
                            RoleLocalServiceUtil.getRole(setupContext.getRunInCompanyId(), resourceRolePermission.getKey());
                    roleId = role.getRoleId();
                    for (String actionId : resourceRolePermission.getValue()) {
                        ResourcePermissionLocalServiceUtil.removeResourcePermission(setupContext.getRunInCompanyId(), clazz.getName(),
                                ResourceConstants.SCOPE_INDIVIDUAL, Long.toString(elementId), roleId, actionId);
                    }
                } catch (PortalException e) {
                    if (roleId == 0) {
                        LOG.warn(String.format(
                                "%3$s,\nCouldn't find role with role name %1$s in company with id %2$s, continuing with other roles..",
                                resourceRolePermission.getKey(), setupContext.getRunInCompanyId(), locationHint));
                    } else {
                        LOG.error(String.format(
                                "%6$s,\nUnexpected error trying to remove permissions (%5$s) in\n companyid %1$s\n for %2$s\n with id %3$s\n and role id %4$s\n continuing with other permissions",
                                setupContext.getRunInCompanyId(), clazz.getName(), elementId, roleId, resourceRolePermission.getValue(),
                                locationHint), e);
                    }
                    continue;
                }
            }
        }
    }

    private void processRolePermission(String locationHint, long elementId, Class clazz,
            RolePermission rolePermission) {
        String roleName = rolePermission.getRoleName();
        List<PermissionAction> roleActions = rolePermission.getPermissionAction();
        String[] actions = new String[roleActions.size()];
        for (int i = 0; i < roleActions.size(); i++) {
            String actionName = roleActions.get(i).getActionName();
            actions[i] = actionName;
        }
        try {
            addPermission(roleName, clazz.getName(), Long.toString(elementId), actions);
        } catch (SystemException | PortalException e) {
            LOG.error("Permissions for " + roleName + " for " + locationHint + " " + "could not be set. ", e);
        } catch (NullPointerException e) {
            LOG.error("Permissions for " + roleName + " for " + locationHint + " " + "could not be set. "
                    + "Probably role not found! ", e);
        }
    }

}
