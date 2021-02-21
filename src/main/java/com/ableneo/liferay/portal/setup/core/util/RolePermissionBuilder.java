package com.ableneo.liferay.portal.setup.core.util;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ableneo.liferay.portal.setup.domain.PermissionAction;
import com.ableneo.liferay.portal.setup.domain.RolePermission;
import com.ableneo.liferay.portal.setup.domain.RolePermissions;

public class RolePermissionBuilder {

	private static final Map<String, Map<String, Set<String>>> PRESETS = new HashMap<String, Map<String, Set<String>>>();

	private Map<String, Set<String>> currentlyBuiltPermissions = new HashMap<String, Set<String>>();

	protected RolePermissionBuilder() {

	}

	public static RolePermissionBuilder create() {
		return new RolePermissionBuilder();
	}

	public static RolePermissionBuilder create(String presetBase) {
		RolePermissionBuilder builder = new RolePermissionBuilder();

		Map<String, Set<String>> preset = PRESETS.get(presetBase);
		if (preset == null) {
			preset = new HashMap<String, Set<String>>();
		} else {
			preset = clone(preset);
		}
		builder.currentlyBuiltPermissions = preset;
		return builder;
	}

	public boolean isEmpty() {
		return currentlyBuiltPermissions.isEmpty();
	}

	public RolePermissionBuilder add(String role, Collection<String> activity) {
		Set<String> values = currentlyBuiltPermissions.get(role);
		if (values == null) {
			values = new HashSet<String>(activity);
			currentlyBuiltPermissions.put(role, values);
		} else {
			values.addAll(activity);
		}
		return this;
	}

	public RolePermissionBuilder addToAll(Collection<String> activity) {
    	for (Entry<String, Set<String>> e : currentlyBuiltPermissions.entrySet()) {
    		e.getValue().addAll(activity);
    	}
    	return this;
	}

	public RolePermissionBuilder storeAsPreset(String key) {
		PRESETS.put(key, currentlyBuiltPermissions);
		return this;
	}

	public Map<String, List<String>> buildMapList() {
    	HashMap<String, List<String>> res = new HashMap<String, List<String>>();
    	for (Entry<String, Set<String>> e : currentlyBuiltPermissions.entrySet()) {
    		res.put(e.getKey(), new ArrayList<String>(e.getValue()));
    	}
    	return res;
	}

	public RolePermissions buildRolePermissions() {
		RolePermissions res = new RolePermissions();

		for (Entry<String, Set<String>> e : currentlyBuiltPermissions.entrySet()) {
			res.getRolePermission().add(rolePermissionFrom(e.getKey(), e.getValue()));
		}

		return res;
	}

	private RolePermission rolePermissionFrom(String key, Set<String> value) {
		RolePermission rp = new RolePermission();
		rp.setRoleName(key);
		for (String action : value) {
			rp.getPermissionAction().add(permissionActionFrom(action));
		}
		return rp;
	}

	protected static PermissionAction permissionActionFrom(String action) {
		PermissionAction pa = new PermissionAction();
		pa.setActionName(action);
		return pa;
	}

	protected static Map<String, Set<String>> clone(Map<String, Set<String>> base) {
    	HashMap<String, Set<String>> res = new HashMap<String, Set<String>>();
    	for (Entry<String, Set<String>> e : base.entrySet()) {
    		res.put(e.getKey(), new HashSet<String>(e.getValue()));
    	}
    	return res;
    }
}
