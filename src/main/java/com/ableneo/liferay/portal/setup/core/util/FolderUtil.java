package com.ableneo.liferay.portal.setup.core.util;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.SetupPermissions;
import com.ableneo.liferay.portal.setup.core.SetupWebFolders;
import com.liferay.document.library.kernel.exception.NoSuchFolderException;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.journal.model.JournalFolder;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.service.ServiceContext;

public final class FolderUtil {
    private static final Log LOG = LogFactoryUtil.getLog(FolderUtil.class);

    private FolderUtil() {}

    public static Folder findFolder(
        final long groupId,
        final long repoId,
        final String name,
        final boolean createIfNotExists
    ) {
        String[] folderPath = name.split("/");
        Folder foundFolder = null;
        int count = 0;
        Long parentId = 0L;
        while (count < folderPath.length) {
            String folder = folderPath[count];
            if (!folder.equals("")) {
                foundFolder = findFolder(groupId, parentId, folder);

                if (foundFolder == null && createIfNotExists) {
                    foundFolder = createDocumentFolder(groupId, repoId, parentId, folder);
                    long company = SetupConfigurationThreadLocal.getRunInCompanyId();

                    if (foundFolder != null) {
                        final String locationHint = String.format(
                            "Folder %1$s, creating folder segment %2$s",
                            name,
                            folder
                        );
                        SetupPermissions.updatePermission(
                            locationHint,
                            company,
                            foundFolder.getFolderId(),
                            JournalFolder.class,
                            null,
                            SetupWebFolders.getDefaultPermissions()
                        );
                    }
                }

                if (foundFolder == null) {
                    break;
                }
                parentId = foundFolder.getFolderId();
            }
            count++;
        }
        return foundFolder;
    }

    public static Folder findFolder(final Long groupId, final Long parentFolderId, final String name) {
        Folder dir = null;
        try {
            dir = DLAppLocalServiceUtil.getFolder(groupId, parentFolderId, name);
        } catch (NoSuchFolderException nsfe) {
            LOG.info(String.format("Folder not found: %1$s", name));
        } catch (PortalException e) {
            LOG.error(String.format("Error while trying to get folder: %1$s", name), e);
        }
        return dir;
    }

    public static Folder createDocumentFolder(
        final long groupId,
        final long repoId,
        final Long pFolderId,
        final String folderName
    ) {
        Folder folder = null;

        try {
            folder = findFolder(groupId, pFolderId, folderName);
            if (folder == null) {
                folder =
                    DLAppLocalServiceUtil.addFolder(
                        null,
                        SetupConfigurationThreadLocal.getRunAsUserId(),
                        repoId,
                        pFolderId,
                        folderName,
                        folderName,
                        new ServiceContext()
                    );
            }
        } catch (SystemException | PortalException e) {
            LOG.error(e);
        }

        return folder;
    }
}
