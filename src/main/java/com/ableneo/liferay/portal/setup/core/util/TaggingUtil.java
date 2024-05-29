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
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class TaggingUtil {
    private static final Logger LOG = LoggerFactory.getLogger(TaggingUtil.class);

    private TaggingUtil() {}

    public static void associateTagsAndCategories(long groupId, Article article, JournalArticle journalArticle) {
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
                    ResolverUtil.lookupAll(
                        groupId,
                        journalArticle.getCompanyId(),
                        category.getUuid(),
                        article.getPath()
                    )
            )
            .filter(Validator::isNumber)
            .mapToLong(Long::parseLong)
            .toArray();

        try {
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
        } catch (PortalException e) {
            LOG.warn("Unable to associate tags ({}) and categories ({}) with article: {}", tagNames, categoryIds, article.getTitle(), e);
        }
    }

}
