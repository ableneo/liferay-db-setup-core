package com.ableneo.liferay.portal.setup.core.util;
//package com.ableneo.liferay.portal.setup.core;//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.Map.Entry;
//
//public class RolePermissionUtil {
//
//
//    // TODO: PermissionUtil..
//    public static HashMap<String, List<String>> getPermissionBase(final boolean isPrivate) {
//    	HashMap<String, List<String>> base = getDefaultPermissions(isPrivate);
//    	return getPermissionClone(base);
//    }
//    // TODO: PermissionUtil..
//    public static HashMap<String, List<String>> getPermissionClone(HashMap<String, List<String>> base) {
//    	HashMap<String, List<String>> res = new HashMap<String, List<String>>();
//    	for (Entry<String, List<String>> e : base.entrySet()) {
//    		res.put(e.getKey(), new ArrayList<String>(e.getValue()));
//    	}
//    	return res;
//    }
//    // TODO: PermissionUtil..
//    public static HashMap<String, List<String>> getPermissionWith(HashMap<String, List<String>> base, List<String> roleNames, List<String> actions) {
//    	if (base == null || roleNames == null || roleNames.isEmpty() || actions == null || actions.isEmpty()) {
//    		return base;
//    	}
//    	HashMap<String, List<String>> res = getPermissionClone(base);
//    	for (String roleName : roleNames) {
//	    	List<String> items = res.get(roleName);
//	    	if (items == null) {
//	    		items = new ArrayList<String>(actions);
//	    		res.put(roleName, items);
//	    	} else {
//	    		Set aid = new HashSet<String>(items);
//	    		aid.addAll(actions);
//	    		res.put(roleName, new ArrayList<String>(aid));
//	    	}
//    	}
//    	return res;
//    }
//
//}
