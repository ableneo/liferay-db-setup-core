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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.CustomFieldSettingUtil;
import com.ableneo.liferay.portal.setup.core.util.PortletConstants;
import com.ableneo.liferay.portal.setup.core.util.TranslationMapUtil;
import com.ableneo.liferay.portal.setup.domain.*;
import com.liferay.exportimport.kernel.service.StagingLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserGroup;
import com.liferay.portal.kernel.service.*;

/**
 * Created by gustavnovotny on 28.08.17.
 */
public class SetupSites {

    private static final Log LOG = LogFactoryUtil.getLog(SetupSites.class);

    private SetupSites() {

    }

    public static void setupSites(final List<com.ableneo.liferay.portal.setup.domain.Site> siteList,
            final Group parentGroup) throws PortalException {

        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        for (com.ableneo.liferay.portal.setup.domain.Site site : siteList) {
            Group liferayGroup = setupSite(parentGroup, companyId, site);
            List<com.ableneo.liferay.portal.setup.domain.Site> sites = site.getSite();
            setupSites(sites, liferayGroup);
        }
    }

    private static Group setupSite(Group parentGroup, long companyId, Site site) throws PortalException {
        Group liferayGroup = null;
        long groupId = -1;
        if (site.isDefault()) {
            liferayGroup = GroupLocalServiceUtil.getGroup(companyId, GroupConstants.GUEST);
            LOG.info(String.format("Setup: default site. Group ID: %1$s", groupId));
        } else if (site.getName() == null) {
            liferayGroup = GroupLocalServiceUtil.getCompanyGroup(companyId);
            LOG.info(String.format("Setup: global site. Group ID: %1$s", groupId));
        } else {
            try {
                liferayGroup = GroupLocalServiceUtil.getGroup(companyId, site.getName());
                LOG.info(String.format("Setup: Site %1$s already exists in system, not creating...", site.getName()));

            } catch (PortalException e) {
                LOG.debug("Site does not exist.", e);
            }
        }
        ServiceContext serviceContext = new ServiceContext();

        if (liferayGroup == null) {
            LOG.info(String.format("Setup: Group (Site) %1$s does not exist in system, creating...", site.getName()));

            liferayGroup = GroupLocalServiceUtil.addGroup(SetupConfigurationThreadLocal.getRunAsUserId(),
                    GroupConstants.DEFAULT_PARENT_GROUP_ID, Group.class.getName(), 0, 0,
                    TranslationMapUtil.getLocalizationMap(site.getName()), null, GroupConstants.TYPE_SITE_RESTRICTED,
                    true, GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION, site.getSiteFriendlyUrl(), true, true,
                    serviceContext);
            LOG.info(String.format("New site created. Group ID: %1$s", groupId));
        } else {
            LOG.info(String.format("Updating site: %1$s", site.getName()));
            GroupLocalServiceUtil.updateFriendlyURL(liferayGroup.getGroupId(), site.getSiteFriendlyUrl());
        }

        if (parentGroup != null && liferayGroup != null && site.isMaintainSiteHierarchy()) {
            liferayGroup.setParentGroupId(parentGroup.getGroupId());
            GroupLocalServiceUtil.updateGroup(liferayGroup);
        } else if (liferayGroup != null && site.isMaintainSiteHierarchy()) {
            liferayGroup.setParentGroupId(0);
            GroupLocalServiceUtil.updateGroup(liferayGroup);
        }

        if (liferayGroup == null) {
            LOG.error(String.format("Failed to create or update site (not found): %1$s",site.getName()));
            return null;
        }
        LOG.info("Setting site content...");

        long userId = SetupConfigurationThreadLocal.getRunAsUserId();
        setStaging(userId, liferayGroup, site.getStaging());

        // If staging group exists for present Group, add all content to staging group
        Group stagingGroup = liferayGroup.getStagingGroup();
        if (Objects.nonNull(stagingGroup)) {
            groupId = stagingGroup.getGroupId();
        }

        SetupArticles.setupSiteStructuresAndTemplates(site, groupId);
        LOG.info("Site DDM structures and templates setting finished.");

        SetupDocumentFolders.setupDocumentFolders(site, groupId);
        LOG.info("Document Folders setting finished.");

        SetupDocuments.setupSiteDocuments(site, groupId);
        LOG.info("Documents setting finished.");

        SetupPages.setupSitePages(site, groupId);
        LOG.info("Site Pages setting finished.");

        SetupWebFolders.setupWebFolders(site, groupId);
        LOG.info("Web folders setting finished.");

        SetupCategorization.setupVocabularies(site.getVocabulary(), groupId);
        LOG.info("Site Categories setting finished.");

        SetupArticles.setupSiteArticles(site.getArticle(), site.getAdt(), site.getDdlRecordset(), groupId);
        LOG.info("Site Articles setting finished.");

        setCustomFields(groupId, site.getCustomFieldSetting());
        LOG.info("Site custom fields set up.");

        // Users and Groups should be referenced to live Group
        setMembership(site.getMembership(), companyId, liferayGroup.getGroupId());
        return liferayGroup;
    }

    private static void setMembership(Membership membership, long companyId, long groupId) {
        if (Objects.isNull(membership)) {
            return;
        }

        List<UserGroupAsMember> memberGroups = membership.getUserGroupAsMember();
        assignMemberGroups(memberGroups, companyId, groupId);

        List<UserAsMember> memberUsers = membership.getUserAsMember();
        assignMemberUsers(memberUsers, companyId, groupId);

    }

    private static void assignMemberUsers(List<UserAsMember> memberUsers, long companyId, long groupId) {
        if (Objects.isNull(memberUsers) || memberUsers.isEmpty()) {
            return;
        }

        for (UserAsMember memberUser : memberUsers) {
            User user = UserLocalServiceUtil.fetchUserByScreenName(companyId, memberUser.getScreenName());
            if (Objects.isNull(user)) {
                LOG.error(String.format("User with screenName %1$s does not exists. Won't be assigned as site member.",
                        memberUser.getScreenName()));
                continue;
            }

            try {
                Group liferayGroup = GroupLocalServiceUtil.getGroup(groupId);
                GroupLocalServiceUtil.addUserGroup(user.getUserId(), liferayGroup.getGroupId());
                LOG.info(String.format("User %1$s was assigned as member of site %2$s", user.getScreenName(),
                        liferayGroup.getDescriptiveName()));

                assignUserMemberRoles(memberUser.getRole(), companyId, liferayGroup, user);

            } catch (PortalException e) {
                LOG.error(e);
            }

        }
    }

    private static void assignUserMemberRoles(List<Role> membershipRoles, long companyId, Group liferayGroup,
            User liferayUser) {
        if (Objects.isNull(membershipRoles) || membershipRoles.isEmpty()) {
            return;
        }

        for (Role membershipRole : membershipRoles) {
            try {
                com.liferay.portal.kernel.model.Role liferayRole =
                        RoleLocalServiceUtil.getRole(companyId, membershipRole.getName());
                UserGroupRoleLocalServiceUtil.addUserGroupRoles(liferayUser.getUserId(), liferayGroup.getGroupId(),
                        new long[] {liferayRole.getRoleId()});
                StringBuilder sb = new StringBuilder("Role ").append(liferayRole.getDescriptiveName())
                        .append(" assigned to User ").append(liferayUser.getScreenName()).append(" for site ")
                        .append(liferayGroup.getDescriptiveName());

                LOG.info(sb.toString());
            } catch (PortalException e) {
                LOG.error(String.format("Can not add role with name%1$s does not exists. Will not be assigned.",
                        membershipRole.getName()));
            }
        }

    }

    private static void assignMemberGroups(List<UserGroupAsMember> memberGroups, long companyId, long groupId) {
        if (Objects.isNull(memberGroups) || memberGroups.isEmpty()) {
            return;
        }

        for (UserGroupAsMember memberGroup : memberGroups) {
            try {
                UserGroup liferayUserGroup =
                        UserGroupLocalServiceUtil.getUserGroup(companyId, memberGroup.getUserGroupName());
                Group liferayGroup = GroupLocalServiceUtil.getGroup(groupId);
                GroupLocalServiceUtil.addUserGroupGroup(liferayUserGroup.getUserGroupId(), liferayGroup);
                LOG.info(String.format("UserGroup %1$s was assigned as site member to %2$s", liferayUserGroup.getName(),
                        liferayGroup.getDescriptiveName()));

                assignGroupMemberRoles(memberGroup.getRole(), companyId, liferayGroup, liferayUserGroup);
            } catch (PortalException e) {
                LOG.error(String.format("Cannot find UserGroup with name: %1$s. Group won't be assigned to site.",
                        memberGroup.getUserGroupName()), e);
            }
        }
    }

    private static void assignGroupMemberRoles(List<Role> membershipRoles, long companyId, Group liferayGroup,
            UserGroup liferayUserGroup) {
        if (Objects.isNull(membershipRoles) || membershipRoles.isEmpty()) {
            return;
        }

        for (Role membershipRole : membershipRoles) {
            try {
                com.liferay.portal.kernel.model.Role liferayRole =
                        RoleLocalServiceUtil.getRole(companyId, membershipRole.getName());
                UserGroupGroupRoleLocalServiceUtil.addUserGroupGroupRoles(liferayUserGroup.getUserGroupId(),
                        liferayGroup.getGroupId(), new long[] {liferayRole.getRoleId()});
                StringBuilder sb = new StringBuilder("Role ").append(liferayRole.getDescriptiveName())
                        .append(" assigned to UserGroup ").append(liferayUserGroup.getName()).append(" for site ")
                        .append(liferayGroup.getDescriptiveName());

                LOG.info(sb.toString());
            } catch (PortalException e) {
                LOG.error(String.format("Can not add role with name%1$s does not exists. Will not be assigned.",
                        membershipRole.getName()));
            }
        }
    }

    static void setStaging(long userId, Group liveGroup, Staging staging) {
        if (Objects.isNull(staging)) {
            LOG.info("No staging configuration present for site.");
            return;
        }

        // only local staging supported, yet
        LOG.info(String.format("Setting up staging for site: %1$s", liveGroup.getName()));
        try {
            if (staging.getType().equals("local")) {
                ServiceContext serviceContext = new ServiceContext();
                serviceContext.setAttribute(
                        PortletConstants.STAGING_PARAM_TEMPLATE.replace("#",
                                "com_liferay_dynamic_data_mapping_web_portlet_PortletDisplayTemplatePortlet"),
                        staging.isStageAdt());

                setStagingParam(staging.isStageAdt(), PortletConstants.STAGING_PORTLET_ID_ADT, serviceContext);
                setStagingParam(staging.isStageBlogs(), PortletConstants.STAGING_PORTLET_ID_BLOGS, serviceContext);
                setStagingParam(staging.isStageBookmarks(), PortletConstants.STAGING_PORTLET_ID_BOOKMARKS,
                        serviceContext);
                setStagingParam(staging.isStageCalendar(), PortletConstants.STAGING_PORTLET_ID_CALENDAR,
                        serviceContext);
                setStagingParam(staging.isStageDdl(), PortletConstants.STAGING_PORTLET_ID_DDL, serviceContext);
                setStagingParam(staging.isStageDocumentLibrary(), PortletConstants.STAGING_PORTLET_ID_DL,
                        serviceContext);
                setStagingParam(staging.isStageForms(), PortletConstants.STAGING_PORTLET_ID_FORMS, serviceContext);
                setStagingParam(staging.isStageMessageBoards(), PortletConstants.STAGING_PORTLET_ID_MB, serviceContext);
                setStagingParam(staging.isStageMobileRules(), PortletConstants.STAGING_PORTLET_ID_MDR, serviceContext);
                setStagingParam(staging.isStagePolls(), PortletConstants.STAGING_PORTLET_ID_POLLS, serviceContext);
                setStagingParam(staging.isStageWebContent(), PortletConstants.STAGING_PORTLET_ID_WEB_CONTENT,
                        serviceContext);
                setStagingParam(staging.isStageWiki(), PortletConstants.STAGING_PORTLET_ID_WIKI, serviceContext);

                StagingLocalServiceUtil.enableLocalStaging(userId, liveGroup, staging.isBranchingPublic(),
                        staging.isBranchingPrivate(), serviceContext);
                LOG.info("Local staging switched on.");
            }
            if (staging.getType().equals("remote")) {
                LOG.error("Remote staging setup is not supported, yet. Staging not set up.");
            }
            if (staging.getType().equals("none") && liveGroup.hasLocalOrRemoteStagingGroup()) {
                ServiceContext serviceContext = new ServiceContext();
                serviceContext.setUserId(userId);
                serviceContext.setAttribute("forceDisable", true);
                StagingLocalServiceUtil.disableStaging(liveGroup, serviceContext);
                LOG.info("Staging switched off.");
            }

        } catch (PortalException e) {
            LOG.error("Error while setting up staging.", e);
        }
    }

    private static void setStagingParam(boolean isParamOn, String portletId, ServiceContext serviceContext) {
        serviceContext.setAttribute(PortletConstants.STAGING_PARAM_TEMPLATE.replace("#", portletId),
                String.valueOf(isParamOn));
    }

    static void setCustomFields(final long groupId, final List<CustomFieldSetting> customFieldSettings) {
        if (customFieldSettings == null || customFieldSettings.isEmpty()) {
            LOG.error("Site does has no Expando field settings.");
        } else {
            Class clazz = com.liferay.portal.kernel.model.Group.class;
            String resolverHint = "Resolving customized value failed for key %1$s and value %2$s";
            for (CustomFieldSetting cfs : customFieldSettings) {
                String key = cfs.getKey();
                String value = cfs.getValue();
                CustomFieldSettingUtil.setExpandoValue(String.format(resolverHint, key, value), groupId,
                        SetupConfigurationThreadLocal.getRunInCompanyId(), clazz, groupId, key, value);
            }
        }
    }

    public static void deleteSite(final List<com.ableneo.liferay.portal.setup.domain.Site> sites,
            final String deleteMethod) {

        switch (deleteMethod) {
            case "excludeListed":
                Map<String, Site> toBeDeletedOrganisations = convertSiteListToHashMap(sites);
                try {
                    for (com.liferay.portal.kernel.model.Group siteGroup : GroupLocalServiceUtil
                            .getGroups(QueryUtil.ALL_POS, QueryUtil.ALL_POS)) {
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
                        com.liferay.portal.kernel.model.Group o =
                                GroupLocalServiceUtil.getGroup(SetupConfigurationThreadLocal.getRunInGroupId(), name);
                        GroupLocalServiceUtil.deleteGroup(o);
                    } catch (Exception e) {
                        LOG.error("Error by deleting Site !", e);
                    }
                    LOG.info(String.format("Deleting Site %1$s", name));
                }

                break;

            default:
                LOG.error(String.format("Unknown delete method : %1$s", deleteMethod));
                break;
        }
    }

    private static void deleteLiferayGroup(Group siteGroup) {
        try {
            GroupLocalServiceUtil.deleteGroup(siteGroup.getGroupId());
            LOG.info(String.format("Deleting Site %1$s", siteGroup.getName()));
        } catch (Exception e) {
            LOG.error("Error by deleting Site !", e);
        }
    }

    public static void addSiteUser(com.liferay.portal.kernel.model.Group group, User user) {
        LOG.info(String.format("Adding user with screenName: %1$sto group with name: %2$s", user.getScreenName(),
                group.getName()));
        GroupLocalServiceUtil.addUserGroup(user.getUserId(), group);
    }

    public static void addSiteUsers(com.liferay.portal.kernel.model.Group group, User... users) {
        for (int i = 0; i < users.length; i++) {
            addSiteUser(group, users[i]);
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

}
