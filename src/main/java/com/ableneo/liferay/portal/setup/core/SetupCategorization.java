package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.ResolverUtil;
import com.ableneo.liferay.portal.setup.core.util.TranslationMapUtil;
import com.ableneo.liferay.portal.setup.domain.AssociatedAssetType;
import com.ableneo.liferay.portal.setup.domain.Category;
import com.ableneo.liferay.portal.setup.domain.PropertyKeyValueType;
import com.ableneo.liferay.portal.setup.domain.Vocabulary;
import com.liferay.asset.category.property.model.AssetCategoryProperty;
import com.liferay.asset.category.property.service.AssetCategoryPropertyLocalServiceUtil;
import com.liferay.asset.kernel.exception.NoSuchCategoryException;
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
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portlet.asset.util.AssetVocabularySettingsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Setup module for creating / updating the categorization. So far it creates
 * tree of categories. In the future also AssetTag creation feature should be
 * here.
 * <p>
 * Created by guno on 8. 6. 2015.
 * </p>
 *
 * @author guno
 */
public final class SetupCategorization {
    private static final Logger LOG = LoggerFactory.getLogger(SetupCategorization.class);

    private SetupCategorization() {
    }

    public static void setupVocabularies(final Iterable<Vocabulary> vocabularies, final long groupId) {
        Locale siteDefaultLocale = LocaleUtil.getSiteDefault();

        LOG.info("Vocabulary setup STARTUP. It may take long time. To see runtime details enable DEBUG logging on {} class.", SetupCategorization.class.getName());
        StringBuilder statusLine = new StringBuilder();

        int index = 0;
        for (Vocabulary vocabulary : vocabularies) {
            setupVocabulary(vocabulary, groupId, siteDefaultLocale);
            if (index > 0) {
                statusLine.append(", ");
            }
            statusLine.append(vocabulary.getName());
            index++;
        }
        LOG.info("Vocabulary setup DONE. Created/updated following vocabularies: {}", statusLine);
    }

    private static void setupVocabulary(final Vocabulary vocabulary, final long groupId, final Locale defaultLocale) {
        LOG.debug("Setting up vocabulary [{}]", vocabulary.getName());

        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(
            vocabulary.getTitleTranslation(),
            groupId,
            vocabulary.getName(),
            ""
        );

        Map<Locale, String> descMap = new HashMap<>();
        descMap.put(defaultLocale, vocabulary.getDescription());

        AssetVocabulary assetVocabulary = null;

        try {
            assetVocabulary = AssetVocabularyLocalServiceUtil.getAssetVocabularyByUuidAndGroupId(vocabulary.getUuid(), groupId);
        } catch (NoSuchVocabularyException e) {
            LOG.warn("Asset vocabulary: [{}] was not found", vocabulary.getName());
        } catch (PortalException e) {
            LOG.error("Error while fetching asset vocabulary: [{}]", vocabulary.getName(), e);
        }

        if (assetVocabulary == null) {
            try {
                assetVocabulary =
                    AssetVocabularyLocalServiceUtil.getGroupVocabulary(
                        groupId,
                        StringUtil.toLowerCase(vocabulary.getName().trim())
                    );
            } catch (NoSuchVocabularyException e) {
                LOG.warn("Asset vocabulary: [{}] was not found", vocabulary.getName());
            } catch (PortalException e) {
                LOG.error("Error while fetching asset vocabulary: [{}]", vocabulary.getName(), e);
            }
        }

        final String settings = composeVocabularySettings(vocabulary, groupId);
        if (assetVocabulary != null) {
            LOG.debug("Vocabulary [{}] already exists. Will be updated.", assetVocabulary.getName());

            boolean update = false;
            if (!assetVocabulary.getTitleMap().equals(titleMap)) {
                assetVocabulary.setTitleMap(titleMap);
                update = true;
            }
            if (!assetVocabulary.getDescriptionMap().equals(descMap)) {
                assetVocabulary.setDescriptionMap(descMap);
                update = true;
            }
            if (!assetVocabulary.getSettings().equals(settings)) {
                assetVocabulary.setSettings(settings);
                update = true;
            }
            if (!Validator.isBlank(vocabulary.getUuid()) && !assetVocabulary.getUuid().equals(vocabulary.getUuid())) {
                assetVocabulary.setUuid(vocabulary.getUuid());
                update = true;
            }

            try {
                if (update) {
                    assetVocabulary = AssetVocabularyLocalServiceUtil.updateAssetVocabulary(assetVocabulary);
                    LOG.debug("Vocabulary [{}] successfully updated.", assetVocabulary.getName());
                }
            } catch (RuntimeException e) {
                LOG.warn(
                    "Error while trying to update AssetVocabulary with ID: {}. Skipping.",
                    assetVocabulary.getVocabularyId(),
                    e
                );
                return;
            }

        } else {
            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(SetupConfigurationThreadLocal.getRunInCompanyId());
            serviceContext.setScopeGroupId(groupId);
            try {
                assetVocabulary =
                    AssetVocabularyLocalServiceUtil.addVocabulary(
                        SetupConfigurationThreadLocal.getRunAsUserId(),
                        groupId,
                        vocabulary.getName(),
                        vocabulary.getName(),
                        titleMap,
                        descMap,
                        settings,
                        serviceContext
                    );

                if (!Validator.isBlank(vocabulary.getUuid())) {
                    assetVocabulary.setUuid(vocabulary.getUuid());
                    AssetVocabularyLocalServiceUtil.updateAssetVocabulary(assetVocabulary);
                }
            } catch (PortalException e) {
                LOG.error("Error while trying to create vocabulary with title: {}", titleMap, e);
            }
            LOG.debug(
                "AssetVocabulary [{}] successfuly added. ID: {}, group: {}",
                assetVocabulary.getName(),
                assetVocabulary.getVocabularyId(),
                assetVocabulary.getGroupId()
            );
        }
        setupCategories(assetVocabulary.getVocabularyId(), groupId, 0L, vocabulary.getCategory(), defaultLocale);

    }

    private static String composeVocabularySettings(Vocabulary vocabulary, final long groupId) {
        // class copied into the bundle from com.liferay.portal.impl bundle with maven shade plugin
        AssetVocabularySettingsHelper assetVocabularySettingsHelper = new AssetVocabularySettingsHelper();
        assetVocabularySettingsHelper.setMultiValued(vocabulary.isMultiValued());
        List<AssociatedAssetType> types = vocabulary.getAssociatedAssetType();

        if (Objects.isNull(types) || types.isEmpty()) {
            assetVocabularySettingsHelper.setClassNameIdsAndClassTypePKs(
                new long[]{AssetCategoryConstants.ALL_CLASS_NAME_ID},
                new long[]{AssetCategoryConstants.ALL_CLASS_TYPE_PK},
                new boolean[]{false}
            );
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
                    subtypePK =
                        ResolverUtil.getStructureId(type.getSubtypeStructureKey(), groupId, type.getClassName(), true);
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
                new long[]{AssetCategoryConstants.ALL_CLASS_NAME_ID},
                new long[]{AssetCategoryConstants.ALL_CLASS_TYPE_PK},
                new boolean[]{false}
            );
            return assetVocabularySettingsHelper.toString();
        }

        // when associated types exists
        boolean[] requiredsArray = new boolean[requireds.size()];
        for (int i = 0; i < requireds.size(); i++) {
            requiredsArray[i] = requireds.get(i);
        }

        assetVocabularySettingsHelper.setClassNameIdsAndClassTypePKs(
            ArrayUtil.toLongArray(classNameIds),
            ArrayUtil.toLongArray(classTypePKs),
            requiredsArray
        );
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                "Vocabulary settings composed for vocabulary: [{}]. Content: {}",
                vocabulary.getName(),
                assetVocabularySettingsHelper
            );
        }

        return assetVocabularySettingsHelper.toString();
    }

    private static void setupCategories(
        final long vocabularyId,
        final long groupId,
        final long parentCategoryId,
        final List<Category> categories,
        final Locale defaultLocale
    ) {
        LOG.debug("Setting up categories with parentCategoryId: [{}]", parentCategoryId);

        if (categories != null && !categories.isEmpty()) {
            for (Category category : categories) {
                setupCategory(category, vocabularyId, groupId, defaultLocale, parentCategoryId);
            }
        }
    }

    private static void setupCategory(
        final Category category,
        final long vocabularyId,
        final long groupId,
        final Locale defaultLocale,
        final long parentCategoryId
    ) {
        LOG.debug("Processing category [{}]", category.getName());

        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(
            category.getTitleTranslation(),
            groupId,
            category.getName(),
            String.format("Category [%1$s]", category.getName())
        );
        Map<Locale, String> descMap = new HashMap<>();
        String description = category.getDescription();
        descMap.put(defaultLocale, description);

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(SetupConfigurationThreadLocal.getRunInCompanyId());
        serviceContext.setScopeGroupId(groupId);

        AssetCategory assetCategory = null;

        if (!Validator.isBlank(category.getUuid())) {
            assetCategory = fetchAssetCategoryByUuid(category.getUuid(), groupId);
        }

        if (assetCategory == null) {
            assetCategory = fetchAssetCategoryByName(titleMap.get(LocaleUtil.getSiteDefault()), parentCategoryId, vocabularyId);
        }

        if (assetCategory != null) {
            LOG.debug("Updating category {}", assetCategory.getName());

            boolean update = false;
            if (!assetCategory.getTitleMap().equals(titleMap)) {
                assetCategory.setTitleMap(titleMap);
                update = true;
            }
            if (!assetCategory.getDescriptionMap().equals(descMap)) {
                assetCategory.setDescriptionMap(descMap);
                update = true;
            }
            if (!assetCategory.getName().equals(category.getName())) {
                assetCategory.setName(category.getName());
                update = true;
            }
            if (!assetCategory.getUuid().equals(category.getUuid())) {
                assetCategory.setUuid(category.getUuid());
                update = true;
            }

            try {
                if (update) {
                    AssetCategoryLocalServiceUtil.updateAssetCategory(assetCategory);
                }
                if (!category.getProperty().isEmpty()) {
                    updateCategoryProperties(category.getProperty(), assetCategory);
                }
                LOG.debug("Category [{}] successfully updated.", assetCategory.getName());
            } catch (RuntimeException e) {
                LOG.error("Error while trying to update category [{}]", assetCategory.getName(), e);
            }

            setupCategories(
                vocabularyId,
                groupId,
                assetCategory.getCategoryId(),
                category.getCategory(),
                defaultLocale
            );
            return;
        }

        try {
            LOG.debug("Creating new category [{}]", category.getName());
            assetCategory =
                AssetCategoryLocalServiceUtil.addCategory(
                    SetupConfigurationThreadLocal.getRunAsUserId(),
                    groupId,
                    parentCategoryId,
                    titleMap,
                    descMap,
                    vocabularyId,
                    null,
                    serviceContext
                );

            if (!Validator.isBlank(category.getUuid())) {
                assetCategory.setUuid(category.getUuid());
                assetCategory = AssetCategoryLocalServiceUtil.updateAssetCategory(assetCategory);
            }
            if (!category.getProperty().isEmpty()) {
                updateCategoryProperties(category.getProperty(), assetCategory);
            }
            LOG.debug(
                "Category [{}] successfully added with title: {}",
                assetCategory.getName(),
                assetCategory.getTitle()
            );

            setupCategories(
                vocabularyId,
                groupId,
                assetCategory.getCategoryId(),
                category.getCategory(),
                defaultLocale
            );
        } catch (PortalException e) {
            LOG.error("Error in creating category [{}]", category.getName(), e);
        }
    }

    private static void updateCategoryProperties(List<PropertyKeyValueType> property, AssetCategory assetCategory) {
        for (PropertyKeyValueType propertyKeyValueType : property) {
            AssetCategoryProperty assetCategoryProperty = null;
            try {
                assetCategoryProperty = AssetCategoryPropertyLocalServiceUtil.getCategoryProperty(assetCategory.getCategoryId(), propertyKeyValueType.getKey());
            } catch (PortalException e) {
                LOG.debug("Failed to get asset category property for asset category {} with id {} with key {}", assetCategory.getTitleCurrentValue(), assetCategory.getCategoryId(), propertyKeyValueType.getKey(), e);
            }
            if (assetCategoryProperty != null) {
                if (!assetCategoryProperty.getValue().equals(propertyKeyValueType.getValue())) {
                    assetCategoryProperty.setValue(propertyKeyValueType.getValue());
                    AssetCategoryPropertyLocalServiceUtil.updateAssetCategoryProperty(assetCategoryProperty);
                }
            } else {
                try {
                    AssetCategoryPropertyLocalServiceUtil.addCategoryProperty(SetupConfigurationThreadLocal.getRunAsUserId(), assetCategory.getCategoryId(), propertyKeyValueType.getKey(), propertyKeyValueType.getValue());
                } catch (PortalException e) {
                    LOG.error("Failed to add category property {} with value {} to category {} with id {} uuid {}", propertyKeyValueType.getKey(), propertyKeyValueType.getValue(), assetCategory.getTitleCurrentValue(), assetCategory.getCategoryId(), assetCategory.getUuid(), e);
                }
            }
        }
    }


    private static AssetCategory fetchAssetCategoryByName(String categoryName, long parentCategoryId, long vocabularyId) {
        AssetCategory assetCategory = null;
        try {
            List<AssetCategory> existingCategories = AssetCategoryLocalServiceUtil.getChildCategories(parentCategoryId);
            for (AssetCategory ac : existingCategories) {
                if (ac.getName().equals(categoryName) && ac.getVocabularyId() == vocabularyId) {
                    assetCategory = ac;
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("Error while trying to find category {} in a children list of category with id {}", categoryName, parentCategoryId, e);
        }
        return assetCategory;
    }

    private static AssetCategory fetchAssetCategoryByUuid(String uuid, long groupId) {
        AssetCategory assetCategory = null;
        try {
            assetCategory = AssetCategoryLocalServiceUtil.getAssetCategoryByUuidAndGroupId(uuid, groupId);
        } catch (NoSuchCategoryException e) {
            LOG.warn("Failed to fetch category with uuid {} from group with groupId {}. It will be updated by name or created as new.", uuid, groupId);
        } catch (PortalException e) {
            LOG.error("Failed to fetch category with uuid {} from group with groupId {}. Unexpected error occured.", uuid, groupId);
            throw new IllegalStateException(e);
        }
        return assetCategory;
    }
}
