package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.DocumentUtil;
import com.ableneo.liferay.portal.setup.core.util.FolderUtil;
import com.ableneo.liferay.portal.setup.core.util.ResourcesUtil;
import com.ableneo.liferay.portal.setup.domain.Document;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SetupDocuments {

    private static final Logger LOG = LoggerFactory.getLogger(SetupDocuments.class);
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS;

    static {
        DEFAULT_PERMISSIONS = new HashMap<>();
        List<String> actionsOwner = new ArrayList<>();

        actionsOwner.add(ActionKeys.ADD_DISCUSSION);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.DELETE_DISCUSSION);
        actionsOwner.add(ActionKeys.OVERRIDE_CHECKOUT);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);
        actionsOwner.add(ActionKeys.VIEW);

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

    private SetupDocuments() {}

    public static void setupSiteDocuments(final Site site, long groupId) {
        for (Document doc : site.getDocument()) {
            String folderPath = doc.getDocumentFolderName();
            String documentName = doc.getDocumentFilename();
            String documentTitle = doc.getDocumentTitle();
            String filenameInFilesystem = doc.getFileSystemName();
            long userId = SetupConfigurationThreadLocal.getRunAsUserId();
            long company = SetupConfigurationThreadLocal.getRunInCompanyId();

            switch (doc.getFileUploadType()) {
                case GENERAL:
                    // groupid == site group (as-is)
                    break;
                case LOGO_IMAGE:
                    // groupid = 'Control Panel' groups id..
                    try {
                        Group controlPanelGroup = GroupLocalServiceUtil.loadGetGroup(
                            company,
                            GroupConstants.CONTROL_PANEL
                        );
                        groupId = controlPanelGroup.getGroupId();
                    } catch (PortalException e) {
                        LOG.error(
                            "Can not get group for " + GroupConstants.CONTROL_PANEL + " and company " + company + ":",
                            e
                        );
                        return;
                    }
                    break;
                default:
                    LOG.error(
                        "Can not understand enum value '" +
                        doc.getFileUploadType() +
                        "' as upload-type.. check dependencies, implement on need!"
                    );
                    return;
            }

            long repoId = groupId;
            FileEntry fe = DocumentUtil.findDocument(documentName, folderPath, groupId, groupId);
            byte[] fileBytes = null;
            try {
                fileBytes = ResourcesUtil.getFileBytes(filenameInFilesystem);
            } catch (IOException e) {
                LOG.error(String.format("Can not read file: %1$s. Skipping file", filenameInFilesystem));
                continue;
            }
            if (fileBytes != null) {
                if (fe == null) {
                    Folder folder = null;
                    if (Validator.isBlank(folderPath)) {
                        folder = FolderUtil.findFolder(groupId, repoId, folderPath, true);
                    } else {
                        try {
                            folder = DLAppLocalServiceUtil.getMountFolder(repoId);
                        } catch (PortalException e) {
                            LOG.warn(
                                "Mount folder not found for file [{}], stopped creating the document.",
                                documentName,
                                e
                            );
                        }
                    }
                    if (folder != null) {
                        LOG.info("{} is not found! It will be created! (c: {},grp: {}", documentName, company, groupId);
                        fe =
                            DocumentUtil.createDocument(
                                groupId,
                                folder.getFolderId(),
                                documentName,
                                documentTitle,
                                userId,
                                repoId,
                                fileBytes
                            );
                    }
                } else {
                    LOG.info(
                        documentName + " is found! Content will be updated! (c:" + company + ",grp:" + groupId + " "
                    );
                    DocumentUtil.updateFile(fe, fileBytes, userId, documentName);
                }
                SetupPermissions.updatePermission(
                    String.format("Document %1$s/%2$s", folderPath, documentName),
                    company,
                    fe.getFileEntryId(),
                    DLFileEntry.class,
                    doc.getRolePermissions(),
                    DEFAULT_PERMISSIONS
                );
            }
        }
    }
}
