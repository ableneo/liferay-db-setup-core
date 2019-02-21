package com.ableneo.liferay.portal.setup.core;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ableneo.liferay.portal.setup.core.util.CustomFieldSettingUtil;
import com.ableneo.liferay.portal.setup.domain.CustomFieldSetting;
import com.ableneo.liferay.portal.setup.domain.Membership;
import com.ableneo.liferay.portal.setup.domain.Role;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.ableneo.liferay.portal.setup.domain.UserAsMember;
import com.ableneo.liferay.portal.setup.domain.UsergroupAsMember;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.security.auth.CompanyThreadLocal;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserGroupGroupRoleLocalServiceUtil;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserGroupRoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;

/**
 * Created by gustavnovotny on 28.08.17.
 */
public class SetupSites {

    private static final Log LOG = LogFactoryUtil.getLog(SetupSites.class);
    private final String DEFAULT_GROUP_NAME;
    private final SetupContext setupContext;

    public SetupSites(SetupContext setupContext) {
        this.setupContext = setupContext;
        DEFAULT_GROUP_NAME = setupContext.getDefaultGroupName();
    }



    public boolean setupSites(final List<com.ableneo.liferay.portal.setup.domain.Site> groups,
            final Group parentGroup) {

        boolean totalSuccess = true;
        CompanyThreadLocal.setCompanyId(getRunInCompanyId());
        for (com.ableneo.liferay.portal.setup.domain.Site site : groups) {
            try {
                Group liferayGroup = null;
                long groupId = -1;
                if (site.isDefault()) {
                    liferayGroup = GroupLocalServiceUtil.getGroup(getRunInCompanyId(), DEFAULT_GROUP_NAME);
                    LOG.info("Setup: default site. Group ID: " + groupId);
                } else if (site.getName() == null) {
                    liferayGroup = GroupLocalServiceUtil.getCompanyGroup(getRunInCompanyId());
                    LOG.info("Setup: global site. Group ID: " + groupId);
                } else {
                    try {
                        liferayGroup = GroupLocalServiceUtil.getGroup(getRunInCompanyId(), site.getName());
                        LOG.info("Setup: Site " + site.getName() + " already exists in system, not creating...");

                    } catch (PortalException | SystemException e) {
                        LOG.debug("Site does not exist.", e);
                    }
                }
                long defaultUserId = UserLocalServiceUtil.getDefaultUserId(getRunInCompanyId());
                ServiceContext serviceContext = new ServiceContext();

                if (liferayGroup == null) {
                    LOG.info("Setup: Group (Site) " + site.getName() + " does not exist in system, creating...");

                    liferayGroup = GroupLocalServiceUtil.addGroup(defaultUserId, GroupConstants.DEFAULT_PARENT_GROUP_ID,
                            Group.class.getName(), 0, 0, site.getName(), null, GroupConstants.TYPE_SITE_RESTRICTED,
                            true, GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION, site.getSiteFriendlyUrl(), true, true,
                            serviceContext);
                    LOG.info("New Organization created. Group ID: " + groupId);
                } else {
                    LOG.info("Setup: Updating " + site.getName());
                    GroupLocalServiceUtil.updateFriendlyURL(liferayGroup.getGroupId(), site.getSiteFriendlyUrl());
                }
                groupId = liferayGroup.getGroupId();

                if (parentGroup != null && liferayGroup != null && site.isMaintainSiteHierarchy()) {
                    liferayGroup.setParentGroupId(parentGroup.getGroupId());
                    GroupLocalServiceUtil.updateGroup(liferayGroup);
                } else if (liferayGroup != null && site.isMaintainSiteHierarchy()) {
                    liferayGroup.setParentGroupId(0);
                    GroupLocalServiceUtil.updateGroup(liferayGroup);
                }

                LOG.info("Setting site content...");

                (new SetupArticles(setupContext.clone())).setupSiteStructuresAndTemplates(site);
                LOG.info("Site DDM structures and templates setting finished.");

                (new SetupDocumentFolders(setupContext.clone())).setupDocumentFolders(site);
                LOG.info("Document Folders setting finished.");

                (new SetupDocuments(setupContext.clone())).setupSiteDocuments(site);
                LOG.info("Documents setting finished.");

                (new SetupPages(setupContext.clone())).setupSitePages(site);
                LOG.info("Site Pages setting finished.");

                (new SetupWebFolders(setupContext.clone())).setupWebFolders(site);
                LOG.info("Web folders setting finished.");

                (new SetupCategorization(setupContext.clone())).setupVocabularies(site);
                LOG.info("Site Categories setting finished.");

                (new SetupArticles(setupContext.clone())).setupSiteArticles(site);
                LOG.info("Site Articles setting finished.");

                setCustomFields(site);
                LOG.info("Site custom fields set up.");

                // Users and Groups should be referenced to live Group
                setMembership(site.getMembership(), getRunInCompanyId(), liferayGroup.getGroupId());

                List<com.ableneo.liferay.portal.setup.domain.Site> sites = site.getSite();
                totalSuccess = totalSuccess & setupSites(sites, liferayGroup);
            } catch (Exception e) {
                LOG.error("Error by setting up site " + site.getName(), e);
                return false;
            }
        }
        return totalSuccess;
    }

    private long getRunAsUserId() {
        return setupContext.getRunAsUserId();
    }

    private long getRunInCompanyId() {
        return setupContext.getRunInCompanyId();
    }

    private static void setMembership(Membership membership, long companyId, long groupId) {
        if (membership == null) {
            return;
        }

        List<UsergroupAsMember> memberGroups = membership.getUsergroupAsMember();
        assignMemberGroups(memberGroups, companyId, groupId);

        List<UserAsMember> memberUsers = membership.getUserAsMember();
        assignMemberUsers(memberUsers, companyId, groupId);

    }

    private static void assignMemberUsers(List<UserAsMember> memberUsers, long companyId, long groupId) {
        if (memberUsers == null || memberUsers.isEmpty()) {
            return;
        }

        for (UserAsMember memberUser : memberUsers) {
            try {
                final com.liferay.portal.model.User user =
                        UserLocalServiceUtil.fetchUserByScreenName(companyId, memberUser.getScreenName());
                if (user == null) {
                    LOG.error("User with screenName " + memberUser.getScreenName()
                            + " does not exists. Won't be assigned as site member.");
                    continue;
                }

                Group liferayGroup = GroupLocalServiceUtil.getGroup(groupId);
                GroupLocalServiceUtil.addUserGroup(user.getUserId(), liferayGroup.getGroupId());
                LOG.info("User " + user.getScreenName() + " was assigned as member of site "
                        + liferayGroup.getDescriptiveName());

                assignUserMemberRoles(memberUser.getRole(), companyId, liferayGroup, user);

            } catch (PortalException | SystemException e) {
                LOG.error(e);
            }

        }
    }

    private static void assignUserMemberRoles(List<Role> membershipRoles, long companyId, Group liferayGroup,
            com.liferay.portal.model.User liferayUser) {
        if (membershipRoles == null || membershipRoles.isEmpty()) {
            return;
        }

        for (Role membershipRole : membershipRoles) {
            try {
                com.liferay.portal.model.Role liferayRole =
                        RoleLocalServiceUtil.getRole(companyId, membershipRole.getName());
                UserGroupRoleLocalServiceUtil.addUserGroupRoles(liferayUser.getUserId(), liferayGroup.getGroupId(),
                        new long[] {liferayRole.getRoleId()});
                StringBuilder sb = new StringBuilder("Role ").append(liferayRole.getDescriptiveName())
                        .append(" assigned to User ").append(liferayUser.getScreenName()).append(" for site ")
                        .append(liferayGroup.getDescriptiveName());

                LOG.info(sb.toString());
            } catch (PortalException | SystemException e) {
                LOG.error("Can not add role with name" + membershipRole.getName()
                        + " does not exists. Will not be assigned.");
            }
        }

    }

    private static void assignMemberGroups(List<UsergroupAsMember> memberGroups, long companyId, long groupId) {
        if (memberGroups == null || memberGroups.isEmpty()) {
            return;
        }

        for (UsergroupAsMember memberGroup : memberGroups) {
            try {
                com.liferay.portal.model.UserGroup liferayUserGroup =
                        UserGroupLocalServiceUtil.getUserGroup(companyId, memberGroup.getUsergroupName());
                Group liferayGroup = GroupLocalServiceUtil.getGroup(groupId);
                GroupLocalServiceUtil.addUserGroupGroup(liferayUserGroup.getUserGroupId(), liferayGroup);
                LOG.info("UserGroup " + liferayUserGroup.getName() + " was assigned as site member to "
                        + liferayGroup.getDescriptiveName());

                assignGroupMemberRoles(memberGroup.getRole(), companyId, liferayGroup, liferayUserGroup);
            } catch (PortalException | SystemException e) {
                LOG.error("Cannot find UserGroup with name: " + memberGroup.getUsergroupName()
                        + ". Group won't be assigned to site.", e);
                continue;
            }
        }
    }

    private static void assignGroupMemberRoles(List<Role> membershipRoles, long companyId, Group liferayGroup,
            com.liferay.portal.model.UserGroup liferayUserGroup) {
        if (membershipRoles == null || membershipRoles.isEmpty()) {
            return;
        }

        for (Role membershipRole : membershipRoles) {
            try {
                com.liferay.portal.model.Role liferayRole =
                        RoleLocalServiceUtil.getRole(companyId, membershipRole.getName());
                UserGroupGroupRoleLocalServiceUtil.addUserGroupGroupRoles(liferayUserGroup.getUserGroupId(),
                        liferayGroup.getGroupId(), new long[] {liferayRole.getRoleId()});
                StringBuilder sb = new StringBuilder("Role ").append(liferayRole.getDescriptiveName())
                        .append(" assigned to UserGroup ").append(liferayUserGroup.getName()).append(" for site ")
                        .append(liferayGroup.getDescriptiveName());

                LOG.info(sb.toString());
            } catch (PortalException | SystemException e) {
                LOG.error("Can not add role with name" + membershipRole.getName()
                        + " does not exists. Will not be assigned.");
            }
        }
    }

    void setCustomFields(final Site site) {
        if (site.getCustomFieldSetting() == null || site.getCustomFieldSetting().isEmpty()) {
            LOG.error("Site does has no Expando field settings.");
        } else {
            Class clazz = com.liferay.portal.model.Group.class;
            String resolverHint = "Resolving customized value for page " + site.getName() + " "
                    + "failed for key %%key%% " + "and value %%value%%";
            for (CustomFieldSetting cfs : site.getCustomFieldSetting()) {
                String key = cfs.getKey();
                String value = cfs.getValue();
                long groupId = setupContext.getRunInGroupId();
                CustomFieldSettingUtil.setExpandoValue(setupContext, resolverHint.replace("%%key%%", key).replace("%%value%%", value),
                        clazz, groupId, key, value);
            }
        }
    }

    public void deleteSite(final List<com.ableneo.liferay.portal.setup.domain.Site> sites,
            final String deleteMethod) {

        switch (deleteMethod) {
            case "excludeListed":
                Map<String, Site> toBeDeletedOrganisations = convertSiteListToHashMap(sites);
                try {
                    for (com.liferay.portal.model.Group siteGroup : GroupLocalServiceUtil.getGroups(QueryUtil.ALL_POS,
                            QueryUtil.ALL_POS)) {
                        if (!toBeDeletedOrganisations.containsKey(siteGroup.getName())) {
                            deleteLiferayGroup(siteGroup);
                        }
                    }
                } catch (SystemException e) {
                    LOG.error("Error by retrieving sites!", e);
                }
                break;

            case "onlyListed":
                for (com.ableneo.liferay.portal.setup.domain.Site site : sites) {
                    String name = site.getName();
                    try {
                        com.liferay.portal.model.Group o =
                                GroupLocalServiceUtil.getGroup(getRunInCompanyId(), name);
                        GroupLocalServiceUtil.deleteGroup(o);
                    } catch (Exception e) {
                        LOG.error("Error by deleting Site !", e);
                    }
                    LOG.info("Deleting Site " + name);
                }

                break;

            default:
                LOG.error("Unknown delete method : " + deleteMethod);
                break;
        }
    }

    private static Map<String, com.ableneo.liferay.portal.setup.domain.Site> convertSiteListToHashMap(
            final List<com.ableneo.liferay.portal.setup.domain.Site> objects) {

        HashMap<String, Site> map = new HashMap<>();
        for (com.ableneo.liferay.portal.setup.domain.Site site : objects) {
            map.put(site.getName(), site);
        }
        return map;
    }

    private static void deleteLiferayGroup(Group siteGroup) {
        try {
            GroupLocalServiceUtil.deleteGroup(siteGroup.getGroupId());
            LOG.info("Deleting Site" + siteGroup.getName());
        } catch (Exception e) {
            LOG.error("Error by deleting Site !", e);
        }
    }

    public static void addSiteUsers(com.liferay.portal.model.Group group, com.liferay.portal.model.User... users)
            throws SystemException {
        for (int i = 0; i < users.length; i++) {
            addSiteUser(group, users[i]);
        }
    }

    public static void addSiteUser(com.liferay.portal.model.Group group, com.liferay.portal.model.User user)
            throws SystemException {
        LOG.info("Adding user with screenName: " + user.getScreenName() + "to group with name: " + group.getName());
        GroupLocalServiceUtil.addUserGroup(user.getUserId(), group);
    }

}
