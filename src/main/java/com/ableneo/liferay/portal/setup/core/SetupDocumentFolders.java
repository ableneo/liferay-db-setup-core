package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.FolderUtil;
import com.ableneo.liferay.portal.setup.domain.DocumentFolder;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class SetupDocumentFolders {
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS;

    static {
        DEFAULT_PERMISSIONS = new HashMap<>();
        List<String> actionsOwner = new ArrayList<>();

        actionsOwner.add(ActionKeys.VIEW);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.ADD_SUBFOLDER);
        actionsOwner.add(ActionKeys.ADD_SHORTCUT);
        actionsOwner.add(ActionKeys.ADD_DOCUMENT);
        actionsOwner.add(ActionKeys.ACCESS);
        actionsOwner.add(ActionKeys.SUBSCRIBE);

        DEFAULT_PERMISSIONS.put(RoleConstants.OWNER, actionsOwner);

        List<String> actionsUser = new ArrayList<>();
        actionsUser.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.USER, actionsUser);

        List<String> actionsGuest = new ArrayList<>();
        actionsGuest.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.GUEST, actionsGuest);

        List<String> actionsViewer = new ArrayList<>();
        actionsViewer.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.SITE_MEMBER, actionsViewer);
    }

    private SetupDocumentFolders() {}

    public static void setupDocumentFolders(final Site group, final long groupId) {
        for (DocumentFolder df : group.getDocumentFolder()) {
            boolean create = df.isCreateIfNotExists();
            String folderName = df.getFolderName();

            long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
            Folder folder = FolderUtil.findFolder(groupId, groupId, folderName, create);
            SetupPermissions.updatePermission(
                String.format("Document folder %1$s", folderName),
                companyId,
                folder.getFolderId(),
                DLFolder.class,
                df.getRolePermissions(),
                DEFAULT_PERMISSIONS
            );
        }
    }
}
