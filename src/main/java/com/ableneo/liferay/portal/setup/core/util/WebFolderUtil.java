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
/*-
 * #%L
 * com.ableneo.liferay.db.setup.core
 * %%
 * Copyright (C) 2016 - 2021 ableneo s. r. o.
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
        Long parentId = 0L;
        boolean hasCreated = false;
        while (count < folderPath.length) {
            String folder = folderPath[count];
            if (!folder.equals("")) {
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

            folder =
                JournalFolderLocalServiceUtil.addFolder(
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
