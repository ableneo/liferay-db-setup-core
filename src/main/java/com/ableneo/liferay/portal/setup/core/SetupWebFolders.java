package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.RolePermissionBuilder;
import com.ableneo.liferay.portal.setup.core.util.WebFolderUtil;
import com.ableneo.liferay.portal.setup.domain.ArticleFolder;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.journal.model.JournalFolder;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SetupWebFolders {
    private static final Map<String, List<String>> DEFAULT_PERMISSIONS;
    public static final String RP_KEY = SetupWebFolders.class.getSimpleName() + "_DEFAULT_PERMISSIONS";

    static {
        RolePermissionBuilder builder = RolePermissionBuilder.create();

        List<String> actionsOwner = new ArrayList<>();

        actionsOwner.add(ActionKeys.VIEW);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.ADD_SUBFOLDER);
        actionsOwner.add(ActionKeys.ADD_ARTICLE);
        actionsOwner.add(ActionKeys.SUBSCRIBE);
        actionsOwner.add(ActionKeys.ACCESS);
        builder.add(RoleConstants.OWNER, actionsOwner);

        List<String> actionsUser = new ArrayList<>();
        actionsUser.add(ActionKeys.VIEW);
        builder.add(RoleConstants.USER, actionsUser);

        List<String> actionsGuest = new ArrayList<>();
        actionsGuest.add(ActionKeys.VIEW);
        builder.add(RoleConstants.GUEST, actionsGuest);

        List<String> actionsViewer = new ArrayList<>();
        actionsViewer.add(ActionKeys.VIEW);
        builder.add(RoleConstants.SITE_MEMBER, actionsViewer);

        builder.storeAsPreset(RP_KEY);
        DEFAULT_PERMISSIONS = builder.buildMapList();
    }

    private SetupWebFolders() {}

    public static Map<String, List<String>> getDefaultPermissions() {
        return DEFAULT_PERMISSIONS;
    }

    public static void setupWebFolders(final Site group, final long groupId) {
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();

        for (ArticleFolder af : group.getArticleFolder()) {
            JournalFolder jf = WebFolderUtil.findArticleWebFolder(af, groupId);

            WebFolderUtil.updateFolderPermissions(jf, companyId, groupId, DEFAULT_PERMISSIONS, af.getRolePermissions());
        }
    }
}
