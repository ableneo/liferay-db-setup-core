package com.ableneo.liferay.portal.setup.core;

/*-
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2020 ableneo s. r. o.
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

import java.util.List;
import java.util.UUID;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.domain.CustomFieldSetting;
import com.ableneo.liferay.portal.setup.domain.Menu;
import com.ableneo.liferay.portal.setup.domain.MenuItem;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.site.navigation.model.SiteNavigationMenu;
import com.liferay.site.navigation.model.SiteNavigationMenuItem;
import com.liferay.site.navigation.service.SiteNavigationMenuItemLocalServiceUtil;
import com.liferay.site.navigation.service.SiteNavigationMenuLocalServiceUtil;

/**
 * Created by gustavnovotny on 28.08.17.
 */
public class SetupMenus {

	private static final Log LOG = LogFactoryUtil.getLog(SetupMenus.class);

	private SetupMenus() {

	}


	public static void setMenus(long groupId, List<Menu> menus) {

		if (menus != null) {
			for (Menu menu : menus) {
				setMenu(groupId, menu);
			}
		}
//		SiteNavigationMenu savedMenu = SiteNavigationMenuLocalServiceUtil.addSiteNavigationMenu(
//				userId, groupId, name, serviceContext);
//		SiteNavigationMenuItem savedItem = SiteNavigationMenuItemLocalServiceUtil.addSiteNavigationMenuItem(
//				userId, groupId, savedMenu.getPrimaryKey(), 0, type, /*order,*/ typeSettings, serviceContext);
//		savedItem.getExpandoBridge().setAttribute(fieldName,  fieldValue, false);
	}


	private static void setMenu(long groupId, Menu newMenu) {
		if (newMenu == null) {
			LOG.error("NULL menu in menu list! Skip.");
			return;
		}
		long userId = SetupConfigurationThreadLocal.getRunAsUserId();
		ServiceContext serviceContext = new ServiceContext();

		// TODO: check name..
		String name = newMenu.getName();
		LOG.info("Adding menu '"+name+"'");

		List<SiteNavigationMenu> existingMenus = SiteNavigationMenuLocalServiceUtil.getSiteNavigationMenus(groupId);
		SiteNavigationMenu menu = null;
		for (SiteNavigationMenu existingMenu : existingMenus) {
			if (name.equals(existingMenu.getName())) {
				LOG.info(" i '"+name+"' already there");
				menu = existingMenu;
				break;
			}
		}

		if (menu != null) {
			if (newMenu.isClearBeforehand()) {
				try {
					LOG.info(" i '"+name+"' clear-items..");
					SiteNavigationMenuItemLocalServiceUtil.deleteSiteNavigationMenuItems(menu.getPrimaryKey());
				} catch (Exception e) {
					LOG.error("Could not clear existing menu items of '"+name+"'", e);
					return;
				}
//				try {
//					LOG.info(" i '"+name+"' clear-menu..");
//					SiteNavigationMenuLocalServiceUtil.deleteSiteNavigationMenu(menu.getPrimaryKey());
//				} catch (Exception e) {
//					LOG.error("Could not clear existing menu '"+name+"'", e);
//					return;
//				}
			}
		}
		if (menu == null) {
			LOG.info(" i '"+name+"' create..");
			try {
				menu = SiteNavigationMenuLocalServiceUtil.addSiteNavigationMenu(userId, groupId, name, serviceContext);
				LOG.info(" i '"+name+"' create OK");
			} catch (PortalException e) {
				LOG.error("Could not create new menu '"+name+"'", e);
				return;
			}
		}

		createMenuItems(groupId, newMenu, userId, serviceContext, menu);

	}

	private static void createMenuItems(long groupId, Menu newMenu, long userId, ServiceContext serviceContext,
			SiteNavigationMenu menu) {
		LOG.info("Adding menu-items for '"+menu.getName()+"'");
		long parentMenuId = menu.getPrimaryKey();
		long parentMenuItem = 0L;
//		List<SiteNavigationMenuItem> currentItems = SiteNavigationMenuItemLocalServiceUtil.getSiteNavigationMenuItems(parentMenuId);
		for (MenuItem newMenuItem : newMenu.getMenuItem()) {
			SiteNavigationMenuItem menuItem = null;//getExistingFromList(newMenuItem, currentItems);
//			if (menuItem == null) {
				menuItem = createMenuItem(groupId, userId, serviceContext, parentMenuId, parentMenuItem, newMenuItem);
//			} else {
//				LOG.info(" i menu-item '"+menu.getName()+"/"+newMenuItem.getName()+"' already there");
//			}
			if (menuItem != null && false == newMenuItem.getMenuItem().isEmpty()) {
				createMenuItems(groupId, userId, serviceContext, menu, menuItem, newMenuItem);
			}
		}
	}

	private static void createMenuItems(long groupId, long userId, ServiceContext serviceContext,
			SiteNavigationMenu parentMenu, SiteNavigationMenuItem parentMenuItem, MenuItem addFromMenuItems) {
		String parentName = parentMenuItem.getName();
		LOG.info(" i creating sub-menu-items under '"+parentName+"'");
		long parentMenuId = parentMenu.getPrimaryKey();
		long parentMenuItemId = parentMenuItem.getPrimaryKey();
//		List<SiteNavigationMenuItem> currentItems = SiteNavigationMenuItemLocalServiceUtil.getSiteNavigationMenuItems(parentMenuId, parentMenuItemId);
		for (MenuItem newMenuItem : addFromMenuItems.getMenuItem()) {
//			SiteNavigationMenuItem menuItem = getExistingFromList(newMenuItem, currentItems);
			SiteNavigationMenuItem menuItem = null;
			//if (menuItem == null) {
				menuItem = createMenuItem(groupId, userId, serviceContext, parentMenuId, parentMenuItemId, newMenuItem);
			//} else {
			//	LOG.info(" i item '"+parentName+"/"+newMenuItem.getName()+"' already there");
			//}
			if (menuItem != null && false == newMenuItem.getMenuItem().isEmpty()) {
				createMenuItems(groupId, userId, serviceContext, parentMenu, menuItem, newMenuItem);
			}
		}
	}

	private static SiteNavigationMenuItem createMenuItem(long groupId, long userId, ServiceContext serviceContext, long parentMenuId,
			long parentMenuItem, MenuItem newMenuItem) {

		// SiteNavigationMenuItemTypeConstants: layout(==page..), node, url
		String type = newMenuItem.getType();
		if (false == newMenuItem.getMenuItem().isEmpty()) {
			type = "node";
		} else if (type == null || "".equals(type)) {
			type = "url";
		} else {
			type = "url";
		}

		LOG.info(" i setting item '"+newMenuItem.getName()+"' as '"+type+"': ["+newMenuItem.getTypeSettings()+"]");

		// 'layout','groupId=20121 ; layoutUuid=accf0b5c-800f-d049-01bc-0a706e3fad15 ; privateLayout=false ; title=Search ; '
		//defaultLanguageId=en_US ; name_en_US=tarsoldalak ;
		String typeSettings = newMenuItem.getTypeSettings();
		// LayoutSiteNavigationMenuItemType => page
		// NodeSiteNavigationMenuItemType => submenu
		// URLSiteNavigationMenuItemType => url link



		SiteNavigationMenuItem savedItem = null;
		try {
			List<SiteNavigationMenuItem> items = SiteNavigationMenuItemLocalServiceUtil.getSiteNavigationMenuItems(parentMenuId, parentMenuItem);
			for (SiteNavigationMenuItem item : items) {
				if (item.getName().equals(newMenuItem.getName())) {
					LOG.info("existing menu-item["+newMenuItem.getName()+"]");
					savedItem = item;
					break;
				}
			}
		} catch (Exception e) {
			LOG.error("Can not list/find existing menu item '"+newMenuItem.getName()+"', CREATE NEW : ", e);
		}
		if (savedItem == null) {
			try {
				serviceContext.setUuid(UUID.randomUUID().toString());
				LOG.info("Inserting menu-item["+newMenuItem.getName()+"]");
				savedItem = SiteNavigationMenuItemLocalServiceUtil.addSiteNavigationMenuItem(
						userId, groupId, parentMenuId, parentMenuItem, type, /*order,*/ typeSettings, serviceContext);
			} catch (PortalException e) {
				LOG.error("Can not add menu item '"+newMenuItem.getName()+"' : ", e);
				return null;
			}
		}
		try {
			LOG.info("Updating menu-item["+newMenuItem.getName()+"] @"+savedItem.getPrimaryKey()+"");
			savedItem.setName(newMenuItem.getName());
			savedItem.setType(type);
			savedItem.setTypeSettings(typeSettings);
			savedItem = SiteNavigationMenuItemLocalServiceUtil.updateSiteNavigationMenuItem(savedItem);
		} catch (Exception e) {
			LOG.error("Can not update menu item '"+newMenuItem.getName()+"' : ", e);
			return null;
		}
		for (CustomFieldSetting cfs : newMenuItem.getCustomFieldSetting()) {
			savedItem.getExpandoBridge().setAttribute(cfs.getKey(),  cfs.getValue(), false);
		}

		return savedItem;
	}

	private static SiteNavigationMenuItem getExistingFromList(MenuItem newMenuItem, List<SiteNavigationMenuItem> currentItems) {
		if (newMenuItem == null || currentItems.isEmpty()) {
			return null;
		}
		for (SiteNavigationMenuItem item : currentItems) {
			if (newMenuItem.getName().equals(item.getName())) {
				return item;
			}
		}
		return null;
	}


}
