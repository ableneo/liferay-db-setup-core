package com.ableneo.liferay.portal.setup.core.util;

import com.ableneo.liferay.portal.setup.domain.PermissionAction;
import com.ableneo.liferay.portal.setup.domain.RolePermissionType;
import com.ableneo.liferay.portal.setup.domain.RolePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class RolePermissionBuilder {

    private static final Map<String, Map<String, Set<String>>> PRESETS = new HashMap<
        String,
        Map<String, Set<String>>
    >();

    private Map<String, Set<String>> currentlyBuiltPermissions = new HashMap<String, Set<String>>();

    protected RolePermissionBuilder() {}

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

    private RolePermissionType rolePermissionFrom(String key, Set<String> value) {
        RolePermissionType rp = new RolePermissionType();
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
