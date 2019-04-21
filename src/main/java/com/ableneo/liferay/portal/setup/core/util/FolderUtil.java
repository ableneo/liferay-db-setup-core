package com.ableneo.liferay.portal.setup.core.util;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Original work Copyright (C) 2016 - 2018 mimacom ag
 * Modified work Copyright (C) 2018 - 2020 ableneo, s. r. o.
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

    private FolderUtil() {

    }

    public static Folder findFolder(final long groupId, final long repoId, final String name,
            final boolean createIfNotExists) {
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
                        final String locationHint = String.format("Folder %1$s, creating folder segment %2$s", name, folder);
                        SetupPermissions.updatePermission(locationHint,
                                groupId, company, foundFolder.getFolderId(), JournalFolder.class, null,
                                SetupWebFolders.DEFAULT_PERMISSIONS);
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
            LOG.info("Folder not found: " + name);
        } catch (PortalException e) {
            LOG.error("Error while trying to get folder: " + name, e);
        } catch (SystemException e) {
            LOG.error("Error while trying to get folder: " + name, e);
        }
        return dir;
    }

    public static Folder createDocumentFolder(final long groupId, final long repoId, final Long pFolderId,
            final String folderName) {
        Folder folder = null;

        try {
            folder = findFolder(groupId, pFolderId, folderName);
            if (folder == null) {
                folder = DLAppLocalServiceUtil.addFolder(SetupConfigurationThreadLocal.getRunAsUserId(), repoId,
                        pFolderId, folderName, folderName, new ServiceContext());

            }
        } catch (SystemException | PortalException e) {
            LOG.error(e);
        }

        return folder;
    }

}
