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

import com.ableneo.liferay.portal.setup.domain.Portal;
import com.ableneo.liferay.portal.setup.domain.PropertyKeyValue;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO: IMPL-ON-NEED
public class SetupPortal {

	private static final Log LOG = LogFactoryUtil.getLog(SetupPortal.class);
	public static void setupPortal(Portal portal) {
		LOG.error("setupPortal is IMPL-ON-NEED");
		
		if (portal == null) {
			return;
		}
		if (portal.getPortalProperties() != null && false == portal.getPortalProperties().isEmpty()) {
			SetupPortal.setPortalProperties(portal.getPortalProperties());
		}

	}

	private static void setPortalProperties(List<PropertyKeyValue> portalProperties) {
		Set<String> keys = new HashSet<String>();
		Map<String, String> values = new  HashMap<String,String>();
		for (PropertyKeyValue kv : portalProperties) {
			keys.add(kv.getKey());
			values.put(kv.getKey(), kv.getValue());
		}
		
////		portal language settings
////		> INSERT INTO PORTALPREFERENCES VALUES(1,20102,20101,1,'<portlet-preferences><preference><name>locales</name><value>en_US,hu_HU</value></preference></portlet-preferences>')
////		PortalPreferences
////		PortalPreferencesLocalServiceUtil.updatePortalPreferences(portalPreferences);
//
////		int count = PortletPreferencesLocalServiceUtil.getPortletPreferencesesCount();
//		List<PortletPreferences> items = PortletPreferencesLocalServiceUtil.getPortletPreferences();
//		for (PortletPreferences item : items) {
//			if (item.get)
//		}
		
	}

}
