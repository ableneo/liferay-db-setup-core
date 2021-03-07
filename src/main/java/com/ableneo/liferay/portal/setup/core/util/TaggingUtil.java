package com.ableneo.liferay.portal.setup.core.util;

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
import java.util.List;

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
            .getCategory()
            .stream()
            .map(
                category ->
                    ResolverUtil.lookupAll(groupId, journalArticle.getCompanyId(), category.getId(), article.getPath())
            )
            .filter(Validator::isNumber)
            .mapToLong(Long::parseLong)
            .toArray();

        AssetEntry entry = AssetEntryLocalServiceUtil.getEntry(
            JournalArticle.class.getName(),
            journalArticle.getResourcePrimKey()
        );
        AssetEntryLocalServiceUtil.updateEntry(
            SetupConfigurationThreadLocal.getRunAsUserId(),
            groupId,
            JournalArticle.class.getName(),
            entry.getClassPK(),
            categoryIds,
            tagNames
        );
    }

    public static void associateTagsWithJournalArticle(
        final List<String> tags,
        final List<String> categories,
        final long userId,
        final long groupId,
        final long primaryKey
    ) {
        try {
            long[] catIds = new long[0];
            if (categories != null) {
                catIds = getCategories(categories, groupId, userId);
            }
            AssetEntryLocalServiceUtil.updateEntry(
                userId,
                groupId,
                JournalArticle.class.getName(),
                primaryKey,
                catIds,
                tags.toArray(new String[tags.size()])
            );
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
