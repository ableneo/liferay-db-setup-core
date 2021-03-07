package com.ableneo.liferay.portal.setup.core.util;
//package com.ableneo.liferay.portal.setup.core;
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
//
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
