package com.ableneo.liferay.portal.setup.core;

import com.ableneo.liferay.portal.setup.SetupConfigurationThreadLocal;
import com.ableneo.liferay.portal.setup.core.util.ResolverUtil;
import com.ableneo.liferay.portal.setup.core.util.ResourcesUtil;
import com.ableneo.liferay.portal.setup.core.util.ServiceTrackerBuilder;
import com.ableneo.liferay.portal.setup.core.util.TaggingUtil;
import com.ableneo.liferay.portal.setup.core.util.TranslationMapUtil;
import com.ableneo.liferay.portal.setup.core.util.WebFolderUtil;
import com.ableneo.liferay.portal.setup.domain.Adt;
import com.ableneo.liferay.portal.setup.domain.Article;
import com.ableneo.liferay.portal.setup.domain.ArticleTemplate;
import com.ableneo.liferay.portal.setup.domain.DdlRecordset;
import com.ableneo.liferay.portal.setup.domain.RelatedAsset;
import com.ableneo.liferay.portal.setup.domain.RelatedAssets;
import com.ableneo.liferay.portal.setup.domain.RolePermissions;
import com.ableneo.liferay.portal.setup.domain.Site;
import com.ableneo.liferay.portal.setup.domain.StructureType;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.asset.link.constants.AssetLinkConstants;
import com.liferay.asset.link.service.AssetLinkLocalServiceUtil;
import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.data.engine.rest.dto.v2_0.DataDefinition;
import com.liferay.data.engine.rest.resource.v2_0.DataDefinitionResource;
import com.liferay.dynamic.data.lists.model.DDLRecordSet;
import com.liferay.dynamic.data.lists.service.DDLRecordSetLocalServiceUtil;
import com.liferay.dynamic.data.mapping.constants.DDMTemplateConstants;
import com.liferay.dynamic.data.mapping.exception.TemplateDuplicateTemplateKeyException;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormLayout;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalServiceUtil;
import com.liferay.journal.constants.JournalArticleConstants;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalFolder;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.journal.web.internal.util.DataDefinitionUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.search.DocumentImpl;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.xml.ElementImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.dom4j.tree.DefaultText;
import org.dom4j.util.IndexedElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mapa, guno..
 */
public final class SetupArticles {

    private static final Logger LOG = LoggerFactory.getLogger(SetupArticles.class);
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS;
    private static final HashMap<String, List<String>> DEFAULT_DDM_PERMISSIONS;
    private static final int ARTICLE_PUBLISH_YEAR = 2008;
    private static final int MIN_DISPLAY_ROWS = 10;
    private static ServiceTrackerBuilder<DataDefinitionResource.Factory> dataDefinitionResourceFactoryTracker;

    static {
        DEFAULT_PERMISSIONS = new HashMap<>();
        DEFAULT_DDM_PERMISSIONS = new HashMap<>();

        List<String> actionsOwner = new ArrayList<>();
        actionsOwner.add(ActionKeys.VIEW);
        actionsOwner.add(ActionKeys.ADD_DISCUSSION);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.DELETE_DISCUSSION);
        actionsOwner.add(ActionKeys.EXPIRE);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);

        List<String> ddmActionsOwner = new ArrayList<>();
        ddmActionsOwner.add(ActionKeys.VIEW);
        ddmActionsOwner.add(ActionKeys.DELETE);
        ddmActionsOwner.add(ActionKeys.UPDATE);
        ddmActionsOwner.add(ActionKeys.PERMISSIONS);

        DEFAULT_PERMISSIONS.put(RoleConstants.OWNER, actionsOwner);
        DEFAULT_DDM_PERMISSIONS.put(RoleConstants.OWNER, ddmActionsOwner);

        List<String> actionsUser = new ArrayList<>();
        actionsUser.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.USER, actionsUser);
        DEFAULT_DDM_PERMISSIONS.put(RoleConstants.USER, actionsUser);

        List<String> actionsGuest = new ArrayList<>();
        actionsGuest.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.GUEST, actionsGuest);
        DEFAULT_DDM_PERMISSIONS.put(RoleConstants.GUEST, actionsGuest);

        List<String> actionsViewer = new ArrayList<>();
        actionsViewer.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.SITE_MEMBER, actionsViewer);
        DEFAULT_DDM_PERMISSIONS.put(RoleConstants.SITE_MEMBER, actionsViewer);
    }

    private SetupArticles() {}

    public static void setupSiteStructuresAndTemplates(final Site site, long groupId) throws PortalException {
        List<StructureType> articleStructures = site.getArticleStructure();

        if (articleStructures != null && false == articleStructures.isEmpty()) {
            long classNameId = ClassNameLocalServiceUtil.getClassNameId(JournalArticle.class);
            for (StructureType structure : articleStructures) {
                addDDMStructure(structure, groupId, classNameId);
            }
        }

        List<StructureType> ddlStructures = site.getDdlStructure();

        if (ddlStructures != null && false == ddlStructures.isEmpty()) {
            long classNameId = ClassNameLocalServiceUtil.getClassNameId(DDLRecordSet.class);
            for (StructureType structure : ddlStructures) {
                LOG.info("Adding DDL structure {}", structure.getName());
                addDDMStructure(structure, groupId, classNameId);
            }
        }

        List<ArticleTemplate> articleTemplates = site.getArticleTemplate();
        if (articleTemplates != null) {
            for (ArticleTemplate template : articleTemplates) {
                try {
                    addDDMTemplate(template, groupId);
                } catch (TemplateDuplicateTemplateKeyException e) {
                    LOG.error("Failed to add DDM template with key {}", template.getKey(), e);
                }
            }
        }
    }

    public static void setupSiteArticles(
        final List<Article> articles,
        final List<Adt> adts,
        final List<DdlRecordset> recordSets,
        final long groupId
    ) throws PortalException {
        if (articles != null) {
            for (Article article : articles) {
                addJournalArticle(article, groupId);
            }
        }
        if (adts != null) {
            for (Adt template : adts) {
                try {
                    addDDMTemplate(template, groupId);
                } catch (TemplateDuplicateTemplateKeyException | IOException e) {
                    LOG.error(String.format("Error in adding ADT: %1$s", template.getName()), e);
                }
            }
        }
        if (recordSets != null) {
            for (DdlRecordset recordSet : recordSets) {
                try {
                    addDDLRecordSet(recordSet, groupId);
                } catch (TemplateDuplicateTemplateKeyException e) {
                    LOG.error(String.format("Error in adding DDLRecordSet: %1$s", recordSet.getName()), e);
                }
            }
        }
    }

    public static void addDDMStructure(final StructureType structure, final long groupId, final long classNameId)
        throws PortalException {
        LOG.info("Adding Article structure {}", structure.getName());
        Map<Locale, String> nameMap = new HashMap<>();
        Locale siteDefaultLocale = null;
        try {
            siteDefaultLocale = PortalUtil.getSiteDefaultLocale(groupId);
        } catch (PortalException e) {
            LOG.error("Failed to get site default locale for groupId {}", groupId, e);
        }
        String name = getStructureNameOrKey(structure);
        // when default site locale is not 'en_us', then LocaleUtil.getSiteDefault still returns en_us.. we are not IN the site yet..
        // so an exception follows: Name is null (for en_us locale). so:
        nameMap.put(siteDefaultLocale, name);
        Map<Locale, String> descMap = new HashMap<>();

        String content = null;
        DataDefinition dataDefinition = null;
        DDMForm ddmForm = null;
        DDMFormLayout ddmFormLayout = null;
        try {
            content = ResourcesUtil.getFileContent(structure.getPath());
            dataDefinition = DataDefinition.toDTO(content);
        } catch (IOException e) {
            LOG.error(String.format("Error Reading Structure File content for: %1$s", structure.getName()));
            return;
        } catch (Exception e) {
            LOG.error(
                String.format(
                    "Other error while trying to get content of the structure file. Possibly wrong filesystem path (%1$s)?",
                    structure.getPath()
                ),
                e
            );
            return;
        }

        DDMStructure ddmStructure = DDMStructureLocalServiceUtil.fetchStructure(
            groupId,
            classNameId,
            structure.getKey()
        );

        long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();

        DataDefinitionResource.Builder dataDefinitionResourcedBuilder = getDataDefinitionResourceFactory().create();

        DataDefinitionResource dataDefinitionResource = dataDefinitionResourcedBuilder
            .user(UserLocalServiceUtil.getUser(runAsUserId))
            .build();

        if (ddmStructure == null) {
            ddmStructure = DDMStructureLocalServiceUtil.createDDMStructure(
                CounterLocalServiceUtil.increment(DDMStructure.class.getName())
            );
            ddmStructure.setGroupId(groupId);
            ddmStructure.setCompanyId(companyId);
            ddmStructure.setClassNameId(classNameId);
            ddmStructure.setStructureKey(structure.getKey());
            ddmStructure = DDMStructureLocalServiceUtil.addDDMStructure(ddmStructure);
        }

        final long dataDefinitionId = ddmStructure.getStructureId();

        Locale contentDefaultLocale = ddmStructure.getDDMForm().getDefaultLocale();
        if (!contentDefaultLocale.equals(siteDefaultLocale)) {
            nameMap.put(contentDefaultLocale, name);
        }

        LOG.info("Structure already exists and will be overwritten.");
        if (structure.getParent() != null && !structure.getParent().isEmpty()) {
            LOG.info("Setting up parent structure: {}", structure.getName());
            DDMStructure parentStructure = DDMStructureLocalServiceUtil.fetchStructure(
                groupId,
                classNameId,
                structure.getParent(),
                true
            );
            if (parentStructure != null) {
                ddmStructure.setParentStructureId(parentStructure.getStructureId());
            } else {
                LOG.info("Parent structure not found: {}", structure.getName());
            }
        }

        try {
            DataDefinitionUtil.updateDataDefinitionFields(dataDefinition, ddmStructure);

            dataDefinitionResource.putDataDefinition(dataDefinitionId, dataDefinition);
        } catch (Exception e) {
            throw new PortalException(e);
        }

        LOG.info("Template successfully updated: {}", structure.getName());

        SetupPermissions.updatePermission(
            String.format("Structure %1$s", structure.getKey()),
            companyId,
            dataDefinitionId,
            DDMStructure.class.getName() + "-" + JournalArticle.class.getName(),
            structure.getRolePermissions(),
            DEFAULT_DDM_PERMISSIONS
        );

        return;
    }

    private static DataDefinitionResource.Factory getDataDefinitionResourceFactory() {
        if (dataDefinitionResourceFactoryTracker == null) {
            dataDefinitionResourceFactoryTracker = new ServiceTrackerBuilder<>(DataDefinitionResource.Factory.class);
        }
        return dataDefinitionResourceFactoryTracker.build().getService();
    }

    private static String getStructureNameOrKey(final StructureType structure) {
        if (structure.getName() == null) {
            return structure.getName();
        }
        return structure.getKey();
    }

    public static void addDDMTemplate(final ArticleTemplate template, long groupId) throws PortalException {
        LOG.info(String.format("Adding Article template %1$s", template.getName()));
        long classNameId = ClassNameLocalServiceUtil.getClassNameId(DDMStructure.class);
        long resourceClassnameId = ClassNameLocalServiceUtil.getClassNameId(JournalArticle.class);
        Map<Locale, String> nameMap = new HashMap<>();
        //        long groupId = SetupConfigurationThreadLocal.getRunInGroupId();
        Locale siteDefaultLocale = PortalUtil.getSiteDefaultLocale(groupId);
        String name = template.getName();
        if (name == null) {
            name = template.getKey();
        }
        nameMap.put(siteDefaultLocale, name);
        // when default site locale is not 'en_us', then LocaleUtil.getSiteDefault still returns en_us.. we are not IN the site yet..
        // so an exception follows: Name is null (for en_us locale). so:
        nameMap.put(LocaleUtil.US, name);
        Map<Locale, String> descMap = new HashMap<>();

        String script;
        try {
            script = ResourcesUtil.getFileContent(template.getPath());
        } catch (Exception e) {
            LOG.error(String.format("Error Reading Template File content for: %1$s", template.getName()));
            return;
        }

        long classPK = 0;
        if (template.getArticleStructureKey() != null) {
            try {
                classPK = ResolverUtil.getStructureId(
                    template.getArticleStructureKey(),
                    groupId,
                    JournalArticle.class.getName(),
                    true
                );
            } catch (PortalException e) {
                LOG.error(
                    String.format(
                        "Given article structure with ID: %1$s can not be found. Therefore, article template can not be added/changed.",
                        template.getArticleStructureKey()
                    ),
                    e
                );
                return;
            }
        }

        DDMTemplate ddmTemplate = null;
        try {
            ddmTemplate = DDMTemplateLocalServiceUtil.fetchTemplate(groupId, classNameId, template.getKey());
        } catch (SystemException e) {
            LOG.error(String.format("Error while trying to find template with key: %1$s", template.getKey()), e);
        }

        if (ddmTemplate != null) {
            LOG.info("Template already exists and will be overwritten.");
            ddmTemplate.setNameMap(nameMap);
            ddmTemplate.setLanguage(template.getLanguage());
            ddmTemplate.setScript(script);
            ddmTemplate.setClassPK(classPK);
            ddmTemplate.setCacheable(template.isCacheable());

            DDMTemplateLocalServiceUtil.updateDDMTemplate(ddmTemplate);
            LOG.info(String.format("Template successfully updated: %1$s", ddmTemplate.getName()));
            return;
        }

        long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();
        String externalReferenceCode = StringUtil.randomString();
        DDMTemplate newTemplate = DDMTemplateLocalServiceUtil.addTemplate(
            externalReferenceCode,
            runAsUserId,
            groupId,
            classNameId,
            classPK,
            resourceClassnameId,
            template.getKey(),
            nameMap,
            descMap,
            DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY,
            null,
            template.getLanguage(),
            script,
            template.isCacheable(),
            false,
            null,
            null,
            new ServiceContext()
        );

        LOG.info(String.format("Added Article template: %1$s", newTemplate.getName()));
    }

    public static void addDDMTemplate(final Adt template, final long groupId) throws PortalException, IOException {
        LOG.info(String.format("Adding ADT %1$s", template.getName()));
        long classNameId = PortalUtil.getClassNameId(template.getClassName());
        long resourceClassnameId = Validator.isBlank(template.getResourceClassName())
            ? ClassNameLocalServiceUtil.getClassNameId(JournalArticle.class)
            : ClassNameLocalServiceUtil.getClassNameId(template.getResourceClassName());

        Map<Locale, String> nameMap = new HashMap<>();

        Locale siteDefaultLocale = PortalUtil.getSiteDefaultLocale(groupId);
        String name = template.getName();
        if (name == null) {
            name = template.getTemplateKey();
        }
        // when default site locale is not 'en_us', then LocaleUtil.getSiteDefault still returns en_us.. we are not IN the site yet..
        // so an exception follows: Name is null (for en_us locale). so:
        nameMap.put(LocaleUtil.US, name);
        nameMap.put(siteDefaultLocale, name);

        Map<Locale, String> descriptionMap = new HashMap<>();
        descriptionMap.put(siteDefaultLocale, template.getDescription());

        String language = template.getLanguage() == null ? TemplateConstants.LANG_TYPE_FTL : template.getLanguage();

        DDMTemplate ddmTemplate = null;
        try {
            ddmTemplate = DDMTemplateLocalServiceUtil.fetchTemplate(groupId, classNameId, template.getTemplateKey());
        } catch (SystemException e) {
            LOG.error(String.format("Error while trying to find ADT with key: %1$s", template.getTemplateKey()));
        }

        String script = ResourcesUtil.getFileContent(template.getPath());

        if (ddmTemplate != null) {
            LOG.info("Template already exists and will be overwritten.");
            ddmTemplate.setLanguage(language);
            ddmTemplate.setNameMap(nameMap);
            ddmTemplate.setDescriptionMap(descriptionMap);
            ddmTemplate.setClassName(template.getClassName());
            ddmTemplate.setCacheable(template.isCacheable());
            ddmTemplate.setScript(script);

            DDMTemplateLocalServiceUtil.updateDDMTemplate(ddmTemplate);
            LOG.info(String.format("ADT successfully updated: %1$s", ddmTemplate.getName()));
            return;
        }
        long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();
        DDMTemplate newTemplate = DDMTemplateLocalServiceUtil.addTemplate(
            StringUtil.randomString(),
            runAsUserId,
            groupId,
            classNameId,
            0,
            resourceClassnameId,
            template.getTemplateKey(),
            nameMap,
            descriptionMap,
            DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY,
            null,
            language,
            script,
            true,
            false,
            null,
            null,
            new ServiceContext()
        );
        LOG.info(String.format("Added ADT: %1$s", newTemplate.getName()));
    }

    public static void getJournalArticleByPath(String folder, String file) {}

    public static long getCreateFolderId(String folder, long groupId, RolePermissions roles) {
        long folderId = 0L;
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();
        if (folder != null && !folder.isEmpty()) {
            JournalFolder jf = WebFolderUtil.findWebFolder(companyId, groupId, runAsUserId, folder, "", true, roles);
            if (jf == null) {
                LOG.warn("Specified webfolder {} of not found! Will put article into web content root folder!", folder);
            } else {
                folderId = jf.getFolderId();
            }
        }
        return folderId;
    }

    public static void addJournalArticle(final Article article, final long groupId) {
        LOG.info("Adding Journal Article {}", article.getTitle());

        long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        String content = null;
        long folderId = getCreateFolderId(article.getArticleFolderPath(), groupId, article.getRolePermissions());

        try {
            content = ResourcesUtil.getFileContent(article.getPath());
            content = ResolverUtil.lookupAll(groupId, companyId, content, article.getPath());
            LOG.info(
                " - Article File content for article ID: {} : path: {}",
                article.getArticleId(),
                article.getPath()
            );
        } catch (IOException e) {
            LOG.error(String.format("Error Reading Article File content for article ID: %1$s", article.getArticleId()));
        }
        Map<Locale, String> titleMap = TranslationMapUtil.getTranslationMap(
            article.getTitleTranslation(),
            groupId,
            article.getTitle(),
            String.format(" Article with title %1$s", article.getArticleId())
        );

        Locale articleDefaultLocale = LocaleUtil.fromLanguageId(LocalizationUtil.getDefaultLanguageId(content));
        if (!titleMap.containsKey(articleDefaultLocale)) {
            titleMap.put(articleDefaultLocale, article.getTitle());
        }

        Map<Locale, String> descriptionMap = null;
        if (article.getArticleDescription() != null && !article.getArticleDescription().isEmpty()) {
            descriptionMap = TranslationMapUtil.getTranslationMap(
                article.getDescriptionTranslation(),
                groupId,
                article.getArticleDescription(),
                String.format(" Article with description %1$s", article.getArticleId())
            );
            if (!descriptionMap.containsKey(articleDefaultLocale)) {
                descriptionMap.put(articleDefaultLocale, article.getArticleDescription());
            }
        }
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setScopeGroupId(groupId);

        JournalArticle journalArticle = null;

        boolean generatedId = (article.getArticleId().isEmpty());
        if (generatedId) {
            LOG.info(String.format("Article %1$s will have autogenerated ID.", article.getTitle()));
        } else {
            journalArticle = getJournalArticle(
                article.getArticleId(),
                folderId,
                groupId,
                article.getArticleFolderPath()
            );
        }

        if (journalArticle == null) {
            try {
                journalArticle = JournalArticleLocalServiceUtil.addArticle(
                    StringUtil.randomString(),
                    runAsUserId,
                    groupId,
                    folderId,
                    0,
                    0,
                    article.getArticleId(),
                    generatedId,
                    JournalArticleConstants.VERSION_DEFAULT,
                    titleMap,
                    descriptionMap,
                    null,
                    content,
                    getDdmStructureId(article.getArticleStructureKey()),
                    article.getArticleTemplateKey(),
                    StringPool.BLANK,
                    1,
                    1,
                    ARTICLE_PUBLISH_YEAR,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    true,
                    0,
                    0,
                    0,
                    0,
                    0, // reviewDateMonth, reviewDateDay, reviewDateYear, reviewDateHour, reviewDateMinute
                    true, // neverReview
                    true, // indexable
                    false, // smallImage
                    0L, // smallImageId
                    0, // smallImageSource
                    StringPool.BLANK, // smallImageURL
                    null, // smallImageFile
                    null, // images
                    StringPool.BLANK, // articleURL
                    serviceContext
                );

                LOG.info(
                    "Added JournalArticle {} with ID: {}",
                    journalArticle.getTitle(),
                    journalArticle.getArticleId()
                );
                Indexer<JournalArticle> bi = IndexerRegistryUtil.getIndexer(JournalArticle.class);
                if (bi != null) {
                    bi.reindex(journalArticle);
                }
            } catch (PortalException e) {
                LOG.error(String.format("Error while trying to add Article with Title: %1$s", article.getTitle()), e);
            }
        } else {
            try {
                LOG.info(
                    "Article {} with article ID: {} already exists. Will be overwritten.",
                    article.getTitle(),
                    article.getArticleId()
                );

                Document document = (Document) new DocumentImpl();
                org.dom4j.Element domElement = new IndexedElement("content");
                domElement.add(new DefaultText(content));
                Element element = new ElementImpl(domElement);
                document.add(element);
                journalArticle.setDocument(document);
                journalArticle.setDocument(document);
                journalArticle.setTitleMap(titleMap);
                journalArticle.setDescriptionMap(descriptionMap);

                JournalArticleLocalServiceUtil.updateJournalArticle(journalArticle);

                // if the folder changed, move it...
                if (journalArticle.getFolderId() != folderId) {
                    JournalArticleLocalServiceUtil.moveArticle(
                        groupId,
                        journalArticle.getArticleId(),
                        folderId,
                        ServiceContextThreadLocal.getServiceContext()
                    );
                }
                LOG.info("Updated JournalArticle: {}", journalArticle.getTitle());
            } catch (PortalException e) {
                LOG.error("Error while trying to update Article with Title: {}", article.getTitle(), e);
            }
        }
        if (journalArticle != null) {
            TaggingUtil.associateTagsAndCategories(groupId, article, journalArticle);
            processRelatedAssets(article, journalArticle, runAsUserId, groupId, companyId);
            SetupPermissions.updatePermission(
                String.format("Article %1$s", journalArticle.getArticleId()),
                companyId,
                journalArticle.getResourcePrimKey(),
                JournalArticle.class,
                article.getRolePermissions(),
                DEFAULT_PERMISSIONS
            );
            article.setArticleId(String.valueOf(journalArticle.getId()));
        } else {
            LOG.error(
                "Error while trying to add/update Article-Permission with Title: {}; see previous error.",
                article.getTitle()
            );
        }
    }

    private static long getDdmStructureId(String articleStructureKey) {
        final DDMStructure ddmStructure = DDMStructureLocalServiceUtil.fetchStructure(
            SetupConfigurationThreadLocal.getRunInGroupId(),
            ClassNameLocalServiceUtil.getClassNameId(JournalArticle.class),
            articleStructureKey
        );
        return ddmStructure.getStructureId();
    }

    public static JournalArticle getJournalArticle(
        String articleId,
        long folderId,
        long groupId,
        String folderPathForTheLog
    ) {
        JournalArticle journalArticle = null;

        try {
            List<JournalArticle> articlesInFolder = JournalArticleLocalServiceUtil.getArticles(groupId, folderId);
            if (articlesInFolder == null || articlesInFolder.isEmpty()) {
                LOG.warn("No such article : {} / {} ({})", folderPathForTheLog, articleId, folderId);
                return null;
            }

            List<JournalArticle> withSameArticleId = new ArrayList<>();
            for (JournalArticle art : articlesInFolder) {
                LOG.info(" - " + folderPathForTheLog + "/" + art.getArticleId());
                if (articleId.equalsIgnoreCase(art.getArticleId())) {
                    // liferay inside: uses 'ignore-case'
                    if (art.getStatus() == WorkflowConstants.STATUS_APPROVED) {
                        LOG.info(String.format(" - found article [%1$s] with same name [%1$s]", articleId));
                        withSameArticleId.add(art);
                    } else {
                        LOG.info(
                            String.format(" - found article which is not 'approved' [%1$s], leave-alone", articleId)
                        );
                    }
                }
            }

            if (!withSameArticleId.isEmpty()) {
                if (withSameArticleId.size() == 1) {
                    LOG.info(
                        String.format(
                            "FOUND article with ID: %1$s and directory:%2$s (%3$s)",
                            articleId,
                            folderPathForTheLog,
                            folderId
                        )
                    );
                    journalArticle = withSameArticleId.get(0);
                } else {
                    LOG.warn(
                        String.format(
                            "MULTIPLE article with ID: %1$s and directory:%2$s (%3$s)",
                            articleId,
                            folderPathForTheLog,
                            folderId
                        )
                    );
                    //
                    for (int i = 0; i < withSameArticleId.size(); i++) {
                        JournalArticle ja = withSameArticleId.get(i);
                        if (ja.getLastPublishDate() != null) {
                            if (journalArticle == null) {
                                journalArticle = ja;
                            } else {
                                if (
                                    ja.getLastPublishDate() != null &&
                                    ja.getLastPublishDate().after(journalArticle.getLastPublishDate())
                                ) {
                                    journalArticle = ja;
                                }
                            }
                        }
                    }
                    if (journalArticle == null) {
                        LOG.warn(
                            String.format(
                                "No article amongst multiple: %1$s and directory:%2$s (%3$s)",
                                articleId,
                                folderPathForTheLog,
                                folderId
                            )
                        );
                    } else {
                        LOG.info(
                            String.format(
                                "SELECTED article with ID: %1$s and directory:%2$s (%3$s)",
                                articleId,
                                folderPathForTheLog,
                                folderId
                            )
                        );
                    }
                }
            } else {
                LOG.warn(
                    String.format(
                        "Cannot find article with ID: %1$s and directory:%2$s (%3$s)",
                        articleId,
                        folderPathForTheLog,
                        folderId
                    )
                );
            }
        } catch (SystemException e) {
            LOG.error(String.format("Error while trying to find article with ID: %1$s", articleId), e);
        }
        return journalArticle;
    }

    private static void addDDLRecordSet(final DdlRecordset recordSet, final long groupId) throws PortalException {
        LOG.info(String.format("Adding DDLRecordSet %1$s", recordSet.getName()));
        Map<Locale, String> nameMap = new HashMap<>();
        Locale siteDefaultLocale = PortalUtil.getSiteDefaultLocale(groupId);
        nameMap.put(siteDefaultLocale, recordSet.getName());
        Map<Locale, String> descMap = new HashMap<>();
        descMap.put(siteDefaultLocale, recordSet.getDescription());
        DDLRecordSet ddlRecordSet = null;
        try {
            ddlRecordSet = DDLRecordSetLocalServiceUtil.fetchRecordSet(groupId, recordSet.getKey());
        } catch (SystemException e) {
            LOG.error(String.format("Error while trying to find DDLRecordSet with key: %1$s", recordSet.getKey()), e);
        }

        if (ddlRecordSet != null) {
            LOG.info("DDLRecordSet already exists and will be overwritten.");
            ddlRecordSet.setNameMap(nameMap);
            ddlRecordSet.setDescriptionMap(descMap);
            ddlRecordSet.setDDMStructureId(
                ResolverUtil.getStructureId(
                    recordSet.getDdlStructureKey(),
                    groupId,
                    DDLRecordSet.class.getName(),
                    false
                )
            );
            DDLRecordSetLocalServiceUtil.updateDDLRecordSet(ddlRecordSet);
            LOG.info(String.format("DDLRecordSet successfully updated: %1$s", recordSet.getName()));
            return;
        }

        long runAsUserId = SetupConfigurationThreadLocal.getRunAsUserId();
        DDLRecordSet newDDLRecordSet = DDLRecordSetLocalServiceUtil.addRecordSet(
            runAsUserId,
            groupId,
            ResolverUtil.getStructureId(recordSet.getDdlStructureKey(), groupId, DDLRecordSet.class.getName(), false),
            recordSet.getDdlStructureKey(),
            nameMap,
            descMap,
            MIN_DISPLAY_ROWS,
            0,
            new ServiceContext()
        );
        LOG.info(String.format("Added DDLRecordSet: %1$s", newDDLRecordSet.getName()));
    }

    public static Long getJournalAssetEntryId(JournalArticle ja) {
        try {
            AssetEntry ae = AssetEntryLocalServiceUtil.getEntry(
                JournalArticle.class.getName(),
                ja.getResourcePrimKey()
            );
            return ae.getEntryId();
        } catch (PortalException | SystemException e) {
            LOG.error(String.format("Problem clearing related assets of article %1$s", ja.getArticleId()), e);
        }
        return null;
    }

    public static void processRelatedAssets(
        final Article article,
        final JournalArticle ja,
        final long runAsUserId,
        final long groupId,
        final long companyId
    ) {
        if (article.getRelatedAssets() != null) {
            RelatedAssets ras = article.getRelatedAssets();
            AssetEntry ae = null;
            if (ras.isClearAllAssets()) {
                try {
                    ae = AssetEntryLocalServiceUtil.getEntry(JournalArticle.class.getName(), ja.getResourcePrimKey());
                    AssetLinkLocalServiceUtil.deleteLinks(ae.getEntryId());
                } catch (PortalException | SystemException e) {
                    LOG.error(String.format("Problem clearing related assets of article %1$s", ja.getArticleId()), e);
                }
            }
            if (ras.getRelatedAsset() != null && !ras.getRelatedAsset().isEmpty()) {
                List<RelatedAsset> ra = ras.getRelatedAsset();
                for (RelatedAsset r : ra) {
                    String clazz = r.getAssetClass();
                    String clazzPrimKey = r.getAssetClassPrimaryKey();
                    String resolverHint =
                        "Related asset for article " +
                        ja.getArticleId() +
                        " clazz " +
                        clazz +
                        ", " +
                        "primary key " +
                        clazzPrimKey;
                    clazzPrimKey = ResolverUtil.lookupAll(groupId, companyId, clazzPrimKey, resolverHint);

                    long id = 0;
                    try {
                        id = Long.parseLong(clazzPrimKey);
                    } catch (Exception ex) {
                        LOG.error("Class primary key is not parseable as long value.", ex);
                    }

                    try {
                        AssetEntry ae2 = AssetEntryLocalServiceUtil.getEntry(clazz, id);
                        AssetLinkLocalServiceUtil.addLink(
                            runAsUserId,
                            ae.getEntryId(),
                            ae2.getEntryId(),
                            AssetLinkConstants.TYPE_RELATED,
                            1
                        );
                    } catch (PortalException | SystemException e) {
                        LOG.error(
                            "Problem resolving related asset of article " +
                            ja.getArticleId() +
                            " with clazz " +
                            clazz +
                            " primary key " +
                            clazzPrimKey,
                            e
                        );
                    }
                }
            }
        }
    }
}
