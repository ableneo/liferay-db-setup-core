package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.CustomFieldSettingUtil;
import com.ableneo.liferay.portal.setup.core.util.ResolverUtil;
import com.ableneo.liferay.portal.setup.core.util.TranslationMapUtil;
import com.ableneo.liferay.portal.setup.domain.ArticleDisplayPortlet;
import com.ableneo.liferay.portal.setup.domain.AssetPublisherPortlet;
import com.ableneo.liferay.portal.setup.domain.CustomFieldSetting;
import com.ableneo.liferay.portal.setup.domain.MenuViewPortlet;
import com.ableneo.liferay.portal.setup.domain.Page;
import com.ableneo.liferay.portal.setup.domain.PagePortlet;
import com.ableneo.liferay.portal.setup.domain.PageTemplate;
import com.ableneo.liferay.portal.setup.domain.PageTemplates;
import com.ableneo.liferay.portal.setup.domain.Pages;
import com.ableneo.liferay.portal.setup.domain.PortletPreference;
import com.ableneo.liferay.portal.setup.domain.PropertyKeyValue;
import com.ableneo.liferay.portal.setup.domain.RolePermissions;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.ableneo.liferay.portal.setup.domain.Theme;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalServiceUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.NoSuchLayoutException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.LayoutPrototype;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.LayoutTemplate;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.model.PortletPreferences;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.portlet.PortletIdCodec;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutPrototypeLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutTemplateLocalServiceUtil;
import com.liferay.portal.kernel.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.kernel.service.ResourceLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.service.permission.PortletPermissionUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.site.navigation.model.SiteNavigationMenu;
import com.liferay.site.navigation.service.SiteNavigationMenuLocalServiceUtil;
/*-
 * #%L
 * com.ableneo.liferay.db.setup.core
 * %%
 * Copyright (C) 2016 - 2021 ableneo s. r. o.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.portlet.ReadOnlyException;

public final class SetupPages {
    private static final Log LOG = LogFactoryUtil.getLog(SetupPages.class);
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS_PUBLIC;
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS_PRIVATE;
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS_PORTLET_PUBLIC;
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS_PORTLET_PRIVATE;
    private static final String LAYOUT_WAS_LOOKED_UP_IN_THEME = "Layout was looked up in theme %1$s";

    static {
        DEFAULT_PERMISSIONS_PUBLIC = new HashMap<>();
        DEFAULT_PERMISSIONS_PRIVATE = new HashMap<>();

        List<String> actionsOwner = new ArrayList<>();
        actionsOwner.add(ActionKeys.ADD_DISCUSSION);
        actionsOwner.add(ActionKeys.ADD_LAYOUT);
        actionsOwner.add(ActionKeys.CONFIGURE_PORTLETS);
        actionsOwner.add(ActionKeys.CUSTOMIZE);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.DELETE_DISCUSSION);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);
        actionsOwner.add(ActionKeys.VIEW);
        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);
        DEFAULT_PERMISSIONS_PUBLIC.put(RoleConstants.OWNER, actionsOwner);

        List<String> actionsUser = new ArrayList<>();
        //        actionsUser.add(ActionKeys.ADD_DISCUSSION);
        //        actionsUser.add(ActionKeys.CUSTOMIZE);
        actionsUser.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS_PUBLIC.put(RoleConstants.SITE_MEMBER, actionsUser);

        DEFAULT_PERMISSIONS_PRIVATE.putAll(DEFAULT_PERMISSIONS_PUBLIC);

        List<String> actionsGuest = new ArrayList<>();
        actionsGuest.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS_PUBLIC.put(RoleConstants.GUEST, actionsGuest);
    }

    static {
        DEFAULT_PERMISSIONS_PORTLET_PUBLIC = new HashMap<>();
        DEFAULT_PERMISSIONS_PORTLET_PRIVATE = new HashMap<>();

        List<String> actionsOwner = new ArrayList<>();
        actionsOwner.add(ActionKeys.CONFIGURE_PORTLETS);
        actionsOwner.add(ActionKeys.CUSTOMIZE);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS_PORTLET_PUBLIC.put(RoleConstants.OWNER, actionsOwner);

        List<String> actionsUser = new ArrayList<>();
        actionsUser.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS_PORTLET_PUBLIC.put(RoleConstants.SITE_MEMBER, actionsUser);

        DEFAULT_PERMISSIONS_PORTLET_PRIVATE.putAll(DEFAULT_PERMISSIONS_PORTLET_PUBLIC);

        List<String> actionsGuest = new ArrayList<>();
        actionsGuest.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS_PORTLET_PUBLIC.put(RoleConstants.GUEST, actionsGuest);
    }

    private SetupPages() {}

    /**
     * @param site
     * @param groupId
     *
     * @throws PortalException
     */
    public static void setupSitePages(final Site site, final long groupId) throws PortalException {
        Pages publicPages = site.getPublicPages();
        long company = SetupConfigurationThreadLocal.getRunInCompanyId();
        long userid = SetupConfigurationThreadLocal.getRunAsUserId();
        if (publicPages != null) {
            if (publicPages.getTheme() != null) {
                setupTheme(groupId, publicPages.getTheme(), false);
            }
            if (publicPages.isDeleteExistingPages()) {
                LOG.info(String.format("Setup: Deleting pages from site %1$s", site.getName()));
                deletePages(groupId, false);
            }
            addPages(
                publicPages.getPage(),
                publicPages.getDefaultLayout(),
                publicPages.getDefaultLayoutsThemeId(),
                groupId,
                false,
                0,
                company,
                userid
            );
            if (publicPages.getVirtualHost() != null) {
                LayoutSetLocalServiceUtil.updateVirtualHost(groupId, false, publicPages.getVirtualHost());
            }
        }

        Pages privatePages = site.getPrivatePages();
        if (privatePages != null) {
            if (privatePages.getTheme() != null) {
                setupTheme(groupId, privatePages.getTheme(), true);
            }
            if (privatePages.isDeleteExistingPages()) {
                LOG.info(String.format("Setup: Deleting pages from site %1$s", site.getName()));
                deletePages(groupId, true);
            }
            addPages(
                privatePages.getPage(),
                privatePages.getDefaultLayout(),
                privatePages.getDefaultLayoutsThemeId(),
                groupId,
                true,
                0,
                company,
                userid
            );
            if (privatePages.getVirtualHost() != null) {
                LayoutSetLocalServiceUtil.updateVirtualHost(groupId, true, privatePages.getVirtualHost());
            }
        }
    }

    /**
     * Set the page templates up. As this is heavily based on page (layout).
     *
     * @param pageTemplates The page template definitions that are imported.
     */
    public static void setupPageTemplates(final PageTemplates pageTemplates) {
        try {
            for (PageTemplate pageTemplate : pageTemplates.getPageTemplate()) {
                String name = pageTemplate.getName();
                if (name != null) {
                    LayoutPrototype lp;
                    DynamicQuery dq = LayoutPrototypeLocalServiceUtil
                        .dynamicQuery()
                        .add(PropertyFactoryUtil.forName("name").like("%" + name + "%"));
                    List<LayoutPrototype> listLayoutPrototype = LayoutPrototypeLocalServiceUtil.dynamicQuery(dq);
                    long groupId = SetupConfigurationThreadLocal.getRunInGroupId();
                    long userid = SetupConfigurationThreadLocal.getRunAsUserId();
                    long company = SetupConfigurationThreadLocal.getRunInCompanyId();
                    if (listLayoutPrototype != null && !listLayoutPrototype.isEmpty()) {
                        lp = listLayoutPrototype.get(0);
                    } else {
                        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(
                            pageTemplate.getTitleTranslation(),
                            groupId,
                            name,
                            String.format(" Page template  %1$s", name)
                        );
                        Map<Locale, String> nameMap = TranslationMapUtil.getTranslationMap(
                            pageTemplate.getTitleTranslation(),
                            groupId,
                            name,
                            String.format(" Page template  %1$s", name)
                        );
                        lp =
                            LayoutPrototypeLocalServiceUtil.addLayoutPrototype(
                                userid,
                                company,
                                titleMap,
                                nameMap,
                                true,
                                new ServiceContext()
                            );
                    }
                    if (lp != null) {
                        Layout layout = lp.getLayout();
                        if (pageTemplate.getPage() != null) {
                            Page page = pageTemplate.getPage();
                            if (page.getFriendlyUrl() != null && !page.getFriendlyUrl().equals("")) {
                                LOG.error(
                                    String.format(
                                        "The page of page template %1$s may not have a friendly URL! Will ignore it!",
                                        name
                                    )
                                );
                            }
                            setupLiferayPage(layout, page, null, null, groupId, false, company, userid, name);
                        }
                    } else {
                        LOG.error(String.format("Could not create or find the page template %1$s", name));
                    }
                }
            }
        } catch (PortalException | SystemException e) {
            LOG.error("Problem during creating page templates ", e);
        }
    }

    /**
     * @param groupId
     * @param theme
     * @param isPrivate
     *
     * @throws SystemException
     * @throws PortalException
     */
    private static void setupTheme(final long groupId, final Theme theme, final boolean isPrivate)
        throws PortalException {
        Group group = GroupLocalServiceUtil.getGroup(groupId);
        LayoutSet set;
        if (isPrivate) {
            set = group.getPrivateLayoutSet();
        } else {
            set = group.getPublicLayoutSet();
        }
        set.setThemeId(theme.getName());
        set.setSettingsProperties(mergeConvertProperties(set.getSettingsProperties(), theme.getLayoutSetSettings()));
        LayoutSetLocalServiceUtil.updateLayoutSet(set);
    }

    private static UnicodeProperties mergeConvertProperties(UnicodeProperties original, List<PropertyKeyValue> props) {
        if (props == null || props.isEmpty()) {
            return original;
        }
        UnicodeProperties res = new UnicodeProperties();
        res.putAll(original);
        for (PropertyKeyValue kv : props) {
            res.put(kv.getKey(), kv.getValue());
        }
        return res;
    }

    /**
     * @param pages
     * @param groupId
     * @param isPrivate
     * @param parentLayoutId
     * @param company
     * @param userId
     *
     * @throws SystemException
     * @throws PortalException
     */
    private static void addPages(
        final List<Page> pages,
        String defaultLayout,
        String defaultLayoutContainedInThemeWithId,
        final long groupId,
        final boolean isPrivate,
        final long parentLayoutId,
        final long company,
        final long userId
    )
        throws PortalException {
        for (Page page : pages) {
            Layout layout = null;
            try {
                layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, isPrivate, page.getFriendlyUrl());
                LOG.info(String.format("Setup: Page %1$s already exist, not creating...", page.getName()));
                if (layout != null && page.isDeleteExistingPages()) {
                    LayoutLocalServiceUtil.deleteLayout(layout);
                    if (page.getLinkToUrl() == null || page.getLinkToUrl().equals("")) {
                        layout = createPage(groupId, page, parentLayoutId, isPrivate);
                    } else {
                        layout = createLinkPage(page, groupId, parentLayoutId, userId);
                    }
                } else if (layout != null && (page.getLinkToUrl() != null && !page.getLinkToUrl().equals(""))) {
                    updateLinkPage(page, groupId);
                } else {
                    LOG.warn(
                        String.format(
                            "Setup: unhandled 'else' while creatng page[%1$s]/layout[%2$s]",
                            page.getName(),
                            page.getLayout()
                        )
                    );
                }
            } catch (NoSuchLayoutException e) {
                if (page.getLinkToUrl() == null || page.getLinkToUrl().equals("")) {
                    layout = createPage(groupId, page, parentLayoutId, isPrivate);
                } else {
                    layout = createLinkPage(page, groupId, parentLayoutId, userId);
                }
                LOG.info(String.format("Setup: Page %1$s created...", page.getName()));
            } catch (Exception ex) {
                LOG.error(ex);
            }
            // If the page has not a layout set, set the default one. Otherwise set that layout as the default for the
            // subtree
            if (page.getLayout() == null) {
                page.setLayout(defaultLayout);
                page.setLayoutThemeId(defaultLayoutContainedInThemeWithId);
            } else {
                defaultLayout = page.getLayout();
                defaultLayoutContainedInThemeWithId = page.getLayoutThemeId();
            }
            setupLiferayPage(
                layout,
                page,
                defaultLayout,
                defaultLayoutContainedInThemeWithId,
                groupId,
                isPrivate,
                company,
                userId,
                null
            );
        }
    }

    private static void setupLiferayPage(
        final Layout layout,
        final Page page,
        final String defaultLayout,
        final String defaultLayoutContainedInThemeWithId,
        final long groupId,
        final boolean isPrivate,
        final long company,
        final long userId,
        final String pageTemplateName
    )
        throws PortalException {
        if (page.getTheme() != null) {
            setPageTheme(layout, page);
        }
        if (page.getLayout() != null) {
            setLayoutTemplate(layout, page, userId);
        }

        setPageTarget(page, layout);

        List<PagePortlet> portlets = page.getPagePortlet();
        if (portlets != null && !portlets.isEmpty()) {
            List<PortletWithRuntimeData> deferredAdd = new ArrayList<SetupPages.PortletWithRuntimeData>();
            for (PagePortlet portlet : portlets) {
                addRuntimeInfo(deferredAdd, portlet, page, layout, company, groupId);
            }
            portlets.clear();
            portlets.addAll(deferredAdd);
        }

        List<Page> subPages = page.getPage();
        if (subPages != null && !subPages.isEmpty()) {
            if (pageTemplateName != null && !pageTemplateName.equals("")) {
                LOG.error(
                    String.format("Page template %1$s may not have any sub-pages! Will ignore them!", pageTemplateName)
                );
            } else {
                addPages(
                    subPages,
                    defaultLayout,
                    defaultLayoutContainedInThemeWithId,
                    groupId,
                    isPrivate,
                    layout.getLayoutId(),
                    company,
                    userId
                );
            }
        }

        if (page.getCustomFieldSetting() != null && !page.getCustomFieldSetting().isEmpty()) {
            setCustomFields(groupId, company, page, layout);
        }

        SetupPermissions.updatePermission(
            String.format("Page %1$s", page.getFriendlyUrl()),
            company,
            layout.getPlid(),
            Layout.class,
            page.getRolePermissions(),
            getDefaultPermissions(isPrivate)
        );
    }

    private static HashMap<String, List<String>> getDefaultPermissions(final boolean isPrivate) {
        if (isPrivate) {
            return new HashMap<String, List<String>>(DEFAULT_PERMISSIONS_PRIVATE);
        }
        return new HashMap<String, List<String>>(DEFAULT_PERMISSIONS_PUBLIC);
    }

    private static HashMap<String, List<String>> getDefaultPortletPermissions(final boolean isPrivate) {
        if (isPrivate) {
            return new HashMap<String, List<String>>(DEFAULT_PERMISSIONS_PORTLET_PRIVATE);
        }
        return new HashMap<String, List<String>>(DEFAULT_PERMISSIONS_PORTLET_PUBLIC);
    }

    private static Layout createLinkPage(
        final Page p,
        final long groupId,
        final long parentLayoutId,
        final long userId
    ) {
        // all values are usually retrieved via special methods from our code
        // for better readability I have added the real values here

        String title = "my title";
        ServiceContext serviceContext = new ServiceContext();
        String layoutType = LayoutConstants.TYPE_URL;
        boolean hidden = p.isHidden();
        String friendlyURL = p.getFriendlyUrl();
        // add the layout
        Layout layout = null;
        try {
            layout =
                LayoutLocalServiceUtil.addLayout(
                    userId,
                    groupId,
                    false,
                    parentLayoutId,
                    title,
                    title,
                    StringPool.BLANK,
                    layoutType,
                    hidden,
                    friendlyURL,
                    serviceContext
                );

            String linkToPageUrl = p.getLinkToUrl();
            // set the value of the "link to page"
            UnicodeProperties props = layout.getTypeSettingsProperties();
            props.put("url", linkToPageUrl);
            layout.setTypeSettingsProperties(props);
            LayoutLocalServiceUtil.updateLayout(
                layout.getGroupId(),
                layout.isPrivateLayout(),
                layout.getLayoutId(),
                layout.getTypeSettings()
            );
        } catch (PortalException | SystemException e) {
            LOG.error(
                String.format(
                    "Could not create link page %1$s with link to url %2$s",
                    p.getFriendlyUrl(),
                    p.getLinkToUrl()
                ),
                e
            );
        }
        return layout;
    }

    private static void updateLinkPage(final Page page, final long groupId) {
        try {
            Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, page.getFriendlyUrl());
            if (layout.getLayoutType().getTypeSettingsProperties().get("url") == null) {
                LOG.error(
                    "Could not update link page " +
                    page.getFriendlyUrl() +
                    " with link to url" +
                    " " +
                    page.getLinkToUrl() +
                    " because page is not a link type page! " +
                    " Maybe it has been imported before as non link type page. Please " +
                    "delete it and rerun!"
                );
            } else {
                UnicodeProperties props = layout.getTypeSettingsProperties();
                props.put("url", page.getLinkToUrl());
                layout.setTypeSettingsProperties(props);
                layout.setHidden(page.isHidden());
                LayoutLocalServiceUtil.updateLayout(
                    layout.getGroupId(),
                    layout.isPrivateLayout(),
                    layout.getLayoutId(),
                    layout.getTypeSettings()
                );
            }
        } catch (PortalException | SystemException e) {
            LOG.error(
                String.format(
                    "Could not update link page %1$s with link to url %2$s",
                    page.getFriendlyUrl(),
                    page.getLinkToUrl()
                ),
                e
            );
        }
    }

    private static Layout createPage(
        final long groupId,
        final Page currentPage,
        final long parentLayoutId,
        final boolean isPrivate
    )
        throws PortalException {
        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(
            currentPage.getTitleTranslation(),
            groupId,
            currentPage.getName(),
            String.format(" Page with title %1$s", currentPage.getFriendlyUrl())
        );

        Locale locale = LocaleUtil.getSiteDefault();

        Map<Locale, String> descriptionMap = new HashMap<>();
        descriptionMap.put(locale, StringPool.BLANK);

        Map<Locale, String> friendlyURLMap = new HashMap<>();
        friendlyURLMap.put(locale, currentPage.getFriendlyUrl());

        return LayoutLocalServiceUtil.addLayout(
            SetupConfigurationThreadLocal.getRunAsUserId(),
            groupId,
            isPrivate,
            parentLayoutId,
            titleMap,
            titleMap,
            null,
            null,
            null,
            currentPage.getType(),
            StringPool.BLANK,
            currentPage.isHidden(),
            friendlyURLMap,
            new ServiceContext()
        );
    }

    private static void setCustomFields(final long groupId, final long company, final Page page, final Layout layout) {
        Class clazz = Layout.class;
        String resolverHint =
            "Resolving customized value for page " +
            page.getFriendlyUrl() +
            " " +
            "failed for key " +
            "%%key%% and value %%value%%";
        for (CustomFieldSetting cfs : page.getCustomFieldSetting()) {
            String key = cfs.getKey();
            String value = cfs.getValue();
            CustomFieldSettingUtil.setExpandoValue(
                resolverHint.replace("%%key%%", key).replace("%%value%%", value),
                groupId,
                company,
                clazz,
                layout.getPlid(),
                key,
                value
            );
        }
    }

    private static void setPageTarget(final Page page, final Layout layout) {
        UnicodeProperties props = layout.getTypeSettingsProperties();
        props.put("target", page.getTarget());
        layout.setTypeSettingsProperties(props);
        try {
            LayoutLocalServiceUtil.updateLayout(
                layout.getGroupId(),
                layout.isPrivateLayout(),
                layout.getLayoutId(),
                layout.getTypeSettings()
            );
        } catch (PortalException e) {
            LOG.error(
                "Can not set target attribute value '" +
                page.getTarget() +
                "' to page with layoutId:" +
                layout.getLayoutId() +
                ".",
                e
            );
        }
    }

    private static void setPageTheme(final Layout layout, final Page page) {
        Theme theme = page.getTheme();
        if (theme != null) {
            layout.setThemeId(theme.getName());
            try {
                LayoutLocalServiceUtil.updateLayout(
                    layout.getGroupId(),
                    layout.isPrivateLayout(),
                    layout.getLayoutId(),
                    layout.getTypeSettings()
                );
            } catch (PortalException e) {
                LOG.error(e);
            }
            LOG.info(String.format("setting theme on page: %1$s : %2$s", page.getName(), theme.getName()));
        }
    }

    private static boolean isLinkPage(Page page) {
        if (page.getLinkToUrl() != null && !page.getLinkToUrl().equals("")) {
            LOG.error(
                "This is a link page! It cannot be cleared. If you intend to use this page for portlets, please delete this page, or remove the link from the page!"
            );
            return true;
        } else {
            return false;
        }
    }

    private static void addAssetPublisherPortletIntoPage(
        Page page,
        Layout layout,
        AssetPublisherPortlet portlet,
        long company,
        long groupId
    )
        throws PortalException {
        if (isLinkPage(page)) {
            return;
        }
        LOG.info(
            "Adding AssetPublisherPortlet: " +
            portlet.getColumn() +
            "@" +
            portlet.getColumnPosition() +
            "-" +
            portlet.getPortletId() +
            "; ADTgroupIdFrom:" +
            portlet.getAdtTemplateSiteUrl()
        );
        PagePortlet toInsert = null;

        toInsert = new PagePortlet();
        toInsert.setColumn(portlet.getColumn());
        toInsert.setColumnPosition(portlet.getColumnPosition());
        toInsert.setPortletId(portlet.getPortletId());
        toInsert.getPortletPreference().addAll(portlet.getPortletPreference());
        toInsert.setRolePermissions(portlet.getRolePermissions());

        //		// ADT STYLE is in this groupId: // 35345 == myra portal, now..
        //		prefMap.put("displayStyleGroupId", "35345");
        Group group = GroupLocalServiceUtil.getFriendlyURLGroup(company, portlet.getAdtTemplateSiteUrl());
        toInsert
            .getPortletPreference()
            .add(newPortletPreference("displayStyleGroupId", String.valueOf(group.getPrimaryKey())));

        //		//-TODO: decode runtime..
        //		// [ WIKI-FTl, ...
        ////		prefMap.put("classNameIds", new String[] {"32502", "28501", "20008", "28506", "33222", "34325", "33246", "20009", "34316", "33208"});
        List<String> shownArticleClasses = StringUtil.split(portlet.getShownArticleClasses(), CharPool.SEMICOLON);
        Set<String> shownClassIds = new HashSet<String>();
        for (String cls : shownArticleClasses) {
            ClassName shownClass = ClassNameLocalServiceUtil.getClassName(cls);
            shownClassIds.add(String.valueOf(shownClass.getPrimaryKey()));
        }
        toInsert
            .getPortletPreference()
            .add(newPortletPreference("classNameIds", StringUtil.merge(shownClassIds, StringPool.COMMA)));

        ////		//TODO: runtime query of all structure templates, where class-type == com.liferay.journal.model.JournalArticle
        ////		//BASIC-WEB-CONTENT, TEXT-ARRAY-STRUCT, BUTTON-BOTTOM-STRUCT
        //////		28501: INSERT INTO CLASSNAME_ VALUES(0,28501,'com.liferay.journal.model.JournalArticle')
        //////		INSERT INTO DDMSTRUCTURE VALUES(0,'55d9d223-9da5-bda0-0c80-88b8d8c97997',34987,20128,20101,20105,NULL,20105,NULL,'2020-11-21 04:57:26.721000','2020-11-21 04:57:26.721000',0,28501,'BASIC-WEB-CONTENT','1.0','<?xml version=''1.0'' encoding=''UTF-8''?><root available-locales="en_US,sv_SE,pt_BR,ja_JP,fr_FR,hu_HU,de_DE,ca_ES,ar_SA,fi_FI,zh_CN,es_ES,nl_NL" default-locale="en_US"><Name language-id="sv_SE">Vanligt inneh\u00e5ll</Name><Name language-id="pt_BR">Conte\u00fado Web b\u00e1sico</Name><Name language-id="ja_JP">\u57fa\u672cWeb\u30b3\u30f3\u30c6\u30f3\u30c4</Name><Name language-id="fr_FR">Contenu web basique</Name><Name language-id="hu_HU">Alapvet\u0151 webtartalom</Name><Name language-id="de_DE">Einfacher Webcontent</Name><Name language-id="ca_ES">Contigut web b\u00e0sic</Name><Name language-id="ar_SA">\u062a\u062d\u0631\u064a\u0631 \u0645\u062d\u062a\u0648\u0649</Name><Name language-id="fi_FI">Tavallinen web-sis\u00e4lt\u00f6</Name><Name language-id="en_US">Basic Web Content</Name><Name language-id="zh_CN">\u57fa\u672c Web \u5185\u5bb9</Name><Name language-id="es_ES">Contenido web b\u00e1sico</Name><Name language-id="nl_NL">Basiswebcontent</Name></root>','<?xml version=''1.0'' encoding=''UTF-8''?><root available-locales="en_US,sv_SE,pt_BR,ja_JP,fr_FR,hu_HU,de_DE,ca_ES,ar_SA,fi_FI,zh_CN,es_ES,nl_NL" default-locale="en_US"><Description language-id="sv_SE">Vanligt inneh\u00e5ll</Description><Description language-id="pt_BR">Conte\u00fado Web b\u00e1sico</Description><Description language-id="ja_JP">\u57fa\u672cWeb\u30b3\u30f3\u30c6\u30f3\u30c4</Description><Description language-id="fr_FR">Contenu web basique</Description><Description language-id="hu_HU">Alapvet\u0151 webtartalom</Description><Description language-id="de_DE">Einfacher Webcontent</Description><Description language-id="ca_ES">Contigut web b\u00e0sic</Description><Description language-id="ar_SA">\u062a\u062d\u0631\u064a\u0631 \u0645\u062d\u062a\u0648\u0649</Description><Description language-id="fi_FI">Tavallinen web-sis\u00e4lt\u00f6</Description><Description language-id="en_US">Basic Web Content</Description><Description language-id="zh_CN">\u57fa\u672c Web \u5185\u5bb9</Description><Description language-id="es_ES">Contenido web b\u00e1sico</Description><Description language-id="nl_NL">Basiswebcontent</Description></root>','{"availableLanguageIds":["en_US"],"successPage":{"body":{},"title":{},"enabled":false},"defaultLanguageId":"en_US","fields":[{"fieldNamespace":"ddm","indexType":"text","dataType":"html","predefinedValue":{},"name":"content","localizable":true,"tip":{},"label":{"en_US":"content"},"type":"ddm-text-html","showLabel":true}]}','json',0,NULL)
        //////		INSERT INTO DDMSTRUCTURE VALUES(0,'5dbceeef-b83f-63db-651a-159ce9964d6e',35348,35345,20101,20130,'Test Test',20130,'Test Test','2020-11-25 13:18:47.567000','2020-11-25 13:18:47.567000',0,28501,'TEXT-ARRAY-STRUCT','1.0','<?xml version=''1.0'' encoding=''UTF-8''?><root available-locales="en_US" default-locale="en_US"><Name language-id="en_US">TEXT-ARRAY-STRUCT</Name></root>','','{"availableLanguageIds":["en_US"],"successPage":{"body":{},"title":{},"enabled":false},"defaultLanguageId":"en_US","fields":[{"indexType":"keyword","repeatable":true,"dataType":"string","predefinedValue":{"en_US":""},"name":"textString","localizable":false,"readOnly":false,"tip":{"en_US":""},"label":{"en_US":"Sz\u00f6veg"},"type":"text","required":false,"showLabel":true}]}','json',0,NULL)
        //////		INSERT INTO DDMSTRUCTURE VALUES(0,'006c2151-f6cb-c1fe-19f6-5ce259582b28',35352,35345,20101,20130,'Test Test',20130,'Test Test','2020-11-25 13:18:47.648000','2020-11-25 13:18:47.648000',0,28501,'BUTTON-BOTTOM-STRUCT','1.0','<?xml version=''1.0'' encoding=''UTF-8''?><root available-locales="en_US" default-locale="en_US"><Name language-id="en_US">BUTTON-BOTTOM-STRUCT</Name></root>','','{"availableLanguageIds":["en_US"],"successPage":{"body":{},"title":{},"enabled":false},"defaultLanguageId":"en_US","fields":[{"indexType":"keyword","repeatable":false,"dataType":"string","predefinedValue":{"en_US":""},"name":"titletext","localizable":true,"readOnly":false,"tip":{"en_US":""},"label":{"en_US":"Cimsor"},"type":"text","required":false,"showLabel":true},{"indexType":"text","repeatable":true,"dataType":"string","predefinedValue":{"en_US":""},"name":"contenttext","localizable":true,"readOnly":false,"tip":{"en_US":""},"label":{"en_US":"Szoveges tartalom"},"type":"textarea","required":false,"showLabel":true},{"dataType":"boolean","predefinedValue":{"en_US":"false"},"readOnly":false,"label":{"en_US":"van gombja?"},"type":"checkbox","required":false,"showLabel":true,"nestedFields":[{"indexType":"keyword","repeatable":false,"dataType":"string","predefinedValue":{"en_US":""},"name":"buttontitle","localizable":true,"readOnly":false,"tip":{"en_US":""},"label":{"en_US":"szovege"},"type":"text","required":false,"showLabel":true},{"indexType":"keyword","repeatable":false,"dataType":"string","predefinedValue":{"en_US":""},"name":"buttonlink","localizable":true,"readOnly":false,"tip":{"en_US":""},"label":{"en_US":"Url/link"},"type":"text","required":false,"showLabel":true},{"dataType":"string","predefinedValue":{"en_US":"[\"\"]"},"multiple":false,"readOnly":false,"label":{"en_US":"gomb szine/tipusa"},"type":"select","required":false,"showLabel":true,"indexType":"keyword","repeatable":false,"name":"buttontype","options":[{"label":{"en_US":"elsodleges"},"value":"btn-primary"},{"label":{"en_US":"masodlagos"},"value":"btn-secondary"},{"label":{"en_US":"atlatszo"},"value":"btn-info"},{"label":{"en_US":"link-szeru"},"value":"btn-link"}],"localizable":true,"tip":{"en_US":""}},{"indexType":"keyword","repeatable":false,"dataType":"string","predefinedValue":{"en_US":""},"name":"buttonicon","localizable":true,"readOnly":false,"tip":{"en_US":""},"label":{"en_US":"ikon/css class"},"type":"text","required":false,"showLabel":true}],"indexType":"keyword","repeatable":true,"name":"hasbutton","localizable":true,"tip":{"en_US":""}}]}','json',0,NULL)
        ////		prefMap.put("classTypeIdsJournalArticleAssetRendererFactory", "34987,35348,35352");
        //        ClassName journalArticleClass = ClassNameLocalServiceUtil.getClassName(portlet.getAssetRendererBaseClass());
        //        List<DDLRecordSet> journalDescendants = DDLRecordSetLocalServiceUtil.getDDMStructureRecordSets(journalArticleClass.getPrimaryKey());
        //        Set<String> ddlIds = new HashSet<String>();
        //        for (DDLRecordSet ddlRecord : journalDescendants) {
        //        	ddlIds.add(String.valueOf(ddlRecord.getPrimaryKey()));
        //        }
        //        toInsert.getPortletPreference().add(newPortletPreference("classTypeIdsJournalArticleAssetRendererFactory",StringUtil.merge(ddlIds, StringPool.COMMA)));

        insertPortletIntoPage(page, layout, toInsert, company, groupId);
    }

    private static void addArticleDisplayPortletIntoPage(
        final Page page,
        final Layout layout,
        final ArticleDisplayPortlet portlet,
        final long companyId,
        final long groupId
    )
        throws PortalException {
        if (isLinkPage(page)) {
            return;
        }
        LOG.info(
            "Adding ArticleDisplayPortlet: " +
            portlet.getColumn() +
            "@" +
            portlet.getColumnPosition() +
            "-" +
            portlet.getPortletId() +
            "; " +
            portlet.getArticleFolder().getFolderPath() +
            ":" +
            portlet.getArticleId()
        );
        PagePortlet toInsert = null;
        long folderId = SetupArticles.getCreateFolderId(
            portlet.getArticleFolder().getFolderPath(),
            groupId,
            portlet.getArticleFolder().getRolePermissions()
        );
        JournalArticle journalArticle = SetupArticles.getJournalArticle(
            portlet.getArticleId(),
            folderId,
            groupId,
            portlet.getArticleFolder().getFolderPath()
        );
        if (journalArticle != null) {
            Long assetEntryId = SetupArticles.getJournalAssetEntryId(journalArticle);

            toInsert = new PagePortlet();
            toInsert.setColumn(portlet.getColumn());
            toInsert.setColumnPosition(portlet.getColumnPosition());
            toInsert.setPortletId(portlet.getPortletId());
            toInsert.getPortletPreference().addAll(portlet.getPortletPreference());
            toInsert.setRolePermissions(portlet.getRolePermissions());

            toInsert.getPortletPreference().add(newPortletPreference("groupId", String.valueOf(groupId)));
            toInsert.getPortletPreference().add(newPortletPreference("articleId", journalArticle.getArticleId())); //String.valueOf(journalArticle.getPrimaryKey())));
            if (assetEntryId != null) {
                toInsert.getPortletPreference().add(newPortletPreference("assetEntryId", String.valueOf(assetEntryId)));
            }
        } else {
            LOG.error("No journal entry found = skip adding it to the wrappedPortlet. Manual task!");
            toInsert = portlet;
        }
        insertPortletIntoPage(page, layout, toInsert, companyId, groupId);
    }

    private static void addMenuViewPortletIntoPage(
        Page page,
        Layout layout,
        MenuViewPortlet portlet,
        long companyId,
        long groupId
    )
        throws PortalException {
        if (isLinkPage(page)) {
            LOG.info(" ! SKIP-page is a link");
            return;
        }
        LOG.info(
            "Adding MenuViewPortlet: " +
            portlet.getColumn() +
            "@" +
            portlet.getColumnPosition() +
            "-" +
            portlet.getPortletId() +
            "; " +
            portlet.getAdtTemplate() +
            ":" +
            portlet.getMenuName()
        );
        PagePortlet toInsert = null;

        Long menuId = getMenuIdByName(groupId, portlet.getMenuName());
        if (menuId == null || menuId == 0L) {
            LOG.info(" ! SKIP-no such menu to add");
            return;
        }
        portlet.getPortletPreference().add(newPortletPreference("siteNavigationMenuId", String.valueOf(menuId)));
        DDMTemplate adtTemplate = getAdtTemplate(groupId, portlet.getAdtTemplate(), portlet.getAdtTemplateSiteUrl());
        if (adtTemplate == null) {
            LOG.info(" ! SKIP-no such template to display with");
            return;
        }
        portlet
            .getPortletPreference()
            .add(newPortletPreference("displayStyle", "ddmTemplate_" + portlet.getAdtTemplate()));
        Group group = GroupLocalServiceUtil.getFriendlyURLGroup(companyId, portlet.getAdtTemplateSiteUrl());
        portlet
            .getPortletPreference()
            .add(newPortletPreference("displayStyleGroupId", String.valueOf(group.getPrimaryKey()))); //

        toInsert = new PagePortlet();
        toInsert.setColumn(portlet.getColumn());
        toInsert.setColumnPosition(portlet.getColumnPosition());
        toInsert.setPortletId(portlet.getPortletId());
        toInsert.setRolePermissions(portlet.getRolePermissions());
        //		toInsert.getPortletPreference().addAll(portlet.getPortletPreference());
        for (Entry<String, String> e : CONFIG_MENU_VIEW_PREFERENCES.entrySet()) {
            toInsert.getPortletPreference().add(newPortletPreference(e.getKey(), e.getValue()));
        }
        toInsert.getPortletPreference().addAll(portlet.getPortletPreference());

        insertPortletIntoPage(page, layout, toInsert, companyId, groupId);
    }

    private static DDMTemplate getAdtTemplate(long groupId, String adtTemplate, String adtClass) {
        long classNameId = 0L; // 0 == no struct!

        DDMTemplate ddmTemplate = null;
        try {
            //ddmTemplate = DDMTemplateLocalServiceUtil.fetchTemplate(groupId, classNameId, adtTemplate, true);
            //            DDMTemplateLocalServiceUtil.getTemplate(0, 0, "left-menu-template".toUpperCase(), true);
            List<DDMTemplate> tplsWithoutStruct = DDMTemplateLocalServiceUtil.getTemplates(classNameId);
            for (DDMTemplate tpl : tplsWithoutStruct) {
                if (
                    /*adtTemplate.equalsIgnoreCase(tpl.getName()) || */adtTemplate.equalsIgnoreCase(
                        tpl.getTemplateKey()
                    ) &&
                    (groupId == 0L || groupId == tpl.getGroupId())
                ) {
                    ddmTemplate = tpl;
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error(String.format("Error while trying to find ADT with key: %1$s", adtTemplate));
        }

        return ddmTemplate;
    }

    private static Long getMenuIdByName(long groupId, String menuName) {
        List<SiteNavigationMenu> existingMenus = SiteNavigationMenuLocalServiceUtil.getSiteNavigationMenus(groupId);
        for (SiteNavigationMenu existingMenu : existingMenus) {
            if (menuName.equalsIgnoreCase(existingMenu.getName())) {
                LOG.info(" i '" + menuName + "' found");
                return existingMenu.getPrimaryKey();
            }
        }
        LOG.error(" i '" + menuName + "' NOT THERE");
        return null;
    }

    private static PortletPreference newPortletPreference(String key, String value) {
        PortletPreference pref = new PortletPreference();
        pref.setKey(key);
        pref.setValue(value);
        return pref;
    }

    private static void addGenericPortletIntoPage(
        final Page page,
        final Layout layout,
        final PagePortlet portlet,
        final long companyId,
        final long groupId
    )
        throws PortalException {
        if (isLinkPage(page)) {
            return;
        }
        LOG.info(
            "Adding PagePortlet: " +
            portlet.getColumn() +
            "@" +
            portlet.getColumnPosition() +
            "-" +
            portlet.getPortletId() +
            ";"
        );
        insertPortletIntoPage(page, layout, portlet, companyId, groupId);
    }

    private static void insertPortletIntoPage(
        final Page page,
        final Layout layout,
        final PagePortlet portlet,
        final long companyId,
        final long groupId
    )
        throws PortalException {
        long plid = layout.getPlid();
        long ownerId = PortletKeys.PREFS_OWNER_ID_DEFAULT;
        int ownerType = PortletKeys.PREFS_OWNER_TYPE_LAYOUT;
        long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();

        LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();

        String portletId = portlet.getPortletId();
        String column = portlet.getColumn();

        String portletIdInc = "";
        try {
            int columnPos = portlet.getColumnPosition();
            portletIdInc = layoutTypePortlet.addPortletId(runAsUserId, portletId, column, columnPos, false);
            if (portletIdInc == null) {
                portletIdInc = portletId;
            }
        } catch (SystemException e) {
            LOG.error("Add wrappedPortlet error ", e);
        }

        javax.portlet.PortletPreferences preferences = PortletPreferencesLocalServiceUtil.getPreferences(
            companyId,
            ownerId,
            ownerType,
            plid,
            portletIdInc
        );
        List<PortletPreference> prefsList = portlet.getPortletPreference();
        for (PortletPreference p : prefsList) {
            try {
                preferences.setValue(
                    p.getKey(),
                    resolvePortletPrefValue(p.getKey(), p.getValue(), portlet, companyId, groupId)
                );
            } catch (ReadOnlyException e) {
                LOG.error(
                    "Portlet preferences (" +
                    p.getKey() +
                    ", " +
                    p.getValue() +
                    ") of " +
                    "wrappedPortlet " +
                    portlet.getPortletId() +
                    " caused an excpetion! "
                );
            }
        }
        PortletPreferencesLocalServiceUtil.updatePreferences(ownerId, ownerType, plid, portletIdInc, preferences);

        if (Validator.isNotNull(column) && Validator.isNotNull(portletIdInc)) {
            layoutTypePortlet.movePortletId(runAsUserId, portletIdInc, column, portlet.getColumnPosition());
        }
        LayoutLocalServiceUtil.updateLayout(
            layout.getGroupId(),
            layout.isPrivateLayout(),
            layout.getLayoutId(),
            layout.getTypeSettings()
        );

        if (portlet.getRolePermissions() != null) {
            LOG.info(" i portlet rights");
            updatePortletDisplayPermissions(
                portlet,
                portletIdInc,
                layout,
                page,
                companyId,
                groupId,
                portlet.getRolePermissions(),
                getDefaultPortletPermissions(layout.isPrivateLayout())
            );
        }

        LOG.info(" i portlet added.");
    }

    public static void updatePortletDisplayPermissions(
        PagePortlet portlet,
        String portletIdInc,
        Layout layout,
        Page page,
        long companyId,
        long groupId,
        RolePermissions rolePermissions,
        Map<String, List<String>> defaultPermissions
    ) {
        List<Object> primaryKeys = new ArrayList<Object>();
        try {
            String portlet_on_layout_id = layout.getPlid() + "_LAYOUT_" + portletIdInc;
            primaryKeys.add(portlet_on_layout_id);
            //			ResourcePermissionLocalServiceImpl
            SetupPermissions.updatePermission(
                String.format("Portlet %1$s on page %2$s", portlet_on_layout_id, page.getFriendlyUrl()),
                companyId,
                primaryKeys,
                portlet.getPortletId(),
                rolePermissions,
                defaultPermissions
            );
            LOG.info(" Permissions for " + portletIdInc + " changed: " + defaultPermissions);
        } catch (Exception e) {
            LOG.error(" Permissions for " + portletIdInc + " could not be changed. ", e);
        }
    }

    /**
     * Substitutes parameters in porlet preferences. Possible values are:
     * <ul>
     * <li>{{$ID_OF_SITE_WITH_NAME= &lt; name of the site/group &gt;}}</li>
     * <li>{{$ART-TEMPLATE-UUID-BY-KEY= &lt; value of article template key &gt;
     * }}</li>
     * <li>{{$ART-STRUCTURE-UUID-BY-KEY=&lt; value of article structure key &gt;
     * }}</li>
     * <li>{{$ART-TEMPLATE-ID-BY-KEY= &lt; value of article template key &gt; }}
     * </li>
     * <li>{{$ART-STRUCTURE-ID-BY-KEY=&lt; value of article structure key &gt;
     * }}</li>
     * <li>{{$ADT-TEMPLATE-ID-BY-KEY= &lt; value of ADT template key &gt; }}
     * </li>
     * <li>{{$ADT-TEMPLATE-UUID-BY-KEY= &lt; value of ADT template key &gt; }}
     * </li>
     * <li>{{$FILE= [value of the site scope in for of ::SITENAME::] &lt; value
     * of path and title of the refered document &gt; }}</li>
     * </ul>
     *
     * @param key The wrappedPortlet key.
     * @param value The defined value which should be parametrized.
     * @param wrappedPortlet The pageportlet definition.
     * @param company Id of the company.
     * @param groupId The group id.
     *
     * @return
     */
    private static String resolvePortletPrefValue(
        final String key,
        final String value,
        final PagePortlet portlet,
        final long company,
        final long groupId
    ) {
        String locationHint = String.format("Key: %1$s of wrappedPortlet %2$s", key, portlet.getPortletId());
        return ResolverUtil.lookupAll(groupId, company, value, locationHint);
    }

    public static void setLayoutTemplate(final Layout layout, final Page page, final long userid) {
        if (layout.getLayoutType() instanceof LayoutTypePortlet) {
            LayoutTypePortlet portletLayout = (LayoutTypePortlet) layout.getLayoutType();

            if (page.isClearPage()) {
                if (
                    page.getPagePortlet() != null &&
                    !page.getPagePortlet().isEmpty() &&
                    page.getLinkToUrl() != null &&
                    !page.getLinkToUrl().equals("")
                ) {
                    LOG.error(
                        "This is a link page! It cannot be cleared. If you intend to use this page for portlets, please delete this page, or remove the link from the page!"
                    );
                } else {
                    removeAllPortlets(userid, portletLayout, layout);
                }
            }
            String themeId = null;
            try {
                if (!Validator.isBlank(page.getLayoutThemeId())) {
                    themeId = page.getLayoutThemeId();
                }
                LayoutTemplate layoutTemplate = LayoutTemplateLocalServiceUtil.getLayoutTemplate(
                    page.getLayout(),
                    false,
                    themeId
                );

                if (layoutTemplate != null) {
                    LOG.info(String.format("Setting layout to %1$s for page %2$s", page.getLayout(), page.getName()));
                    if (themeId != null) {
                        LOG.info(String.format(LAYOUT_WAS_LOOKED_UP_IN_THEME, themeId));
                    }
                    portletLayout.setLayoutTemplateId(
                        UserLocalServiceUtil.getDefaultUserId(layout.getCompanyId()),
                        layoutTemplate.getLayoutTemplateId()
                    );
                    LayoutLocalServiceUtil.updateLayout(
                        layout.getGroupId(),
                        layout.isPrivateLayout(),
                        layout.getLayoutId(),
                        layout.getTypeSettings()
                    );
                } else {
                    LOG.error(String.format("Layout template %1$s not found !", page.getLayout()));
                    if (themeId != null) {
                        LOG.error(String.format(LAYOUT_WAS_LOOKED_UP_IN_THEME, themeId));
                    }
                }
            } catch (Exception e) {
                LOG.error(String.format("Error by setting layout template : %1$s", page.getLayout()), e);
                if (themeId != null) {
                    LOG.error(String.format(LAYOUT_WAS_LOOKED_UP_IN_THEME, themeId));
                }
            }
        }
    }

    private static void removeAllPortlets(
        final long runasUser,
        final LayoutTypePortlet layoutTypePortlet,
        final Layout layout
    ) {
        List<Portlet> portlets = null;
        try {
            portlets = layoutTypePortlet.getAllPortlets();
        } catch (SystemException e1) {
            LOG.error(e1);
        }
        if (portlets != null) {
            for (Portlet portlet : portlets) {
                String portletId = portlet.getPortletId();

                try {
                    if (layoutTypePortlet.hasPortletId(portletId)) {
                        LOG.debug(String.format("Removing wrappedPortlet %1$s", portletId));
                        layoutTypePortlet.removePortletId(runasUser, portletId);
                        String rootPortletId = PortletIdCodec.decodePortletName(portletId);
                        LOG.debug(String.format("Root portletId: %1$s", rootPortletId));
                        ResourceLocalServiceUtil.deleteResource(
                            layout.getCompanyId(),
                            rootPortletId,
                            ResourceConstants.SCOPE_INDIVIDUAL,
                            PortletPermissionUtil.getPrimaryKey(layout.getPlid(), portletId)
                        );
                        LayoutLocalServiceUtil.updateLayout(
                            layout.getGroupId(),
                            layout.isPrivateLayout(),
                            layout.getLayoutId(),
                            layout.getTypeSettings()
                        );
                        List<PortletPreferences> list = PortletPreferencesLocalServiceUtil.getPortletPreferences(
                            PortletKeys.PREFS_OWNER_TYPE_LAYOUT,
                            layout.getPlid(),
                            portletId
                        );
                        for (PortletPreferences p : list) {
                            PortletPreferencesLocalServiceUtil.deletePortletPreferences(p);
                        }
                    }
                } catch (PortalException | SystemException e) {
                    LOG.error(e);
                }
            }
        }
    }

    private static void deletePages(final long groupId, boolean privatePages) {
        ServiceContext serviceContext = new ServiceContext();
        try {
            LayoutLocalServiceUtil.deleteLayouts(groupId, privatePages, serviceContext);
            LOG.info("Setup: Pages removed.");
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("cannot remove pages: %1$s", e));
        }
    }

    public static void setupSitePortlets(Site site, long groupId) {
        if (site.getPrivatePages() != null) {
            for (Page p : site.getPrivatePages().getPage()) {
                setupPagePortlets(p, groupId);
            }
        }
        if (site.getPublicPages() != null) {
            for (Page p : site.getPublicPages().getPage()) {
                setupPagePortlets(p, groupId);
            }
        }
    }

    private static void setupPagePortlets(Page page, long groupId) {
        for (PagePortlet p : page.getPagePortlet()) {
            if (p instanceof PortletWithRuntimeData) {
                addDeferredPortlet((PortletWithRuntimeData) p);
            } else {
                LOG.error("Not implemented / only deferred portlet addition!");
            }
        }
        if (false == page.getPage().isEmpty()) {
            for (Page sub : page.getPage()) {
                setupPagePortlets(sub, groupId);
            }
        }
    }

    private static void addDeferredPortlet(PortletWithRuntimeData wrap) {
        try {
            if (wrap.wrappedPortlet instanceof ArticleDisplayPortlet) {
                addArticleDisplayPortletIntoPage(
                    wrap.page,
                    wrap.layout,
                    (ArticleDisplayPortlet) wrap.wrappedPortlet,
                    wrap.company,
                    wrap.groupId
                );
            } else if (wrap.wrappedPortlet instanceof AssetPublisherPortlet) {
                addAssetPublisherPortletIntoPage(
                    wrap.page,
                    wrap.layout,
                    (AssetPublisherPortlet) wrap.wrappedPortlet,
                    wrap.company,
                    wrap.groupId
                );
            } else if (wrap.wrappedPortlet instanceof MenuViewPortlet) {
                addMenuViewPortletIntoPage(
                    wrap.page,
                    wrap.layout,
                    (MenuViewPortlet) wrap.wrappedPortlet,
                    wrap.company,
                    wrap.groupId
                );
            } else {
                addGenericPortletIntoPage(wrap.page, wrap.layout, wrap.wrappedPortlet, wrap.company, wrap.groupId);
            }
        } catch (Exception e) {
            LOG.error("Cannot add pre-wrapped portlet:[" + wrap + "]", e);
        }
    }

    private static void addRuntimeInfo(
        List<PortletWithRuntimeData> deferredAdd,
        PagePortlet portlet,
        Page page,
        Layout layout,
        long company,
        long groupId
    ) {
        PortletWithRuntimeData p = new PortletWithRuntimeData();
        p.wrappedPortlet = portlet;
        p.page = page;
        p.layout = layout;
        p.company = company;
        p.groupId = groupId;
        deferredAdd.add(p);
    }

    // TODO: better implement it.. maybe later.
    private static class PortletWithRuntimeData extends PagePortlet {
        public long groupId;
        public long company;
        public Layout layout;
        public Page page;
        private PagePortlet wrappedPortlet;
    }

    public static final Map<String, String> CONFIG_MENU_VIEW_PREFERENCES = new HashMap<String, String>();

    private static void configure_CONFIG_MENU_VIEW_PREFERENCES() {
        CONFIG_MENU_VIEW_PREFERENCES.put("displayDepth", "0");
        CONFIG_MENU_VIEW_PREFERENCES.put("rootMenuItemType", "absolute");
        CONFIG_MENU_VIEW_PREFERENCES.put("siteNavigationMenuType", "-1");
        CONFIG_MENU_VIEW_PREFERENCES.put("expandedLevels", "auto");
        CONFIG_MENU_VIEW_PREFERENCES.put("rootMenuItemLevel", "0");
    }
}
