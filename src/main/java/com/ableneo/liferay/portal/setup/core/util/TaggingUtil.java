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

import java.util.List;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.domain.Article;
import com.ableneo.liferay.portal.setup.domain.Tag;
import com.liferay.asset.kernel.exception.NoSuchTagException;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetTagLocalServiceUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.Validator;

public final class TaggingUtil {
    private static final Log LOG = LogFactoryUtil.getLog(TaggingUtil.class);

    private TaggingUtil() {}

    public static void associateTagsAndCategories(long groupId, Article article, JournalArticle journalArticle)
            throws PortalException {

        List<Tag> tags = article.getTag();
        String[] tagNames = null;
        if (tags != null) {
            tagNames = tags.stream().map(Tag::getName).toArray(String[]::new);
        }

        long[] categoryIds = article
                .getCategory().stream().map(category -> ResolverUtil.lookupAll(groupId, journalArticle.getCompanyId(),
                        category.getId(), article.getPath()))
                .filter(Validator::isNumber).mapToLong(Long::parseLong).toArray();

        AssetEntry entry = AssetEntryLocalServiceUtil.getEntry(JournalArticle.class.getName(),
                journalArticle.getResourcePrimKey());
        AssetEntryLocalServiceUtil.updateEntry(SetupConfigurationThreadLocal.getRunAsUserId(), groupId,
                JournalArticle.class.getName(), entry.getClassPK(), categoryIds, tagNames);
    }

    public static void associateTagsWithJournalArticle(final List<String> tags, final List<String> categories,
            final long userId, final long groupId, final long primaryKey) {

        try {
            long[] catIds = new long[0];
            if (categories != null) {
                catIds = getCategories(categories, groupId, userId);
            }
            AssetEntryLocalServiceUtil.updateEntry(userId, groupId, JournalArticle.class.getName(), primaryKey, catIds,
                    tags.toArray(new String[tags.size()]));
        } catch (PortalException | SystemException e) {
            LOG.error(e);
        }
    }

    public static long[] getCategories(final List<String> categories, final long groupId, final long runAsUser) {
        // The categories and tags to assign
        final long[] assetCategoryIds = new long[categories.size()];

        for (int i = 0; i < categories.size(); ++i) {
            final String name = categories.get(i);

            AssetTag assetTag = null;
            try {
                assetTag = AssetTagLocalServiceUtil.getTag(groupId, name);
            } catch (final NoSuchTagException e) {
                try {
                    assetTag = AssetTagLocalServiceUtil.addTag(runAsUser, groupId, name, new ServiceContext());
                } catch (PortalException | SystemException e1) {
                    LOG.error(String.format("Category %1$s not found! ", name), e1);
                }
            } catch (PortalException | SystemException e) {
                LOG.error(String.format("Category %1$s not found! ", name), e);
            }

            if (assetTag != null) {
                assetCategoryIds[i] = assetTag.getTagId();
            }
        }
        return assetCategoryIds;
    }
}
