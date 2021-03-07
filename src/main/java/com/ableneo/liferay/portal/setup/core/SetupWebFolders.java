package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.RolePermissionBuilder;
import com.ableneo.liferay.portal.setup.core.util.WebFolderUtil;
import com.ableneo.liferay.portal.setup.domain.ArticleFolder;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.liferay.journal.model.JournalFolder;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
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
