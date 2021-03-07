package com.ableneo.liferay.portal.setup.core;

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
        Map<String, String> values = new HashMap<String, String>();
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
