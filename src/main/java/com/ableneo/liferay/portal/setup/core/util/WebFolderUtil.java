package com.ableneo.liferay.portal.setup.core.util;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.SetupPermissions;
import com.ableneo.liferay.portal.setup.core.SetupWebFolders;
import com.ableneo.liferay.portal.setup.domain.ArticleFolder;
import com.ableneo.liferay.portal.setup.domain.RolePermissions;
import com.liferay.journal.model.JournalFolder;
import com.liferay.journal.service.JournalFolderLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WebFolderUtil {

    private static final Log LOG = LogFactoryUtil.getLog(WebFolderUtil.class);

    private WebFolderUtil() {}

    public static JournalFolder findWebFolder(
        final long companyId,
        final long groupId,
        final long userId,
        final String name,
        final String description,
        final boolean createIfNotExists,
        RolePermissions rolePermissions
    ) {
        String[] folderPath = name.split("/");
        JournalFolder foundFolder = null;
        int count = 0;
        long parentId = 0L;
        boolean hasCreated = false;
        while (count < folderPath.length) {
            String folder = folderPath[count];
            if (!Validator.isBlank(folder)) {
                foundFolder = findWebFolder(groupId, parentId, folder);

                if (foundFolder == null && createIfNotExists) {
                    foundFolder = createWebFolder(userId, companyId, groupId, parentId, folder, description);
                    hasCreated = true;
                }

                if (foundFolder == null) {
                    break;
                }
                parentId = foundFolder.getFolderId();
            }
            count++;
        }
        if (hasCreated) {
            updateFolderPermissions(
                foundFolder,
                companyId,
                groupId,
                SetupWebFolders.getDefaultPermissions(),
                rolePermissions
            );
        }
        return foundFolder;
    }

    public static JournalFolder findWebFolder(final Long groupId, final Long parentFolderId, final String name) {
        JournalFolder dir = null;
        List<JournalFolder> dirs;
        try {
            dirs = JournalFolderLocalServiceUtil.getFolders(groupId, parentFolderId);
            for (JournalFolder jf : dirs) {
                if (jf.getName() != null && jf.getName().equals(name)) {
                    dir = jf;
                    break;
                }
            }
        } catch (SystemException e) {
            LOG.error(e);
        }
        return dir;
    }

    public static JournalFolder createWebFolder(
        final long userId,
        final long companyId,
        final long groupId,
        final long parentFolderId,
        final String name,
        final String description
    ) {
        JournalFolder folder = null;
        try {
            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setScopeGroupId(groupId);
            serviceContext.setCompanyId(companyId);

            folder = JournalFolderLocalServiceUtil.addFolder(
                null,
                userId,
                groupId,
                parentFolderId,
                name,
                description,
                serviceContext
            );
        } catch (PortalException e) {
            LOG.error(e);
        }
        return folder;
    }

    public static JournalFolder findArticleWebFolder(ArticleFolder af, long groupId) {
        String webFolderPath = af.getFolderPath();
        String description = af.getDescription();
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        JournalFolder jf = WebFolderUtil.findWebFolder(
            companyId,
            groupId,
            SetupConfigurationThreadLocal.getRunAsUserId(),
            webFolderPath,
            description,
            true,
            af.getRolePermissions()
        );
        return jf;
    }

    public static void updateFolderPermissions(
        JournalFolder jf,
        long companyId,
        long groupId,
        Map<String, List<String>> webFolderDefaultPermissions,
        RolePermissions rolePermissions
    ) {
        List<Object> ownAndParentIds;
        try {
            ownAndParentIds = new ArrayList<Object>(jf.getAncestorFolderIds());
            ownAndParentIds.add(jf.getFolderId());
            SetupPermissions.updatePermission(
                String.format("Folder %1$s", jf.getName()),
                companyId,
                ownAndParentIds,
                JournalFolder.class,
                rolePermissions,
                webFolderDefaultPermissions
            );
            LOG.info(" Permissions for " + ownAndParentIds + " changed: " + webFolderDefaultPermissions);
        } catch (PortalException e) {
            LOG.error(" Permissions for " + jf.getName() + " could not be changed. ", e);
        }
    }
}
