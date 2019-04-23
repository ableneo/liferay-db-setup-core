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

import java.util.*;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.ResolverUtil;
import com.ableneo.liferay.portal.setup.core.util.TranslationMapUtil;
import com.ableneo.liferay.portal.setup.domain.AssociatedAssetType;
import com.ableneo.liferay.portal.setup.domain.Category;
import com.ableneo.liferay.portal.setup.domain.Vocabulary;
import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portlet.asset.util.AssetVocabularySettingsHelper;

/**
 * Setup module for creating / updating the categorization. So far it creates
 * tree of categories. In the future also AssetTag creation feature should be
 * here.
 * <p/>
 * Created by guno on 8. 6. 2015.
 */
public final class SetupCategorization {
    private static final Log LOG = LogFactoryUtil.getLog(SetupArticles.class);

    private SetupCategorization() {

    }

    public static void setupVocabularies(final List<Vocabulary> vocabularies, final long groupId)
            throws PortalException {
        Locale siteDefaultLocale = PortalUtil.getSiteDefaultLocale(groupId);

        LOG.info("Setting up vocabularies");

        for (Vocabulary vocabulary : vocabularies) {
            setupVocabulary(vocabulary, groupId, siteDefaultLocale);
        }
    }

    private static void setupVocabulary(final Vocabulary vocabulary, final long groupId, final Locale defaultLocale) {

        LOG.info(String.format("Setting up vocabulary with name: %1$s", vocabulary.getName()));

        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(vocabulary.getTitleTranslation(), groupId,
                vocabulary.getName(), "");

        Map<Locale, String> descMap = new HashMap<>();
        descMap.put(defaultLocale, vocabulary.getDescription());

        AssetVocabulary assetVocabulary = null;
        try {
            assetVocabulary = AssetVocabularyLocalServiceUtil.getGroupVocabulary(groupId, vocabulary.getName());
        } catch (PortalException | SystemException e) {
            LOG.error("Asset vocabulary was not found");
        }

        if (assetVocabulary != null) {
            LOG.debug("Vocabulary already exists. Will be updated.");

            assetVocabulary.setName(vocabulary.getName());
            assetVocabulary.setTitleMap(titleMap);
            assetVocabulary.setDescriptionMap(descMap);
            assetVocabulary.setSettings(composeVocabularySettings(vocabulary, groupId));

            try {
                assetVocabulary = AssetVocabularyLocalServiceUtil.updateAssetVocabulary(assetVocabulary);
                LOG.debug("Vocabulary successfully updated.");
            } catch (SystemException e) {
                LOG.info(String.format("Error while trying to update AssetVocabulary with ID:%1$s. Skipping.",
                        assetVocabulary.getVocabularyId()));
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
                    SetupConfigurationThreadLocal.getRunAsUserId(), groupId, null, titleMap, descMap,
                    composeVocabularySettings(vocabulary, groupId), serviceContext);
            LOG.info(String.format("AssetVocabulary successfuly added. ID:%1$s, group:%2$s",
                    assetVocabulary.getVocabularyId(), assetVocabulary.getGroupId()));
            setupCategories(assetVocabulary.getVocabularyId(), groupId, 0L, vocabulary.getCategory(), defaultLocale);
        } catch (PortalException | SystemException | NullPointerException e) {
            LOG.error(String.format("Error while trying to create vocabulary with title: %1$s", titleMap), e);
        }
    }

    private static String composeVocabularySettings(Vocabulary vocabulary, final long groupId) {
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
                            Class.forName(type.getClassName()), true);
                } catch (ClassNotFoundException | PortalException e) {
                    LOG.error(String.format("Class can not be be resolved for classname: %1$s", type.getClassName()),
                            e);
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
            LOG.debug(String.format("Vocabulary settings composed for vocabulary:%1$s. Content: %2$s",
                    vocabulary.getName(), assetVocabularySettingsHelper.toString()));
        }

        return assetVocabularySettingsHelper.toString();
    }

    private static void setupCategories(final long vocabularyId, final long groupId, final long parentId,
            final List<Category> categories, final Locale defaultLocale) {
        LOG.info(String.format("Setting up categories for parentId:%1$s", parentId));

        if (categories != null && !categories.isEmpty()) {
            for (Category category : categories) {
                setupCategory(category, vocabularyId, groupId, defaultLocale, parentId);
            }
        }
    }

    private static void setupCategory(final Category category, final long vocabularyId, final long groupId,
            final Locale defaultLocale, final long parentCategoryId) {

        LOG.info(String.format("Setting up category with name:%1$s", category.getName()));

        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(category.getTitleTranslation(), groupId,
                category.getName(), String.format("Category with name: %1$s", category.getName()));
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
                if (ac.getName().equals(category.getName())) {
                    assetCategory = ac;
                }
            }
        } catch (SystemException e) {
            LOG.error(String.format("Error while trying to find category with name: %1$s", category.getName()), e);
        }

        if (assetCategory != null) {
            LOG.error("Asset category already exists for parent category. Updating...");

            assetCategory.setTitleMap(titleMap);
            assetCategory.setDescriptionMap(descMap);
            assetCategory.setName(category.getName());

            try {
                AssetCategoryLocalServiceUtil.updateAssetCategory(assetCategory);
                LOG.info("Category successfully updated.");
            } catch (SystemException e) {
                LOG.error(
                        String.format("Error while trying to update category with name: %1$s", assetCategory.getName()),
                        e);
            }

            setupCategories(vocabularyId, groupId, assetCategory.getCategoryId(), category.getCategory(),
                    defaultLocale);
            return;
        }

        try {
            assetCategory = AssetCategoryLocalServiceUtil.addCategory(SetupConfigurationThreadLocal.getRunAsUserId(),
                    groupId, parentCategoryId, titleMap, descMap, vocabularyId, null, serviceContext);
            LOG.info(String.format("Category successfully added with title: %1$s", assetCategory.getTitle()));

            setupCategories(vocabularyId, groupId, assetCategory.getCategoryId(), category.getCategory(),
                    defaultLocale);

        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Error in creating category with name: %1$s", category.getName()), e);
        }

    }
}
