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
package com.ableneo.liferay.portal.setup.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.ResolverUtil;
import com.ableneo.liferay.portal.setup.core.util.TranslationMapUtil;
import com.ableneo.liferay.portal.setup.domain.AssociatedAssetType;
import com.ableneo.liferay.portal.setup.domain.Category;
import com.ableneo.liferay.portal.setup.domain.Vocabulary;
import com.liferay.asset.kernel.exception.NoSuchVocabularyException;
import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portlet.asset.util.AssetVocabularySettingsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Setup module for creating / updating the categorization. So far it creates
 * tree of categories. In the future also AssetTag creation feature should be
 * here.
 * <p/>
 * Created by guno on 8. 6. 2015.
 * @author guno
 */
public final class SetupCategorization {
    private static final Logger LOG = LoggerFactory.getLogger(SetupCategorization.class);

    private SetupCategorization() {}

    public static void setupVocabularies(final Iterable<Vocabulary> vocabularies, final long groupId) {
        Locale siteDefaultLocale = LocaleUtil.getSiteDefault();

        LOG.info("Setting up vocabularies");

        for (Vocabulary vocabulary : vocabularies) {
            setupVocabulary(vocabulary, groupId, siteDefaultLocale);
        }
    }

    private static void setupVocabulary(final Vocabulary vocabulary, final long groupId, final Locale defaultLocale) {

        LOG.info("Setting up vocabulary [{}]", vocabulary.getName());

        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(vocabulary.getTitleTranslation(), groupId,
                vocabulary.getName(), "");

        Map<Locale, String> descMap = new HashMap<>();
        descMap.put(defaultLocale, vocabulary.getDescription());

        AssetVocabulary assetVocabulary = null;

        try {
            assetVocabulary = AssetVocabularyLocalServiceUtil
                .getGroupVocabulary(groupId, StringUtil.toLowerCase(vocabulary.getName().trim()));
        } catch (NoSuchVocabularyException e) {
            LOG.trace("", e);
            LOG.info("Asset vocabulary: [{}] was not found", vocabulary.getName());
        } catch (PortalException e) {
            LOG.error("Error while fetching asset vocabulary: [{}]", vocabulary.getName(), e);
        }

        if (assetVocabulary != null) {
            LOG.debug("Vocabulary [{}] already exists. Will be updated.", assetVocabulary.getName());

            assetVocabulary.setTitleMap(titleMap);
            assetVocabulary.setDescriptionMap(descMap);
            assetVocabulary.setSettings(composeVocabularySettings(vocabulary, groupId));

            try {
                assetVocabulary = AssetVocabularyLocalServiceUtil.updateAssetVocabulary(assetVocabulary);
                LOG.debug("Vocabulary [{}] successfully updated.", assetVocabulary.getName());
            } catch (RuntimeException e) {
                LOG.info("Error while trying to update AssetVocabulary with ID: {}. Skipping.",
                        assetVocabulary.getVocabularyId(), e);
                return;
            }

            setupCategories(assetVocabulary.getVocabularyId(), groupId, 0L, vocabulary.getCategory(), defaultLocale);
            return;
        }

        try {
            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(SetupConfigurationThreadLocal.getRunInCompanyId());
            serviceContext.setScopeGroupId(groupId);
            assetVocabulary = AssetVocabularyLocalServiceUtil.addVocabulary(
                    SetupConfigurationThreadLocal.getRunAsUserId(), groupId, vocabulary.getName(), vocabulary.getName(),
                    titleMap, descMap, composeVocabularySettings(vocabulary, groupId), serviceContext);
            LOG.info("AssetVocabulary [{}] successfuly added. ID: {}, group: {}",
                    assetVocabulary.getName(), assetVocabulary.getVocabularyId(), assetVocabulary.getGroupId());
            setupCategories(assetVocabulary.getVocabularyId(), groupId, 0L, vocabulary.getCategory(), defaultLocale);
        } catch (PortalException e) {
            LOG.error("Error while trying to create vocabulary with title: {}", titleMap, e);
        }
    }

    private static String composeVocabularySettings(Vocabulary vocabulary, final long groupId) {
        // class copied into the bundle from com.liferay.portal.impl bundle with maven shade plugin
        AssetVocabularySettingsHelper assetVocabularySettingsHelper = new AssetVocabularySettingsHelper();
        assetVocabularySettingsHelper.setMultiValued(vocabulary.isMultiValued());
        List<AssociatedAssetType> types = vocabulary.getAssociatedAssetType();

        if (Objects.isNull(types) || types.isEmpty()) {
            assetVocabularySettingsHelper.setClassNameIdsAndClassTypePKs(
                    new long[] {AssetCategoryConstants.ALL_CLASS_NAME_ID},
                    new long[] {AssetCategoryConstants.ALL_CLASS_TYPE_PK}, new boolean[] {false});
            return assetVocabularySettingsHelper.toString();
        }

        List<Long> classNameIds = new LinkedList<>();
        List<Long> classTypePKs = new LinkedList<>();
        List<Boolean> requireds = new LinkedList<>();

        for (AssociatedAssetType type : types) {
            ClassName className = ClassNameLocalServiceUtil.fetchClassName(type.getClassName());
            if (className.getValue().isEmpty()) {
                continue;
            }

            long subtypePK = -1;
            if (Objects.nonNull(type.getSubtypeStructureKey()) && !type.getSubtypeStructureKey().isEmpty()) {
                // has subtype
                try {
                    subtypePK = ResolverUtil.getStructureId(type.getSubtypeStructureKey(), groupId,
                            type.getClassName(), true);
                } catch (PortalException e) {
                    LOG.error("Class can not be be resolved for classname: {}", type.getClassName(), e);
                    continue;
                }
            }

            classNameIds.add(className.getClassNameId());
            classTypePKs.add(subtypePK);
            requireds.add(type.isRequired());
        }

        // no valid associated types case
        if (classNameIds.isEmpty()) {
            assetVocabularySettingsHelper.setClassNameIdsAndClassTypePKs(
                    new long[] {AssetCategoryConstants.ALL_CLASS_NAME_ID},
                    new long[] {AssetCategoryConstants.ALL_CLASS_TYPE_PK}, new boolean[] {false});
            return assetVocabularySettingsHelper.toString();
        }

        // when associated types exists
        boolean[] requiredsArray = new boolean[requireds.size()];
        for (int i = 0; i < requireds.size(); i++) {
            requiredsArray[i] = requireds.get(i);
        }

        assetVocabularySettingsHelper.setClassNameIdsAndClassTypePKs(ArrayUtil.toLongArray(classNameIds),
                ArrayUtil.toLongArray(classTypePKs), requiredsArray);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Vocabulary settings composed for vocabulary: [{}]. Content: {}",
                    vocabulary.getName(), assetVocabularySettingsHelper);
        }

        return assetVocabularySettingsHelper.toString();
    }

    private static void setupCategories(final long vocabularyId, final long groupId, final long parentId,
            final List<Category> categories, final Locale defaultLocale) {
        LOG.debug("Setting up categories for parentId: [{}]", parentId);

        if (categories != null && !categories.isEmpty()) {
            for (Category category : categories) {
                setupCategory(category, vocabularyId, groupId, defaultLocale, parentId);
            }
        }
    }

    private static void setupCategory(final Category category, final long vocabularyId, final long groupId,
            final Locale defaultLocale, final long parentCategoryId) {

        LOG.debug("Processing category [{}]", category.getName());

        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(category.getTitleTranslation(), groupId,
                category.getName(), String.format("Category [%1$s]", category.getName()));
        Map<Locale, String> descMap = new HashMap<>();
        String description = category.getDescription();
        descMap.put(defaultLocale, description);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(SetupConfigurationThreadLocal.getRunInCompanyId());
        serviceContext.setScopeGroupId(groupId);

        AssetCategory assetCategory = null;

        try {
            List<AssetCategory> existingCategories = AssetCategoryLocalServiceUtil.getChildCategories(parentCategoryId);
            for (AssetCategory ac : existingCategories) {
                if (ac.getName().equals(titleMap.get(LocaleUtil.getSiteDefault()))) {
                    assetCategory = ac;
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Error while trying to find category [{}]", category.getName(), e);
        }

        if (assetCategory != null) {
            LOG.info("Updating category [{}]", assetCategory.getName());

            assetCategory.setTitleMap(titleMap);
            assetCategory.setDescriptionMap(descMap);
            assetCategory.setName(category.getName());

            try {
                AssetCategoryLocalServiceUtil.updateAssetCategory(assetCategory);
                LOG.info("Category [{}] successfully updated.", assetCategory.getName());
            } catch (RuntimeException e) {
                LOG.error("Error while trying to update category [{}]", assetCategory.getName(), e);
            }

            setupCategories(vocabularyId, groupId, assetCategory.getCategoryId(), category.getCategory(),
                    defaultLocale);
            return;
        }

        try {
            LOG.info("Creating new category [{}]", category.getName());
            assetCategory = AssetCategoryLocalServiceUtil.addCategory(SetupConfigurationThreadLocal.getRunAsUserId(),
                    groupId, parentCategoryId, titleMap, descMap, vocabularyId, null, serviceContext);
            LOG.info(
                "Category [{}] successfully added with title: {}", assetCategory.getName(), assetCategory.getTitle());

            setupCategories(vocabularyId, groupId, assetCategory.getCategoryId(), category.getCategory(),
                    defaultLocale);

        } catch (PortalException e) {
            LOG.error("Error in creating category [{}]", category.getName(), e);
        }

    }
}
