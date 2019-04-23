package com.ableneo.liferay.portal.setup.core;

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

import java.util.*;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.domain.*;
import com.liferay.portal.kernel.exception.NestableException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;

public final class SetupPermissions {

    private static final String[] PERMISSION_RO = {ActionKeys.VIEW};
    private static final String[] PERMISSION_RW = {ActionKeys.VIEW, ActionKeys.UPDATE};
    private static final Log LOG = LogFactoryUtil.getLog(SetupPermissions.class);

    private SetupPermissions() {

    }

    public static void setupPortletPermissions(final ResourcePermissions resourcePermissions) {

        for (ResourcePermissions.Resource resource : resourcePermissions.getResource()) {

            deleteAllPortletPermissions(resource);

            Map<String, Set<String>> actionsPerRole = getActionsPerRole(resource);
            for (String roleName : actionsPerRole.keySet()) {
                try {
                    long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
                    long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
                    final Set<String> actionStrings = actionsPerRole.get(roleName);
                    final String[] actionIds = actionStrings.toArray(new String[actionStrings.size()]);

                    ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId, resource.getResourceId(),
                            ResourceConstants.SCOPE_COMPANY, String.valueOf(companyId), roleId, actionIds);
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

    public static void addReadRight(final String roleName, final String className, final String primaryKey)
            throws SystemException, PortalException {

        addPermission(roleName, className, primaryKey, PERMISSION_RO);
    }

    public static void addReadWrightRight(final String roleName, final String className, final String primaryKey)
            throws SystemException, PortalException {

        addPermission(roleName, className, primaryKey, PERMISSION_RW);
    }

    public static void removePermission(final long companyId, final String name, final String primKey)
            throws PortalException, SystemException {
        ResourcePermissionLocalServiceUtil.deleteResourcePermissions(companyId, name,
                ResourceConstants.SCOPE_INDIVIDUAL, primKey);
    }

    public static void addPermission(String roleName, String name, String primaryKey, int scope, String[] permission) {
        try {
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
            ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId, name, scope, primaryKey, roleId,
                    permission);
        } catch (Exception ex) {
            LOG.error("Error when adding role!", ex);
        }
    }

    public static void addPermission(final String roleName, final String className, final String primaryKey,
            final String[] permission) throws SystemException, PortalException {
        try {
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
            ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId, className,
                    ResourceConstants.SCOPE_INDIVIDUAL, primaryKey, roleId, permission);
        } catch (Exception ex) {
            LOG.error(ex);
        }
    }

    public static void addPermissionToPage(final Role role, final String primaryKey, final String[] actionKeys)
            throws PortalException, SystemException {

        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        long roleId = RoleLocalServiceUtil.getRole(companyId, role.getName()).getRoleId();
        ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId, Layout.class.getName(),
                ResourceConstants.SCOPE_INDIVIDUAL, String.valueOf(primaryKey), roleId, actionKeys);
    }

    private static void deleteAllPortletPermissions(final ResourcePermissions.Resource resource) {

        try {
            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            List<ResourcePermission> resourcePermissions = ResourcePermissionLocalServiceUtil.getResourcePermissions(
                    companyId, resource.getResourceId(), ResourceConstants.SCOPE_COMPANY, String.valueOf(companyId));
            for (ResourcePermission resourcePermission : resourcePermissions) {
                ResourcePermissionLocalServiceUtil.deleteResourcePermission(resourcePermission);
            }
        } catch (SystemException e) {
            LOG.error(String.format("could not delete permissions for resource :%1$s", resource.getResourceId()), e);
        }
    }

    public static void clearPagePermissions(final String primaryKey) throws PortalException, SystemException {

        ResourcePermissionLocalServiceUtil.deleteResourcePermissions(SetupConfigurationThreadLocal.getRunInCompanyId(),
                Layout.class.getName(), ResourceConstants.SCOPE_INDIVIDUAL, String.valueOf(primaryKey));
    }

    public static void updatePermission(final String locationHint, final long groupId, final long companyId,
            final long elementId, final Class clazz, final RolePermissions rolePermissions,
            final Map<String, List<String>> defaultPermissions) {

        updatePermission(locationHint, groupId, companyId, elementId, clazz.getName(), rolePermissions,
                defaultPermissions);
    }

    public static void updatePermission(final String locationHint, final long groupId, final long companyId,
            final long elementId, final String className, final RolePermissions rolePermissions,
            final Map<String, List<String>> defaultPermissions) {
        boolean useDefaultPermissions = false;
        if (rolePermissions != null) {
            if (rolePermissions.isClearPermissions()) {
                try {
                    SetupPermissions.removePermission(companyId, className, Long.toString(elementId));
                } catch (PortalException e) {
                    LOG.error(String.format("Permissions for %1$s could not be cleared. ", locationHint), e);
                } catch (SystemException e) {
                    LOG.error(String.format("Permissions for %1$s could not be cleared. ", locationHint), e);
                }
            }
            List<String> actions = new ArrayList<String>();
            List<RolePermission> rolePermissionList = rolePermissions.getRolePermission();
            if (rolePermissionList != null) {
                for (RolePermission rp : rolePermissionList) {
                    actions.clear();
                    String roleName = rp.getRoleName();
                    List<PermissionAction> roleActions = rp.getPermissionAction();
                    for (PermissionAction pa : roleActions) {
                        String actionName = pa.getActionName();
                        actions.add(actionName);
                    }
                    try {
                        SetupPermissions.addPermission(roleName, className, Long.toString(elementId),
                                actions.toArray(new String[actions.size()]));
                    } catch (SystemException e) {
                        LOG.error("Permissions for " + roleName + " for " + locationHint + " " + "could not be set. ",
                                e);
                    } catch (PortalException e) {
                        LOG.error("Permissions for " + roleName + " for " + locationHint + " " + "could not be set. ",
                                e);
                    } catch (NullPointerException e) {
                        LOG.error("Permissions for " + roleName + " for " + locationHint + " " + "could not be set. "
                                + "Probably role not found! ", e);
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
            for (String r : roles) {
                actions = defaultPermissions.get(r);
                try {
                    SetupPermissions.addPermission(r, className, Long.toString(elementId),
                            actions.toArray(new String[actions.size()]));
                } catch (SystemException e) {
                    LOG.error("Permissions for " + r + " for " + locationHint + " could not be " + "set. ", e);
                } catch (PortalException e) {
                    LOG.error("Permissions for " + r + " for " + locationHint + " could not be " + "set. ", e);
                }
            }
        }
    }

}
