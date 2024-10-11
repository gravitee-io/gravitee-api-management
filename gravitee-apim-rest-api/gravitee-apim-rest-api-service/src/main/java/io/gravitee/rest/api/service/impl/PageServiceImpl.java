/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PAGE;
import static io.gravitee.repository.management.model.Page.AuditEvent.*;
import static io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity.Type.INLINE;
import static io.gravitee.rest.api.model.PageType.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.fetcher.api.*;
import io.gravitee.fetcher.api.ResourceNotFoundException;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.descriptor.GraviteeDescriptorEntity;
import io.gravitee.rest.api.model.descriptor.GraviteeDescriptorPageEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.PageConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.gravitee.rest.api.service.impl.swagger.transformer.SwaggerTransformer;
import io.gravitee.rest.api.service.impl.swagger.transformer.entrypoints.EntrypointsOAITransformer;
import io.gravitee.rest.api.service.impl.swagger.transformer.page.PageConfigurationOAITransformer;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.gravitee.rest.api.service.swagger.SwaggerDescriptor;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Guillaume Gillon
 * @author GraviteeSource Team
 */
@Component
public class PageServiceImpl extends AbstractService implements PageService, ApplicationContextAware {

    public static final String SYSTEM_CONTRIBUTOR = "system";

    private static final Gson gson = new Gson();

    private static final Logger logger = LoggerFactory.getLogger(PageServiceImpl.class);

    private static final String SENSITIVE_DATA_REPLACEMENT = "********";

    @Value("${documentation.markdown.sanitize:true}")
    private boolean markdownSanitize;

    @Lazy
    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private ApiSearchService apiSearchService;

    @Autowired
    private SwaggerService swaggerService;

    @Autowired
    private PluginManager<FetcherPlugin> fetcherPluginManager;

    @Autowired
    private FetcherConfigurationFactory fetcherConfigurationFactory;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AuditService auditService;

    @Autowired
    private SearchEngineService searchEngineService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private PageRevisionService pageRevisionService;

    @Autowired
    private GraviteeDescriptorService graviteeDescriptorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ImportConfiguration importConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanSearchService planSearchService;

    @Autowired
    private PageConverter pageConverter;

    @Autowired
    private ApiEntrypointService apiEntrypointService;

    @Autowired
    private ApiTemplateService apiTemplateService;

    private static Page convert(NewPageEntity newPageEntity) {
        Page page = new Page();
        page.setCrossId(newPageEntity.getCrossId());
        page.setName(newPageEntity.getName());
        final PageType type = newPageEntity.getType();
        if (type != null) {
            page.setType(type.name());
        }
        page.setContent(newPageEntity.getContent());
        page.setLastContributor(newPageEntity.getLastContributor());
        page.setOrder(newPageEntity.getOrder());
        page.setPublished(newPageEntity.isPublished());
        if (newPageEntity.getVisibility() != null) {
            page.setVisibility(newPageEntity.getVisibility().name());
        }
        page.setHomepage(newPageEntity.isHomepage());
        page.setSource(convert(newPageEntity.getSource()));
        page.setConfiguration(newPageEntity.getConfiguration());
        page.setExcludedAccessControls(newPageEntity.isExcludedAccessControls());
        page.setAccessControls(convertACL(newPageEntity.getAccessControls()));
        page.setParentId("".equals(newPageEntity.getParentId()) ? null : newPageEntity.getParentId());
        page.setMetadata(newPageEntity.getMetadata());
        page.setUseAutoFetch(newPageEntity.getUseAutoFetch());
        page.setAttachedMedia(convertMediaEntity(newPageEntity.getAttachedMedia()));
        return page;
    }

    private static Page convert(ImportPageEntity importPageEntity) {
        Page page = new Page();
        final PageType type = importPageEntity.getType();
        if (type != null) {
            page.setType(type.name());
        }
        page.setLastContributor(importPageEntity.getLastContributor());
        page.setPublished(importPageEntity.isPublished());
        page.setVisibility(importPageEntity.getVisibility().name());
        page.setSource(convert(importPageEntity.getSource()));
        page.setConfiguration(importPageEntity.getConfiguration());
        page.setExcludedAccessControls(importPageEntity.isExcludedAccessControls());

        if (importPageEntity.getExcludedGroups() != null && !importPageEntity.getExcludedGroups().isEmpty()) {
            page.setExcludedAccessControls(true);
            page.setAccessControls(
                importPageEntity
                    .getExcludedGroups()
                    .stream()
                    .map(
                        (
                            groupId -> {
                                AccessControl accessControl = new AccessControl();
                                accessControl.setReferenceType("GROUP");
                                accessControl.setReferenceId(groupId);
                                return accessControl;
                            }
                        )
                    )
                    .collect(Collectors.toSet())
            );
        } else {
            page.setAccessControls(convertACL(importPageEntity.getAccessControls()));
        }
        return page;
    }

    private static Page merge(UpdatePageEntity updatePageEntity, Page withUpdatePage) {
        Page page = new Page();
        page.setCrossId(updatePageEntity.getCrossId() != null ? updatePageEntity.getCrossId() : withUpdatePage.getCrossId());
        page.setName(updatePageEntity.getName() != null ? updatePageEntity.getName() : withUpdatePage.getName());
        page.setContent(updatePageEntity.getContent() != null ? updatePageEntity.getContent() : withUpdatePage.getContent());
        page.setLastContributor(
            updatePageEntity.getLastContributor() != null ? updatePageEntity.getLastContributor() : withUpdatePage.getLastContributor()
        );
        page.setOrder(updatePageEntity.getOrder() != null ? updatePageEntity.getOrder() : withUpdatePage.getOrder());
        page.setPublished(updatePageEntity.isPublished() != null ? updatePageEntity.isPublished() : withUpdatePage.isPublished());
        page.setVisibility(
            updatePageEntity.getVisibility() != null ? updatePageEntity.getVisibility().name() : withUpdatePage.getVisibility()
        );

        PageSource pageSource = convert(updatePageEntity.getSource());
        page.setSource(pageSource != null ? pageSource : withUpdatePage.getSource());
        page.setConfiguration(
            updatePageEntity.getConfiguration() != null ? updatePageEntity.getConfiguration() : withUpdatePage.getConfiguration()
        );
        page.setHomepage(updatePageEntity.isHomepage() != null ? updatePageEntity.isHomepage() : withUpdatePage.isHomepage());
        page.setExcludedAccessControls(
            updatePageEntity.isExcludedAccessControls() != null
                ? updatePageEntity.isExcludedAccessControls()
                : withUpdatePage.isExcludedAccessControls()
        );
        page.setAccessControls(
            updatePageEntity.getAccessControls() != null
                ? convertACL(updatePageEntity.getAccessControls())
                : withUpdatePage.getAccessControls()
        );

        page.setAttachedMedia(
            updatePageEntity.getAttachedMedia() != null
                ? convertMediaEntity(updatePageEntity.getAttachedMedia())
                : withUpdatePage.getAttachedMedia()
        );
        page.setParentId(
            updatePageEntity.getParentId() != null
                ? updatePageEntity.getParentId().isEmpty() ? null : updatePageEntity.getParentId()
                : withUpdatePage.getParentId()
        );

        return page;
    }

    private static Page convert(UpdatePageEntity updatePageEntity) {
        Page page = new Page();

        page.setCrossId(updatePageEntity.getCrossId());
        page.setName(updatePageEntity.getName());
        page.setContent(updatePageEntity.getContent());
        page.setLastContributor(updatePageEntity.getLastContributor());
        page.setOrder(updatePageEntity.getOrder());
        page.setPublished(Boolean.TRUE.equals(updatePageEntity.isPublished()));
        if (updatePageEntity.getVisibility() != null) {
            page.setVisibility(updatePageEntity.getVisibility().name());
        }
        page.setSource(convert(updatePageEntity.getSource()));
        page.setConfiguration(updatePageEntity.getConfiguration());
        page.setHomepage(Boolean.TRUE.equals(updatePageEntity.isHomepage()));
        page.setExcludedAccessControls(Boolean.TRUE.equals(updatePageEntity.isExcludedAccessControls()));
        page.setAccessControls(convertACL(updatePageEntity.getAccessControls()));
        page.setAttachedMedia(convertMediaEntity(updatePageEntity.getAttachedMedia()));
        page.setParentId("".equals(updatePageEntity.getParentId()) ? null : updatePageEntity.getParentId());
        page.setMetadata(updatePageEntity.getMetadata());
        page.setUseAutoFetch(updatePageEntity.getUseAutoFetch());
        return page;
    }

    private static PageSource convert(PageSourceEntity pageSourceEntity) {
        PageSource source = null;
        if (pageSourceEntity != null && pageSourceEntity.getType() != null && pageSourceEntity.getConfiguration() != null) {
            source = new PageSource();
            source.setType(pageSourceEntity.getType());
            source.setConfiguration(pageSourceEntity.getConfiguration());
        }
        return source;
    }

    @SuppressWarnings("squid:S1166")
    private static boolean isJson(String content) {
        try {
            gson.fromJson(content, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

    private static List<PageMediaEntity> convertMedia(List<PageMedia> pages) {
        if (pages == null) {
            return emptyList();
        }
        return pages.stream().map(PageServiceImpl::convertMedia).collect(toList());
    }

    private static PageMediaEntity convertMedia(PageMedia pm) {
        final PageMediaEntity pageMediaEntity = new PageMediaEntity();
        pageMediaEntity.setMediaHash(pm.getMediaHash());
        pageMediaEntity.setMediaName(pm.getMediaName());
        pageMediaEntity.setAttachedAt(pm.getAttachedAt());
        return pageMediaEntity;
    }

    private static List<PageMedia> convertMediaEntity(List<PageMediaEntity> pages) {
        if (pages == null) {
            return emptyList();
        }
        return pages.stream().map(PageServiceImpl::convertMediaEntity).collect(toList());
    }

    private static PageMedia convertMediaEntity(PageMediaEntity pme) {
        final PageMedia pageMedia = new PageMedia();
        pageMedia.setMediaHash(pme.getMediaHash());
        pageMedia.setMediaName(pme.getMediaName());
        pageMedia.setAttachedAt(pme.getAttachedAt());
        return pageMedia;
    }

    private static Set<AccessControlEntity> convertToEntities(Set<AccessControl> accessControls) {
        if (accessControls == null) {
            return emptySet();
        }
        return accessControls.stream().map(accessControl -> convert(accessControl)).collect(Collectors.toSet());
    }

    private static AccessControlEntity convert(AccessControl accessControl) {
        final AccessControlEntity accessControlEntity = new AccessControlEntity();
        accessControlEntity.setReferenceId(accessControl.getReferenceId());
        accessControlEntity.setReferenceType(accessControl.getReferenceType());
        return accessControlEntity;
    }

    private static Set<AccessControl> convertACL(Set<AccessControlEntity> accessControlEntities) {
        if (accessControlEntities == null) {
            return emptySet();
        }
        return accessControlEntities.stream().map(PageServiceImpl::convert).collect(Collectors.toSet());
    }

    private static AccessControl convert(AccessControlEntity accessControlEntity) {
        AccessControl accessControl = new AccessControl();
        accessControl.setReferenceId(accessControlEntity.getReferenceId());
        accessControl.setReferenceType(accessControlEntity.getReferenceType());
        return accessControl;
    }

    private PageSituation getPageSituation(String pageId) throws TechnicalException {
        if (pageId == null) {
            return PageSituation.ROOT;
        } else {
            Optional<Page> optionalPage = pageRepository.findById(pageId);
            if (optionalPage.isPresent()) {
                Page page = optionalPage.get();
                if (PageType.SYSTEM_FOLDER.name().equalsIgnoreCase(page.getType())) {
                    return PageSituation.SYSTEM_FOLDER;
                }

                if (PageType.TRANSLATION.name().equalsIgnoreCase(page.getType())) {
                    return PageSituation.TRANSLATION;
                }

                String parentId = page.getParentId();
                if (parentId == null) {
                    return PageSituation.IN_ROOT;
                }

                Optional<Page> optionalParent = pageRepository.findById(parentId);
                if (optionalParent.isPresent()) {
                    Page parentPage = optionalParent.get();
                    if (PageType.SYSTEM_FOLDER.name().equalsIgnoreCase(parentPage.getType())) {
                        return PageSituation.IN_SYSTEM_FOLDER;
                    }

                    if (PageType.FOLDER.name().equalsIgnoreCase(parentPage.getType())) {
                        String grandParentId = parentPage.getParentId();
                        if (grandParentId == null) {
                            return PageSituation.IN_FOLDER_IN_ROOT;
                        }

                        Optional<Page> optionalGrandParent = pageRepository.findById(grandParentId);
                        if (optionalGrandParent.isPresent()) {
                            Page grandParentPage = optionalGrandParent.get();
                            if (PageType.SYSTEM_FOLDER.name().equalsIgnoreCase(grandParentPage.getType())) {
                                return PageSituation.IN_FOLDER_IN_SYSTEM_FOLDER;
                            }
                            if (PageType.FOLDER.name().equalsIgnoreCase(grandParentPage.getType())) {
                                return PageSituation.IN_FOLDER_IN_FOLDER;
                            }
                        }
                    }
                }
            }
            logger.debug("Impossible to determine page situation for the page " + pageId);
            return null;
        }
    }

    @Override
    public boolean isPageUsedAsGeneralConditions(ExecutionContext executionContext, PageEntity page, String apiId) {
        boolean result = false;
        if (PageType.MARKDOWN.name().equals(page.getType())) {
            Optional<GenericPlanEntity> optPlan = planSearchService
                .findByApi(executionContext, apiId)
                .stream()
                .filter(p -> p.getGeneralConditions() != null)
                .filter(p -> !(PlanStatus.CLOSED == p.getPlanStatus() || PlanStatus.STAGING == p.getPlanStatus()))
                .filter(p -> page.getId().equals(p.getGeneralConditions()))
                .findFirst();
            result = optPlan.isPresent();
        }
        return result;
    }

    @Override
    public io.gravitee.common.data.domain.Page<PageEntity> findAll(Pageable pageable) {
        Objects.requireNonNull(pageable, "FindAll requires a pageable parameter");
        logger.debug("Find all pages with pageNumber {} and pageSize {}", pageable.getPageNumber(), pageable.getPageSize());
        try {
            io.gravitee.common.data.domain.Page<Page> pages = this.pageRepository.findAll(convert(pageable));
            List<PageEntity> entities = pages.getContent().stream().map(this::convert).collect(toList());

            logger.debug("{} pages found", pages.getPageElements());
            return new io.gravitee.common.data.domain.Page<PageEntity>(
                entities,
                pages.getPageNumber(),
                entities.size(),
                pages.getTotalElements()
            );
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to fetch pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to fetch pages", ex);
        }
    }

    @Override
    public PageEntity findById(String pageId) {
        return this.findById(pageId, null);
    }

    @Override
    public PageEntity findById(String pageId, String acceptedLocale) {
        try {
            logger.debug("Find page by ID: {}", pageId);

            Optional<Page> page = pageRepository.findById(pageId);

            if (page.isPresent()) {
                String contentPageId = pageId;
                PageEntity foundPageEntity = convert(page.get());
                if (acceptedLocale != null && !acceptedLocale.isEmpty()) {
                    Page translation = getTranslation(foundPageEntity, acceptedLocale);
                    if (translation != null) {
                        String translationName = translation.getName();
                        if (translationName != null && !translationName.isEmpty()) {
                            foundPageEntity.setName(translationName);
                        }

                        String inheritContent = translation.getConfiguration().get(PageConfigurationKeys.TRANSLATION_INHERIT_CONTENT);
                        if (inheritContent != null && "false".equals(inheritContent)) {
                            foundPageEntity.setContent(translation.getContent());
                        }
                        // translation is used, set the translation page id as the one used to retrieve the content revision
                        contentPageId = translation.getId();
                    }
                } else {
                    List<PageEntity> translations = convert(getTranslations(foundPageEntity.getId()));
                    if (translations != null && !translations.isEmpty()) {
                        foundPageEntity.setTranslations(translations);
                    }
                }
                fillContentRevisionId(foundPageEntity, contentPageId);
                return foundPageEntity;
            }

            throw new PageNotFoundException(pageId);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a page using its ID {}", pageId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find a page using its ID " + pageId, ex);
        }
    }

    private void fillContentRevisionId(PageEntity foundPageEntity, String pageId) {
        if (foundPageEntity.getType() != null && shouldHaveRevision(foundPageEntity.getType())) {
            Optional<PageRevisionEntity> revision = pageRevisionService.findLastByPageId(pageId);
            if (revision.isPresent()) {
                foundPageEntity.setContentRevisionId(
                    new PageEntity.PageRevisionId(revision.get().getPageId(), revision.get().getRevision())
                );
            } else {
                logger.info("Revision is missing for the page {}", pageId);
            }
        }
    }

    @Override
    public void transformSwagger(final ExecutionContext executionContext, PageEntity pageEntity) {
        if (pageEntity instanceof ApiPageEntity) {
            ApiPageEntity apiPageEntity = (ApiPageEntity) pageEntity;
            GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiPageEntity.getApi());
            transformSwagger(executionContext, pageEntity, genericApiEntity);
        } else {
            if (markdownSanitize && PageType.MARKDOWN.name().equalsIgnoreCase(pageEntity.getType())) {
                sanitizeMarkdown(pageEntity);
            } else if (PageType.SWAGGER.name().equalsIgnoreCase(pageEntity.getType())) {
                SwaggerDescriptor<?> descriptor = swaggerService.parse(pageEntity.getContent());
                Collection<SwaggerTransformer<OAIDescriptor>> transformers = new ArrayList<>();
                transformers.add(new PageConfigurationOAITransformer(pageEntity));
                swaggerService.transform((OAIDescriptor) descriptor, transformers);
                setSwaggerContent(pageEntity, descriptor);
            }
        }
    }

    @Override
    public void transformSwagger(final ExecutionContext executionContext, PageEntity pageEntity, GenericApiEntity genericApiEntity) {
        // First apply templating if required
        transformWithTemplate(executionContext, pageEntity, genericApiEntity.getId());

        if (markdownSanitize && PageType.MARKDOWN.name().equalsIgnoreCase(pageEntity.getType())) {
            sanitizeMarkdown(pageEntity);
        } else if (PageType.SWAGGER.name().equalsIgnoreCase(pageEntity.getType())) {
            // If swagger page, let's try to apply transformations
            SwaggerDescriptor<?> descriptor;

            try {
                descriptor = swaggerService.parse(pageEntity.getContent());
            } catch (SwaggerDescriptorException sde) {
                logger.error("Parsing error for API id[{}]: {}", genericApiEntity.getId(), sde.getMessage());
                throw sde;
            }

            Collection<SwaggerTransformer<OAIDescriptor>> transformers = new ArrayList<>();
            transformers.add(new PageConfigurationOAITransformer(pageEntity));

            List<ApiEntrypointEntity> apiEntrypoints = apiEntrypointService.getApiEntrypoints(executionContext, genericApiEntity);
            String contextPath = getContextPath(genericApiEntity);
            transformers.add(new EntrypointsOAITransformer(pageEntity, apiEntrypoints, contextPath));

            swaggerService.transform((OAIDescriptor) descriptor, transformers);

            setSwaggerContent(pageEntity, descriptor);
        }
    }

    private void setSwaggerContent(PageEntity pageEntity, SwaggerDescriptor<?> descriptor) {
        try {
            if (pageEntity.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                pageEntity.setContent(descriptor.toJson());
            } else {
                pageEntity.setContent(descriptor.toYaml());
            }
        } catch (JsonProcessingException e) {
            logger.error("Unexpected error", e);
        }
    }

    private String getContextPath(GenericApiEntity genericApiEntity) {
        if (genericApiEntity.getDefinitionVersion() == DefinitionVersion.FEDERATED) {
            return null;
        }

        if (genericApiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            ApiEntity apiEntity = (ApiEntity) genericApiEntity;
            return apiEntity.getContextPath();
        } else {
            io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity = (io.gravitee.rest.api.model.v4.api.ApiEntity) genericApiEntity;
            return apiEntity
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .findFirst()
                .map(HttpListener.class::cast)
                .map(listener -> listener.getPaths().get(0).getPath())
                .orElse(null);
        }
    }

    private void sanitizeMarkdown(PageEntity pageEntity) {
        if (markdownSanitize && PageType.MARKDOWN.name().equalsIgnoreCase(pageEntity.getType())) {
            final HtmlSanitizer.SanitizeInfos safe = HtmlSanitizer.isSafe(pageEntity.getContent());
            if (!safe.isSafe()) {
                pageEntity.setContent(HtmlSanitizer.sanitize(pageEntity.getContent()));
            }
        }
    }

    @Override
    public List<PageEntity> search(final String environmentId, final PageQuery query) {
        return search(environmentId, query, false);
    }

    @Override
    public List<PageEntity> search(final String environmentId, final PageQuery query, boolean withTranslations) {
        return this.search(environmentId, query, null, withTranslations, true);
    }

    @Override
    public List<PageEntity> search(final String environmentId, final PageQuery query, String acceptedLocale) {
        return this.search(environmentId, query, acceptedLocale, false, true);
    }

    private List<PageEntity> search(
        final String environmentId,
        final PageQuery query,
        String acceptedLocale,
        boolean withTranslations,
        boolean withLinks
    ) {
        try {
            Stream<Page> pagesStream = pageRepository.search(queryToCriteria(query, environmentId)).stream();
            if (!withTranslations) {
                pagesStream = pagesStream.filter(page -> !PageType.TRANSLATION.name().equals(page.getType()));
            }
            if (!withLinks) {
                pagesStream = pagesStream.filter(page -> !PageType.LINK.name().equals(page.getType()));
            }

            List<PageEntity> pages = pagesStream.map(this::convert).collect(Collectors.toList());

            if (acceptedLocale == null || acceptedLocale.isEmpty()) {
                pages.forEach(p -> {
                    if (!PageType.TRANSLATION.name().equals(p.getType())) {
                        List<PageEntity> translations = convert(getTranslations(p.getId()));
                        if (translations != null && !translations.isEmpty()) {
                            p.setTranslations(translations);
                        }
                    }
                });
            } else {
                pages.forEach(p -> {
                    if (!PageType.TRANSLATION.name().equals(p.getType())) {
                        Page translation = getTranslation(p, acceptedLocale);
                        if (translation != null) {
                            String translationName = translation.getName();
                            if (translationName != null && !translationName.isEmpty()) {
                                p.setName(translationName);
                            }
                            String inheritContent = translation.getConfiguration().get(PageConfigurationKeys.TRANSLATION_INHERIT_CONTENT);
                            if (inheritContent != null && "false".equals(inheritContent)) {
                                p.setContent(translation.getContent());
                            }
                        }
                    }
                });
            }

            if (Boolean.TRUE.equals(query != null && query.getPublished() != null && query.getPublished()) || !isAuthenticated()) {
                // remove child of unpublished folders
                return pages
                    .stream()
                    .filter(page -> {
                        if (page.getParentId() != null) {
                            PageEntity parent = this.findById(page.getParentId());
                            if (!isAuthenticated()) {
                                return parent.isPublished() && Visibility.PUBLIC.equals(parent.getVisibility());
                            } else {
                                return parent.isPublished();
                            }
                        }
                        return true;
                    })
                    .collect(toList());
            }

            return pages;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to search pages", ex);
        }
    }

    private Page getTranslation(PageEntity pageToTranslate, String acceptedLocale) {
        if (
            PageType.LINK.name().equals(pageToTranslate.getType()) &&
            pageToTranslate.getConfiguration() != null &&
            "true".equals(pageToTranslate.getConfiguration().get(PageConfigurationKeys.LINK_INHERIT))
        ) {
            Page relatedTranslation = getTranslation(pageToTranslate.getContent(), acceptedLocale);
            Page linkTranslation = null;
            if (relatedTranslation != null) {
                linkTranslation = new Page();
                linkTranslation.setName(relatedTranslation.getName());
                linkTranslation.setContent(relatedTranslation.getContent());
                linkTranslation.setConfiguration(Collections.emptyMap());
            }
            return linkTranslation;
        }
        return getTranslation(pageToTranslate.getId(), acceptedLocale);
    }

    private Page getTranslation(String pageId, String acceptedLocale) {
        try {
            Optional<Page> optTranslation =
                this.pageRepository.search(new PageCriteria.Builder().parent(pageId).type(PageType.TRANSLATION.name()).build())
                    .stream()
                    .filter(t -> acceptedLocale.equalsIgnoreCase(t.getConfiguration().get(PageConfigurationKeys.TRANSLATION_LANG)))
                    .findFirst();
            if (optTranslation.isPresent()) {
                return optTranslation.get();
            }
            return null;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to search pages", ex);
        }
    }

    @Override
    public void transformWithTemplate(final ExecutionContext executionContext, final PageEntity pageEntity, final String api) {
        if (pageEntity.getContent() != null) {
            final Map<String, Object> model = new HashMap<>();
            if (api == null) {
                final List<MetadataEntity> metadataList = metadataService.findByReferenceTypeAndReferenceId(
                    MetadataReferenceType.ENVIRONMENT,
                    executionContext.getEnvironmentId()
                );
                if (metadataList != null) {
                    final Map<String, String> mapMetadata = new HashMap<>(metadataList.size());
                    metadataList.forEach(metadata -> mapMetadata.put(metadata.getKey(), metadata.getValue()));
                    model.put("metadata", mapMetadata);
                }
            } else {
                GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(executionContext, api, true);
                model.put("api", genericApiModel);
            }

            try {
                String content =
                    this.notificationTemplateService.resolveInlineTemplateWithParam(
                            executionContext.getOrganizationId(),
                            pageEntity.getId(),
                            pageEntity.getContent(),
                            model,
                            false
                        );
                pageEntity.setContent(content);
            } catch (TemplateProcessingException e) {
                if (pageEntity.getMessages() == null) {
                    pageEntity.setMessages(new ArrayList<>());
                }
                pageEntity.getMessages().add("Invalid expression or value is missing for " + e.getBlamedExpressionString());
            } catch (InvalidTemplateException e) {
                if (pageEntity.getMessages() == null) {
                    pageEntity.setMessages(new ArrayList<>());
                }
                pageEntity.getMessages().add("Invalid template: " + e.getMessage());
            }
        }
    }

    @Override
    public PageEntity createPage(final ExecutionContext executionContext, final String apiId, NewPageEntity newPageEntity) {
        return this.createPage(executionContext, apiId, newPageEntity, null);
    }

    @Override
    public PageEntity createPage(final ExecutionContext executionContext, String apiId, NewPageEntity newPageEntity, String newPageId) {
        try {
            logger.debug("Create page {} for API {}", newPageEntity, apiId);

            String id = newPageId != null && UUID.fromString(newPageId) != null ? newPageId : UuidString.generateRandom();

            PageType newPageType = newPageEntity.getType();

            // handle default visibility
            if (newPageEntity.getVisibility() == null) {
                newPageEntity.setVisibility(Visibility.PUBLIC);
            }

            // create page revision only for :
            // - SWAGGER
            // - Markdown
            // - Translation
            boolean createRevision = false;

            // Useful when import api with pages in old version
            if (newPageEntity.getExcludedGroups() != null && !newPageEntity.getExcludedGroups().isEmpty()) {
                newPageEntity.setVisibility(Visibility.PRIVATE);
                newPageEntity.setExcludedAccessControls(true);
                newPageEntity.setAccessControls(
                    newPageEntity
                        .getExcludedGroups()
                        .stream()
                        .map(
                            (
                                groupId -> {
                                    AccessControlEntity accessControl = new AccessControlEntity();
                                    accessControl.setReferenceType("GROUP");
                                    accessControl.setReferenceId(groupId);
                                    return accessControl;
                                }
                            )
                        )
                        .collect(Collectors.toSet())
                );
            }

            if (PageType.TRANSLATION.equals(newPageType)) {
                checkTranslationConsistency(newPageEntity.getParentId(), newPageEntity.getConfiguration(), true);

                Optional<Page> optTranslatedPage = this.pageRepository.findById(newPageEntity.getParentId());
                if (optTranslatedPage.isPresent()) {
                    newPageEntity.setPublished(optTranslatedPage.get().isPublished());
                    // create revision only for Swagger & Markdown page
                    createRevision = isSwaggerOrMarkdown(optTranslatedPage.get().getType());
                }
            }

            if (PageType.FOLDER.equals(newPageType)) {
                checkFolderConsistency(newPageEntity);
            }

            if (PageType.LINK.equals(newPageType)) {
                String resourceType = newPageEntity.getConfiguration().get(PageConfigurationKeys.LINK_RESOURCE_TYPE);
                String content = newPageEntity.getContent();
                if (content == null || content.isEmpty()) {
                    throw new PageActionException(PageType.LINK, "be created. It must have a URL, a page Id or a category Id");
                }
                if (
                    "root".equals(content) ||
                    PageConfigurationKeys.LINK_RESOURCE_TYPE_EXTERNAL.equals(resourceType) ||
                    PageConfigurationKeys.LINK_RESOURCE_TYPE_CATEGORY.equals(resourceType)
                ) {
                    newPageEntity.setPublished(true);
                } else {
                    Optional<Page> optionalRelatedPage = pageRepository.findById(content);
                    if (optionalRelatedPage.isPresent()) {
                        Page relatedPage = optionalRelatedPage.get();
                        checkLinkRelatedPageType(relatedPage);
                        newPageEntity.setPublished(relatedPage.isPublished());
                        newPageEntity.setVisibility(Visibility.valueOf(relatedPage.getVisibility()));
                    }
                }
            }

            if (PageType.SWAGGER == newPageType || PageType.MARKDOWN == newPageType) {
                checkMarkdownOrSwaggerConsistency(newPageEntity, newPageType);
                createRevision = true;
            }

            if (newPageEntity.getContent() == null && newPageEntity.getSource() != null && newPageType != ROOT) {
                fetchPage(newPageEntity);
            }

            Page page = convert(newPageEntity);

            page.setId(id);
            if (StringUtils.isEmpty(apiId)) {
                page.setReferenceId(executionContext.getEnvironmentId());
                page.setReferenceType(PageReferenceType.ENVIRONMENT);
            } else {
                page.setReferenceId(apiId);
                page.setReferenceType(PageReferenceType.API);
            }
            // Set date fields
            page.setCreatedAt(new Date());
            page.setUpdatedAt(page.getCreatedAt());

            List<String> messages = validateSafeContent(executionContext, page);
            Page createdPage = this.pageRepository.create(page);

            if (createRevision) {
                createPageRevision(createdPage);
            }

            //only one homepage is allowed
            onlyOneHomepage(page);
            createAuditLog(
                executionContext,
                PageReferenceType.API.equals(page.getReferenceType()) ? page.getReferenceId() : null,
                PAGE_CREATED,
                page.getCreatedAt(),
                null,
                page
            );
            PageEntity pageEntity = convert(createdPage);
            if (messages != null && messages.size() > 0) {
                pageEntity.setMessages(messages);
            }

            // add document in search engine
            if (PageType.isIndexable(PageType.valueOf(pageEntity.getType()))) {
                index(executionContext, pageEntity);
            }

            return pageEntity;
        } catch (TechnicalException | FetcherException ex) {
            logger.error("An error occurs while trying to create {}", newPageEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newPageEntity, ex);
        }
    }

    private void createPageRevision(Page page) {
        try {
            if (
                PageType.valueOf(page.getType()) == PageType.TRANSLATION &&
                page.getConfiguration() != null &&
                !page.getConfiguration().isEmpty() &&
                page.getConfiguration().get(PageConfigurationKeys.TRANSLATION_INHERIT_CONTENT).equalsIgnoreCase("true")
            ) {
                String translatedPageId = page.getParentId();
                this.pageRepository.findById(translatedPageId).ifPresent(translatedPage -> page.setContent(translatedPage.getContent()));
            }

            pageRevisionService.create(page);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create a revision for {}", page, ex);
            throw new TechnicalManagementException("An error occurs while trying create a revision for " + page, ex);
        }
    }

    private void checkMarkdownOrSwaggerConsistency(NewPageEntity newPageEntity, PageType newPageType) throws TechnicalException {
        PageSituation newPageParentSituation = getPageSituation(newPageEntity.getParentId());
        if (newPageParentSituation == PageSituation.SYSTEM_FOLDER || newPageParentSituation == PageSituation.IN_SYSTEM_FOLDER) {
            throw new PageActionException(newPageType, "be created under a system folder");
        }
    }

    private void checkFolderConsistency(NewPageEntity newPageEntity) throws TechnicalException {
        if (newPageEntity.getContent() != null && newPageEntity.getContent().length() > 0) {
            throw new PageFolderActionException("have a content");
        }

        if (newPageEntity.isHomepage()) {
            throw new PageFolderActionException("be affected to the home page");
        }

        PageSituation newPageParentSituation = getPageSituation(newPageEntity.getParentId());
        if (newPageParentSituation == PageSituation.IN_SYSTEM_FOLDER) {
            throw new PageFolderActionException("be created in a folder of a system folder");
        }
    }

    private Optional<PageEntity> findSystemFolder(String environmentId, String apiId) {
        PageQuery pageQuery = new PageQuery.Builder().api(apiId).name(SystemFolderType.ASIDE.folderName()).type(SYSTEM_FOLDER).build();
        return search(environmentId, pageQuery).stream().findFirst();
    }

    private void checkTranslationConsistency(String parentId, Map<String, String> configuration, boolean forCreation)
        throws TechnicalException {
        if (parentId == null || parentId.isEmpty()) {
            throw new PageActionException(PageType.TRANSLATION, "have no parentId");
        }
        if (
            configuration == null ||
            configuration.get(PageConfigurationKeys.TRANSLATION_LANG) == null ||
            configuration.get(PageConfigurationKeys.TRANSLATION_LANG).isEmpty()
        ) {
            throw new PageActionException(PageType.TRANSLATION, "have no configured language");
        }

        Optional<Page> optTranslatedPage = this.pageRepository.findById(parentId);
        if (optTranslatedPage.isPresent()) {
            Page translatedPage = optTranslatedPage.get();
            PageType translatedPageType = PageType.valueOf(translatedPage.getType());
            if (
                PageType.ROOT == translatedPageType ||
                PageType.SYSTEM_FOLDER == translatedPageType ||
                PageType.TRANSLATION == translatedPageType
            ) {
                throw new PageActionException(
                    PageType.TRANSLATION,
                    "have a parent with type " +
                    translatedPageType.name() +
                    ". Parent " +
                    parentId +
                    " is not one of this type : FOLDER, LINK, MARKDOWN, SWAGGER"
                );
            }

            if (forCreation) {
                String newTranslationLang = configuration.get(PageConfigurationKeys.TRANSLATION_LANG);
                Page existingTranslation = getTranslation(parentId, newTranslationLang);

                if (existingTranslation != null) {
                    throw new PageActionException(PageType.TRANSLATION, "be created. A translation for this language already exist");
                }
            }
        } else {
            // TODO: should be reactivated when import/export is fixed
            //throw new PageActionException(PageType.TRANSLATION, "have an inexisting parent. Parent " + parentId + " not found");
        }
    }

    private void checkLinkRelatedPageType(Page relatedPage) throws TechnicalException {
        PageSituation relatedPageSituation = getPageSituation(relatedPage.getId());

        if (
            PageType.LINK.name().equalsIgnoreCase(relatedPage.getType()) ||
            PageType.SYSTEM_FOLDER.name().equalsIgnoreCase(relatedPage.getType()) ||
            (PageType.FOLDER.name().equalsIgnoreCase(relatedPage.getType()) && relatedPageSituation == PageSituation.IN_SYSTEM_FOLDER)
        ) {
            throw new PageActionException(PageType.LINK, "be related to a Link, a System folder or a folder in a System folder");
        }
    }

    @Override
    public PageEntity createPage(final ExecutionContext executionContext, NewPageEntity newPageEntity) {
        return createPage(executionContext, null, newPageEntity);
    }

    @Override
    public PageEntity create(final ExecutionContext executionContext, final String apiId, final PageEntity pageEntity) {
        final NewPageEntity newPageEntity = pageConverter.toNewPageEntity(pageEntity);
        newPageEntity.setLastContributor(null);
        return createPage(executionContext, apiId, newPageEntity);
    }

    private void onlyOneHomepage(Page page) throws TechnicalException {
        if (page.isHomepage()) {
            Collection<Page> pages = pageRepository.search(
                new PageCriteria.Builder()
                    .referenceId(page.getReferenceId())
                    .referenceType(page.getReferenceType().name())
                    .homepage(true)
                    .build()
            );
            pages
                .stream()
                .filter(i -> !i.getId().equals(page.getId()))
                .forEach(i -> {
                    try {
                        i.setHomepage(false);
                        pageRepository.update(i);
                    } catch (TechnicalException e) {
                        logger.error("An error occurs while trying update homepage attribute from {}", page, e);
                    }
                });
        }
    }

    @Override
    public PageEntity update(final ExecutionContext executionContext, String pageId, UpdatePageEntity updatePageEntity) {
        return this.update(executionContext, pageId, updatePageEntity, false);
    }

    @Override
    public PageEntity update(final ExecutionContext executionContext, String pageId, UpdatePageEntity updatePageEntity, boolean partial) {
        try {
            logger.debug("Update Page {}", pageId);

            Optional<Page> optPageToUpdate = pageRepository.findById(pageId);
            if (!optPageToUpdate.isPresent()) {
                throw new PageNotFoundException(pageId);
            }

            Page pageToUpdate = optPageToUpdate.get();
            Page page = null;

            String pageType = pageToUpdate.getType();

            // create page revision if content has changed only for :
            // - SWAGGER
            // - Markdown
            // - Translation
            boolean createRevision = false;
            if (PageType.LINK.name().equalsIgnoreCase(pageType)) {
                String newResourceRef = updatePageEntity.getContent();
                String actualResourceRef = pageToUpdate.getContent();

                if (newResourceRef != null && !newResourceRef.equals(actualResourceRef)) {
                    String resourceType =
                        (
                            updatePageEntity.getConfiguration() != null
                                ? updatePageEntity.getConfiguration().get(PageConfigurationKeys.LINK_RESOURCE_TYPE)
                                : pageToUpdate.getConfiguration().get(PageConfigurationKeys.LINK_RESOURCE_TYPE)
                        );

                    if (
                        PageConfigurationKeys.LINK_RESOURCE_TYPE_EXTERNAL.equals(resourceType) &&
                        (updatePageEntity.getContent() != null && updatePageEntity.getContent().isEmpty())
                    ) {
                        throw new PageActionException(PageType.LINK, "be created. An external Link must have a URL");
                    }

                    if (
                        "root".equals(newResourceRef) ||
                        PageConfigurationKeys.LINK_RESOURCE_TYPE_EXTERNAL.equals(resourceType) ||
                        PageConfigurationKeys.LINK_RESOURCE_TYPE_CATEGORY.equals(resourceType)
                    ) {
                        updatePageEntity.setPublished(true);
                    } else {
                        Optional<Page> optionalRelatedPage = pageRepository.findById(newResourceRef);
                        if (optionalRelatedPage.isPresent()) {
                            Page relatedPage = optionalRelatedPage.get();
                            checkLinkRelatedPageType(relatedPage);
                            updatePageEntity.setPublished(relatedPage.isPublished());
                        }
                    }
                } else if (newResourceRef != null && newResourceRef.equals(actualResourceRef)) {
                    // cannot publish or unpublish a Link. LINK publication state is changed when the related page is updated.
                    updatePageEntity.setPublished(pageToUpdate.isPublished());
                }
            }

            if (PageType.TRANSLATION.name().equalsIgnoreCase(pageType)) {
                String parentId = (updatePageEntity.getParentId() != null && !updatePageEntity.getParentId().isEmpty())
                    ? updatePageEntity.getParentId()
                    : pageToUpdate.getParentId();
                Map<String, String> configuration = updatePageEntity.getConfiguration() != null
                    ? updatePageEntity.getConfiguration()
                    : pageToUpdate.getConfiguration();
                checkTranslationConsistency(parentId, configuration, false);

                boolean hasChanged = pageHasChanged(updatePageEntity, pageToUpdate);
                Optional<Page> optParentPage = this.pageRepository.findById(parentId);
                if (optParentPage.isPresent()) {
                    createRevision = isSwaggerOrMarkdown(optParentPage.get().getType()) && hasChanged;
                }
            }

            if (updatePageEntity.getParentId() != null && !updatePageEntity.getParentId().equals(pageToUpdate.getParentId())) {
                checkUpdatedPageSituation(updatePageEntity, pageType, pageId);
                if (PageType.TRANSLATION.name().equalsIgnoreCase(pageType)) {
                    Optional<Page> optionalTranslatedPage = pageRepository.findById(updatePageEntity.getParentId());
                    if (optionalTranslatedPage.isPresent()) {
                        updatePageEntity.setPublished(optionalTranslatedPage.get().isPublished());
                    }
                }
            }

            if (updatePageEntity.getExcludedGroups() != null && !updatePageEntity.getExcludedGroups().isEmpty()) {
                updatePageEntity.setVisibility(Visibility.PRIVATE);
                updatePageEntity.setExcludedAccessControls(true);

                Set<AccessControlEntity> accessControlEntities = updatePageEntity.getAccessControls() != null
                    ? updatePageEntity.getAccessControls()
                    : new HashSet<>();
                accessControlEntities.addAll(
                    updatePageEntity
                        .getExcludedGroups()
                        .stream()
                        .map(
                            (
                                groupId -> {
                                    AccessControlEntity accessControl = new AccessControlEntity();
                                    accessControl.setReferenceType("GROUP");
                                    accessControl.setReferenceId(groupId);
                                    return accessControl;
                                }
                            )
                        )
                        .collect(Collectors.toSet())
                );

                updatePageEntity.setAccessControls(accessControlEntities);
                updatePageEntity.setExcludedGroups(updatePageEntity.getExcludedGroups());
            }

            if (updatePageEntity.getSource() != null) {
                try {
                    if (updatePageEntity.getContent() == null && !ROOT.name().equals(pageType)) {
                        fetchPage(updatePageEntity);
                    }
                } catch (FetcherException e) {
                    throw onUpdateFail(pageId, e);
                }
            } else {
                updatePageEntity.setUseAutoFetch(null); // set null to remove the value not set to false
            }

            if (partial) {
                page = merge(updatePageEntity, pageToUpdate);
            } else {
                page = convert(updatePageEntity);
            }

            try {
                if (page.getSource() != null && page.getSource().getConfiguration() != null) {
                    final FetcherConfiguration newFetcherConfiguration = this.getFetcher(page.getSource()).getConfiguration();
                    if (newFetcherConfiguration.isAutoFetch()) {
                        page.setUseAutoFetch(Boolean.TRUE);
                    } else {
                        page.setUseAutoFetch(null);
                    }

                    if (pageToUpdate.getSource() != null && pageToUpdate.getSource().getConfiguration() != null) {
                        final FetcherConfiguration originalFetcherConfiguration =
                            this.getFetcher(pageToUpdate.getSource()).getConfiguration();
                        mergeSensitiveData(originalFetcherConfiguration, page);
                    }
                }
            } catch (FetcherException e) {
                throw onUpdateFail(pageId, e);
            }

            if (isSwaggerOrMarkdown(pageType)) {
                createRevision = pageHasChanged(pageToUpdate, page);
            }

            page.setId(pageId);
            page.setUpdatedAt(new Date());

            // Copy fields from existing values
            page.setCreatedAt(pageToUpdate.getCreatedAt());
            page.setType(pageType);
            page.setReferenceId(pageToUpdate.getReferenceId());
            page.setReferenceType(pageToUpdate.getReferenceType());
            if (page.getVisibility() == null) {
                page.setVisibility(pageToUpdate.getVisibility());
            }
            if (page.getCrossId() == null) {
                page.setCrossId(pageToUpdate.getCrossId());
            }
            onlyOneHomepage(page);

            // if the page is used as general condition for a plan,
            // we can't unpublish it until the plan is closed
            if (PageReferenceType.API.equals(pageToUpdate.getReferenceType())) {
                if (updatePageEntity.isPublished() != null && !updatePageEntity.isPublished()) {
                    Optional<GenericPlanEntity> activePlan = planSearchService
                        .findByApi(executionContext, pageToUpdate.getReferenceId())
                        .stream()
                        .filter(plan -> plan.getGeneralConditions() != null)
                        .filter(plan -> pageToUpdate.getId().equals(plan.getGeneralConditions()))
                        .filter(p -> !(PlanStatus.CLOSED == p.getPlanStatus() || PlanStatus.STAGING == p.getPlanStatus()))
                        .findFirst();
                    if (activePlan.isPresent()) {
                        throw new PageUsedAsGeneralConditionsException(pageId, page.getName(), "unpublish", activePlan.get().getName());
                    }
                }
            }

            // if the page is used by a category,
            // we can't unpublish it
            if (PageReferenceType.ENVIRONMENT.equals(pageToUpdate.getReferenceType())) {
                if (updatePageEntity.isPublished() != null && !updatePageEntity.isPublished()) {
                    List<CategoryEntity> categoriesUsingPage = categoryService.findByPage(pageId);
                    if (!categoriesUsingPage.isEmpty()) {
                        String categoriesName = categoriesUsingPage
                            .stream()
                            .map(CategoryEntity::getName)
                            .collect(Collectors.joining(", ", "{ ", " }"));
                        throw new PageUsedByCategoryException(pageId, page.getName(), "unpublish", categoriesName);
                    }
                }
            }

            // if order change, reorder all pages
            if (page.getOrder() != pageToUpdate.getOrder()) {
                reorderAndSavePages(page);
            }

            List<String> messages = validateSafeContent(executionContext, page);
            Page updatedPage = pageRepository.update(page);

            if (
                (pageToUpdate.isPublished() != page.isPublished() || !Objects.equals(pageToUpdate.getVisibility(), page.getVisibility())) &&
                !PageType.LINK.name().equalsIgnoreCase(pageType) &&
                !PageType.TRANSLATION.name().equalsIgnoreCase(pageType)
            ) {
                // if just publishing the page, updatePageEntity.getVisibility() will return null. In that case, we must keep the existing visibility status.
                String newVisibility = updatePageEntity.getVisibility() == null
                    ? pageToUpdate.getVisibility()
                    : updatePageEntity.getVisibility().name();

                // update all the related links and translations publication status.
                this.changeRelatedPagesPublicationStatusAndVisibility(pageId, updatePageEntity.isPublished(), newVisibility);
            }

            createAuditLog(
                executionContext,
                PageReferenceType.API.equals(page.getReferenceType()) ? page.getReferenceId() : null,
                PAGE_UPDATED,
                page.getUpdatedAt(),
                pageToUpdate,
                page
            );

            if (createRevision) {
                createPageRevision(updatedPage);
            }

            PageEntity pageEntity = convert(updatedPage);
            pageEntity.setMessages(messages);

            // update document in search engine
            if (pageToUpdate.isPublished() && !page.isPublished()) {
                searchEngineService.delete(executionContext, convert(pageToUpdate));
            } else {
                index(executionContext, pageEntity);
            }

            return pageEntity;
        } catch (TechnicalException ex) {
            throw onUpdateFail(pageId, ex);
        }
    }

    private boolean pageHasChanged(UpdatePageEntity updatePageEntity, Page pageToUpdate) {
        return pageHasChanged(convert(updatePageEntity), pageToUpdate);
    }

    private boolean pageHasChanged(Page updatedPage, Page pageToUpdate) {
        String newContent = updatedPage.getContent();
        String actualContent = pageToUpdate.getContent();

        String newName = updatedPage.getName();
        String actualName = pageToUpdate.getName();
        // newContent may be null in case of partialUpdate
        boolean hasChanged = (newContent != null && !newContent.equals(actualContent)) || (newName != null && !newName.equals(actualName));
        return hasChanged;
    }

    private boolean isSwaggerOrMarkdown(String pageType) {
        return PageType.SWAGGER.name().equalsIgnoreCase(pageType) || PageType.MARKDOWN.name().equalsIgnoreCase(pageType);
    }

    private void checkUpdatedPageSituation(UpdatePageEntity updatePageEntity, String pageType, String pageId) throws TechnicalException {
        PageSituation newParentSituation = getPageSituation(updatePageEntity.getParentId());
        switch (pageType) {
            case "SYSTEM_FOLDER":
                if (newParentSituation != PageSituation.ROOT) {
                    throw new PageActionException(PageType.SYSTEM_FOLDER, " be moved in this folder");
                }
                break;
            case "MARKDOWN":
                if (newParentSituation == PageSituation.SYSTEM_FOLDER || newParentSituation == PageSituation.IN_SYSTEM_FOLDER) {
                    throw new PageActionException(PageType.MARKDOWN, " be moved in a system folder or in a folder of a system folder");
                }
                break;
            case "SWAGGER":
                if (newParentSituation == PageSituation.SYSTEM_FOLDER || newParentSituation == PageSituation.IN_SYSTEM_FOLDER) {
                    throw new PageActionException(PageType.SWAGGER, " be moved in a system folder or in a folder of a system folder");
                }
                break;
            case "FOLDER":
                PageSituation folderSituation = getPageSituation(pageId);
                if (folderSituation == PageSituation.IN_SYSTEM_FOLDER && newParentSituation != PageSituation.SYSTEM_FOLDER) {
                    throw new PageActionException(PageType.FOLDER, " be moved anywhere other than in a system folder");
                } else if (folderSituation != PageSituation.IN_SYSTEM_FOLDER && newParentSituation == PageSituation.SYSTEM_FOLDER) {
                    throw new PageActionException(PageType.FOLDER, " be moved in a system folder");
                }
                break;
            case "LINK":
                if (newParentSituation != PageSituation.SYSTEM_FOLDER && newParentSituation != PageSituation.IN_SYSTEM_FOLDER) {
                    throw new PageActionException(
                        PageType.LINK,
                        " be moved anywhere other than in a system folder or in a folder of a system folder"
                    );
                }
                break;
            case "TRANSLATION":
                if (
                    newParentSituation == PageSituation.ROOT ||
                    newParentSituation == PageSituation.SYSTEM_FOLDER ||
                    newParentSituation == PageSituation.TRANSLATION
                ) {
                    throw new PageActionException(
                        PageType.TRANSLATION,
                        "be updated. Parent " +
                        updatePageEntity.getParentId() +
                        " is not one of this type : FOLDER, LINK, MARKDOWN, SWAGGER"
                    );
                }
                break;
            default:
                break;
        }
    }

    private void changeRelatedPagesPublicationStatusAndVisibility(String pageId, Boolean published, String newVisibility) {
        try {
            // Update related page's links
            this.pageRepository.search(new PageCriteria.Builder().type(PageType.LINK.name()).build())
                .stream()
                .filter(p -> pageId.equals(p.getContent()))
                .forEach(p -> {
                    try {
                        // Update link
                        p.setPublished(published);
                        p.setVisibility(newVisibility);
                        pageRepository.update(p);

                        // Update link's translations
                        changeTranslationPagesPublicationStatusAndVisibility(p.getId(), published, newVisibility);
                    } catch (TechnicalException ex) {
                        throw onUpdateFail(p.getId(), ex);
                    }
                });

            // Update related page's translations
            changeTranslationPagesPublicationStatusAndVisibility(pageId, published, newVisibility);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to search pages", ex);
        }
    }

    private void changeTranslationPagesPublicationStatusAndVisibility(String translatedPageId, Boolean published, String newVisibility) {
        try {
            this.pageRepository.search(new PageCriteria.Builder().parent(translatedPageId).type(PageType.TRANSLATION.name()).build())
                .stream()
                .forEach(p -> {
                    try {
                        p.setPublished(published);
                        p.setVisibility(newVisibility);
                        pageRepository.update(p);
                    } catch (TechnicalException ex) {
                        throw onUpdateFail(p.getId(), ex);
                    }
                });
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to search pages", ex);
        }
    }

    private void deleteRelatedPages(String pageId) {
        try {
            this.pageRepository.search(new PageCriteria.Builder().type("LINK").build())
                .stream()
                .filter(p -> pageId.equals(p.getContent()))
                .forEach(p -> {
                    try {
                        pageRepository.delete(p.getId());
                        this.deleteRelatedTranslations(p.getId());
                    } catch (TechnicalException ex) {
                        logger.error("An error occurs while trying to delete Page {}", p.getId(), ex);
                        throw new TechnicalManagementException("An error occurs while trying to delete Page " + p.getId(), ex);
                    }
                });
            this.deleteRelatedTranslations(pageId);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to search pages", ex);
        }
    }

    private void deleteRelatedTranslations(String pageId) {
        try {
            this.pageRepository.search(new PageCriteria.Builder().parent(pageId).type(PageType.TRANSLATION.name()).build())
                .stream()
                .forEach(p -> {
                    try {
                        pageRepository.delete(p.getId());
                    } catch (TechnicalException ex) {
                        logger.error("An error occurs while trying to delete Page {}", p.getId(), ex);
                        throw new TechnicalManagementException("An error occurs while trying to delete Page " + p.getId(), ex);
                    }
                });
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to search pages", ex);
        }
    }

    private void index(ExecutionContext executionContext, PageEntity pageEntity) {
        if (pageEntity.isPublished()) {
            searchEngineService.index(executionContext, pageEntity, false);
        }
    }

    private void fetchPage(FetchablePageEntity page) throws FetcherException {
        validateSafeSource(page.getSource());
        Fetcher fetcher = this.getFetcher(convert(page.getSource()));

        if (fetcher != null) {
            try {
                final Resource resource = fetcher.fetch();
                page.setContent(getResourceContentAsString(resource));
                if (resource.getMetadata() != null) {
                    page.setMetadata(new HashMap<>(resource.getMetadata().size()));
                    for (Map.Entry<String, Object> entry : resource.getMetadata().entrySet()) {
                        if (!(entry.getValue() instanceof Map)) {
                            page.getMetadata().put(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                    }
                }
                if (fetcher.getConfiguration().isAutoFetch()) {
                    page.setUseAutoFetch(Boolean.TRUE);
                } else {
                    page.setUseAutoFetch(null); // set null to remove the value not set to false
                }
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new FetcherException(e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings({ "Duplicates", "unchecked" })
    private Fetcher getFetcher(PageSource ps) throws FetcherException {
        if (ps == null || ps.getConfiguration().isEmpty()) {
            return null;
        }
        try {
            FetcherPlugin fetcherPlugin = fetcherPluginManager.get(ps.getType());
            ClassLoader fetcherCL = fetcherPlugin.fetcher().getClassLoader();
            Fetcher fetcher;
            if (fetcherPlugin.configuration().isAssignableFrom(FilepathAwareFetcherConfiguration.class)) {
                Class<? extends FetcherConfiguration> fetcherConfigurationClass =
                    (Class<? extends FetcherConfiguration>) fetcherCL.loadClass(fetcherPlugin.configuration().getName());
                Class<? extends FilesFetcher> fetcherClass = (Class<? extends FilesFetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());
                FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(
                    fetcherConfigurationClass,
                    ps.getConfiguration()
                );
                fetcher = fetcherClass.getConstructor(fetcherConfigurationClass).newInstance(fetcherConfigurationInstance);
            } else {
                Class<? extends FetcherConfiguration> fetcherConfigurationClass =
                    (Class<? extends FetcherConfiguration>) fetcherCL.loadClass(fetcherPlugin.configuration().getName());
                Class<? extends Fetcher> fetcherClass = (Class<? extends Fetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());
                FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(
                    fetcherConfigurationClass,
                    ps.getConfiguration()
                );
                fetcher = fetcherClass.getConstructor(fetcherConfigurationClass).newInstance(fetcherConfigurationInstance);
            }
            applicationContext.getAutowireCapableBeanFactory().autowireBean(fetcher);
            return fetcher;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new FetcherException(e.getMessage(), e);
        }
    }

    private String getResourceContentAsString(final Resource resource) throws FetcherException {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getContent()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new FetcherException(e.getMessage(), e);
        }
    }

    @Override
    public List<PageEntity> importFiles(final ExecutionContext executionContext, ImportPageEntity pageEntity) {
        return importFiles(executionContext, null, pageEntity);
    }

    @Override
    public List<PageEntity> importFiles(ExecutionContext executionContext, String apiId, ImportPageEntity pageEntity) {
        Page page = upsertRootPage(executionContext, apiId, pageEntity);
        pageEntity.setSource(convert(page.getSource(), false));
        return fetchPages(executionContext, apiId, pageEntity);
    }

    @Override
    public long execAutoFetch(ExecutionContext executionContext) {
        logger.debug("Auto Fetch pages");
        try {
            List<Page> autoFetchPages = pageRepository.search(new PageCriteria.Builder().withAutoFetch().build());
            long nbOfFetchedPages = autoFetchPages
                .stream()
                .filter(pageListItem -> pageListItem.getSource() != null)
                .filter(this::isFetchRequired)
                .map(page -> executeAutoFetch(executionContext, page))
                .flatMap(Collection::stream)
                .count();

            logger.debug("{} pages fetched", nbOfFetchedPages);
            return nbOfFetchedPages;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to fetch pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to fetch pages", ex);
        }
    }

    private boolean isFetchRequired(Page pageItem) {
        boolean fetchRequired = false;
        try {
            Fetcher fetcher = getFetcher(pageItem.getSource());
            if (fetcher == null) {
                logger.error("An error occurs while trying to fetch page source");
                throw new TechnicalManagementException("An error occurs while trying to fetch page source");
            }
            FetcherConfiguration configuration = fetcher.getConfiguration();
            if (configuration.isAutoFetch()) {
                String cron = configuration.getFetchCron();
                if (cron != null && !cron.isEmpty()) {
                    CronExpression cronExpression = CronExpression.parse(cron);
                    if (pageItem.getUpdatedAt() != null) {
                        LocalDateTime nextRun;
                        LocalDateTime updatedAt = LocalDateTime.ofInstant(pageItem.getUpdatedAt().toInstant(), ZoneId.systemDefault());
                        if ((nextRun = cronExpression.next(updatedAt)) != null) {
                            fetchRequired = nextRun.isBefore(LocalDateTime.now());
                        }
                    }
                }
            }
        } catch (FetcherException e) {
            logger.error("An error occurs while trying to initialize fetcher '{}'", pageItem.getSource().getType(), e);
        } catch (IllegalArgumentException e) {
            logger.error("An error occurs while trying to parse the cron expression", e);
        }
        return fetchRequired;
    }

    private List<PageEntity> executeAutoFetch(ExecutionContext executionContext, Page page) {
        try {
            if (page.getType() != null && page.getType().equals("ROOT")) {
                final ImportPageEntity pageEntity = new ImportPageEntity();
                pageEntity.setType(PageType.valueOf(page.getType()));
                pageEntity.setSource(convert(page.getSource(), false));
                pageEntity.setConfiguration(page.getConfiguration());
                pageEntity.setPublished(page.isPublished());
                pageEntity.setExcludedAccessControls(page.isExcludedAccessControls());
                pageEntity.setAccessControls(convertToEntities(page.getAccessControls()));
                pageEntity.setLastContributor(SYSTEM_CONTRIBUTOR);
                pageEntity.setVisibility(Visibility.valueOf(page.getVisibility()));
                return fetchPages(executionContext, page.getReferenceId(), pageEntity);
            } else {
                PageEntity fetchedPage = fetch(executionContext, page, SYSTEM_CONTRIBUTOR);
                if (fetchedPage != null) {
                    return List.of(fetchedPage);
                }
                return List.of();
            }
        } catch (TechnicalException e) {
            logger.error("An error occurs while trying to auto fetch page {}", page.getId(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public void fetchAll(ExecutionContext executionContext, PageQuery query, String contributor) {
        try {
            pageRepository
                .search(queryToCriteria(query, executionContext.getEnvironmentId()))
                .stream()
                .filter(pageListItem -> pageListItem.getSource() != null)
                .forEach(pageListItem -> {
                    if (pageListItem.getType() != null && pageListItem.getType().equals("ROOT")) {
                        final ImportPageEntity pageEntity = new ImportPageEntity();
                        pageEntity.setType(PageType.valueOf(pageListItem.getType()));
                        pageEntity.setSource(convert(pageListItem.getSource(), false));
                        pageEntity.setConfiguration(pageListItem.getConfiguration());
                        pageEntity.setPublished(pageListItem.isPublished());
                        pageEntity.setExcludedAccessControls(pageListItem.isExcludedAccessControls());
                        pageEntity.setAccessControls(convertToEntities(pageListItem.getAccessControls()));
                        pageEntity.setLastContributor(contributor);
                        fetchPages(executionContext, query.getApi(), pageEntity);
                    } else {
                        fetch(executionContext, pageListItem.getId(), contributor);
                    }
                });
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to fetch pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to fetch pages", ex);
        }
    }

    private List<PageEntity> importDescriptor(
        final ExecutionContext executionContext,
        final String apiId,
        final ImportPageEntity descriptorPageEntity,
        final FilesFetcher fetcher,
        final GraviteeDescriptorEntity descriptorEntity
    ) {
        if (
            descriptorEntity.getDocumentation() == null ||
            descriptorEntity.getDocumentation().getPages() == null ||
            descriptorEntity.getDocumentation().getPages().isEmpty()
        ) {
            return emptyList();
        }

        Map<String, String> parentsIdByPath = new HashMap<>();
        List<PageEntity> createdPages = new ArrayList<>();
        int order = 0;
        for (GraviteeDescriptorPageEntity descriptorPage : descriptorEntity.getDocumentation().getPages()) {
            NewPageEntity newPage = new NewPageEntity();
            newPage.setName(
                StringUtils.isEmpty(descriptorPage.getName())
                    ? FilenameUtils.getBaseName(descriptorPage.getSrc())
                    : descriptorPage.getName()
            );
            newPage.setHomepage(descriptorPage.isHomepage());
            newPage.setLastContributor(descriptorPageEntity.getLastContributor());
            newPage.setPublished(descriptorPageEntity.isPublished());
            newPage.setOrder(order++);
            newPage.setVisibility(descriptorPageEntity.getVisibility());
            newPage.setSource(descriptorPageEntity.getSource());

            try {
                fetchPageAndDefineType(newPage, fetcher, descriptorPage.getSrc());

                if (newPage.getType() == null) {
                    logger.warn("Unable to find a source file to import. Please fix the descriptor content.");
                } else {
                    String parentPath = descriptorPage.getDest() == null || descriptorPage.getDest().isEmpty()
                        ? getParentPathFromFilePath(descriptorPage.getSrc())
                        : descriptorPage.getDest();

                    createdPages.addAll(
                        upsertPageAndParentFolders(
                            executionContext,
                            parentPath,
                            newPage,
                            parentsIdByPath,
                            fetcher,
                            apiId,
                            descriptorPage.getSrc()
                        )
                    );
                }
            } catch (TechnicalException | FetcherException ex) {
                logger.error("An error occurs while trying to import a gravitee descriptor", ex);
                throw new TechnicalManagementException("An error occurs while trying to import a gravitee descriptor", ex);
            }
        }
        return createdPages;
    }

    private void fetchPageAndDefineType(NewPageEntity newPage, Fetcher fetcher, String filePath) throws FetcherException {
        updatePageSourceConfigurationFromFetcherConfiguration(newPage.getSource(), fetcher, filePath);

        fetchPage(newPage);

        newPage.setType(PageType.fromPageExtensionAndContent(FilenameUtils.getExtension(filePath), newPage.getContent()));
    }

    private void updatePageSourceConfigurationFromFetcherConfiguration(PageSourceEntity pageSource, Fetcher fetcher, String filePath) {
        FilepathAwareFetcherConfiguration configuration = (FilepathAwareFetcherConfiguration) fetcher.getConfiguration();
        configuration.setFilepath(filePath);
        pageSource.setConfiguration(new ObjectMapper().valueToTree(configuration));
    }

    private List<PageEntity> importDirectory(
        final ExecutionContext executionContext,
        String apiId,
        ImportPageEntity pageEntity,
        FilesFetcher fetcher
    ) {
        try {
            String[] filenames = fetcher.files();

            // if a gravitee descriptor is present, import it.
            Optional<String> optDescriptor = Arrays
                .stream(filenames)
                .filter(f -> f.endsWith(graviteeDescriptorService.descriptorName()))
                .findFirst();
            if (optDescriptor.isPresent()) {
                try {
                    ((FilepathAwareFetcherConfiguration) fetcher.getConfiguration()).setFilepath(optDescriptor.get());
                    final Resource resource = fetcher.fetch();
                    final GraviteeDescriptorEntity descriptorEntity = graviteeDescriptorService.read(getResourceContentAsString(resource));
                    return importDescriptor(executionContext, apiId, pageEntity, fetcher, descriptorEntity);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new FetcherException(e.getMessage(), e);
                }
            }

            Map<String, String> parentsIdByPath = new HashMap<>();

            List<PageEntity> createdPages = new ArrayList<>();
            // for each files returned by the fetcher
            int order = 0;
            for (String filename : filenames) {
                NewPageEntity newPage = new NewPageEntity();
                newPage.setLastContributor(pageEntity.getLastContributor());
                newPage.setPublished(pageEntity.isPublished());
                newPage.setSource(pageEntity.getSource());
                newPage.setOrder(order++);
                newPage.setName(FilenameUtils.getBaseName(filename));

                fetchPageAndDefineType(newPage, fetcher, filename);

                if (newPage.getType() != null) {
                    try {
                        createdPages.addAll(
                            upsertPageAndParentFolders(
                                executionContext,
                                getParentPathFromFilePath(filename),
                                newPage,
                                parentsIdByPath,
                                fetcher,
                                apiId,
                                filename
                            )
                        );
                    } catch (TechnicalException ex) {
                        logger.error("An error occurs while trying to import a directory", ex);
                        throw new TechnicalManagementException("An error occurs while trying to import a directory", ex);
                    }
                } else {
                    logger.warn("Importing a directory containing a file {} in a unknown format. Ignoring this file.", filename);
                }
            }
            return createdPages;
        } catch (FetcherException ex) {
            logger.error("An error occurs while trying to import a directory", ex);
            throw new TechnicalManagementException("An error occurs while trying import a directory", ex);
        }
    }

    protected String getParentPathFromFilePath(String filePath) {
        return StringUtils.isEmpty(filePath) ? "/" : FilenameUtils.getFullPathNoEndSeparator(filePath);
    }

    private List<PageEntity> upsertPageAndParentFolders(
        final ExecutionContext executionContext,
        final String parentPath,
        final NewPageEntity newPageEntity,
        final Map<String, String> parentsIdByPath,
        final FilesFetcher fetcher,
        final String apiId,
        final String src
    ) throws TechnicalException {
        String[] pathElements = parentPath.split("/");
        String pwd = "";
        List<PageEntity> createdPages = new ArrayList<>();

        //create each folders before the page itself
        for (String pathElement : pathElements) {
            if (!pathElement.isEmpty()) {
                String futurePwd = pwd + ("/" + pathElement);
                if (!parentsIdByPath.containsKey(futurePwd)) {
                    String parentId = parentsIdByPath.get(pwd);

                    List<Page> pages = pageRepository.search(
                        new PageCriteria.Builder()
                            .parent(parentId)
                            .referenceId(apiId)
                            .referenceType(PageReferenceType.API.name())
                            .name(pathElement)
                            .type(PageType.FOLDER.name())
                            .build()
                    );
                    PageEntity folder;
                    if (pages.isEmpty()) {
                        NewPageEntity newPage = new NewPageEntity();
                        newPage.setParentId(parentId);
                        newPage.setPublished(newPageEntity.isPublished());
                        newPage.setLastContributor(newPageEntity.getLastContributor());
                        newPage.setName(pathElement);
                        newPage.setType(PageType.FOLDER);
                        newPage.setVisibility(Visibility.PUBLIC);
                        folder = createPage(executionContext, apiId, newPage);
                    } else {
                        folder = convert(pages.get(0));
                    }
                    parentsIdByPath.put(futurePwd, folder.getId());
                    createdPages.add(folder);
                }
                pwd = futurePwd;
            }
        }

        // if we have reached the end of path, create or update the page
        String parentId = parentsIdByPath.get(pwd);
        List<Page> pages = pageRepository.search(
            new PageCriteria.Builder()
                .parent(parentId)
                .referenceId(apiId)
                .referenceType(PageReferenceType.API.name())
                .name(newPageEntity.getName())
                .type(newPageEntity.getType().name())
                .build()
        );
        if (pages.isEmpty()) {
            newPageEntity.setParentId(parentId);
            updatePageSourceConfigurationFromFetcherConfiguration(newPageEntity.getSource(), fetcher, src);
            newPageEntity.setVisibility(Visibility.PUBLIC);
            createdPages.add(createPage(executionContext, apiId, newPageEntity));
        } else {
            Page page = pages.get(0);
            UpdatePageEntity updatePage = convertToUpdateEntity(page);
            updatePage.setContent(newPageEntity.getContent());
            updatePage.setLastContributor(newPageEntity.getLastContributor());
            updatePage.setPublished(newPageEntity.isPublished());
            updatePage.setOrder(newPageEntity.getOrder());
            updatePage.setHomepage(newPageEntity.isHomepage());
            updatePage.setSource(newPageEntity.getSource());
            updatePageSourceConfigurationFromFetcherConfiguration(updatePage.getSource(), fetcher, src);
            createdPages.add(update(executionContext, page.getId(), updatePage));
        }
        return createdPages;
    }

    private Page upsertRootPage(final ExecutionContext executionContext, String apiId, ImportPageEntity rootPage) {
        try {
            // root page exists ?
            List<Page> searchResult = pageRepository.search(
                new PageCriteria.Builder().referenceId(apiId).referenceType(PageReferenceType.API.name()).type(PageType.ROOT.name()).build()
            );

            Page page = convert(rootPage);
            page.setReferenceId(apiId);
            page.setReferenceType(PageReferenceType.API);

            if (page.getSource() != null) {
                Fetcher fetcher = getFetcher(page.getSource());
                if (fetcher == null) {
                    logger.error("An error occurs while trying to fetch page source");
                    throw new TechnicalManagementException("An error occurs while trying to fetch page source");
                }
                final FetcherConfiguration configuration = fetcher.getConfiguration();
                if (configuration.isAutoFetch()) {
                    page.setUseAutoFetch(Boolean.TRUE);
                } else {
                    page.setUseAutoFetch(null);
                }
            }

            if (searchResult.isEmpty()) {
                page.setCreatedAt(new Date());
                page.setUpdatedAt(page.getCreatedAt());
                page.setId(UuidString.generateRandom());
                validateSafeContent(executionContext, page);
                return pageRepository.create(page);
            } else {
                page.setId(searchResult.get(0).getId());

                Fetcher fetcher = getFetcher(searchResult.get(0).getSource());
                if (fetcher == null) {
                    logger.error("An error occurs while trying to fetch page source");
                    throw new TechnicalManagementException("An error occurs while trying to fetch page source");
                }
                final FetcherConfiguration configuration = fetcher.getConfiguration();

                mergeSensitiveData(configuration, page);
                page.setUpdatedAt(new Date());
                validateSafeContent(executionContext, page);
                return pageRepository.update(page);
            }
        } catch (TechnicalException | FetcherException ex) {
            logger.error("An error occurs while trying to save the configuration", ex);
            throw new TechnicalManagementException("An error occurs while trying to save the configuration", ex);
        }
    }

    private void reorderAndSavePages(final Page pageToReorder) throws TechnicalException {
        PageCriteria.Builder q = new PageCriteria.Builder()
            .referenceId(pageToReorder.getReferenceId())
            .referenceType(pageToReorder.getReferenceType().name());
        if (pageToReorder.getParentId() == null) {
            q.rootParent(Boolean.TRUE);
        } else {
            q.parent(pageToReorder.getParentId());
        }
        final Collection<Page> pages = pageRepository.search(q.build());
        final List<Boolean> increment = asList(true);
        pages
            .stream()
            .sorted(Comparator.comparingInt(Page::getOrder))
            .forEachOrdered(page -> {
                try {
                    if (page.equals(pageToReorder)) {
                        increment.set(0, false);
                        page.setOrder(pageToReorder.getOrder());
                    } else {
                        final int newOrder;
                        final Boolean isIncrement = increment.get(0);
                        if (page.getOrder() < pageToReorder.getOrder()) {
                            newOrder = page.getOrder() - (isIncrement ? 0 : 1);
                        } else if (page.getOrder() > pageToReorder.getOrder()) {
                            newOrder = page.getOrder() + (isIncrement ? 1 : 0);
                        } else {
                            newOrder = page.getOrder() + (isIncrement ? 1 : -1);
                        }
                        page.setOrder(newOrder);
                    }
                    pageRepository.update(page);
                } catch (final TechnicalException ex) {
                    throw onUpdateFail(page.getId(), ex);
                }
            });
    }

    private TechnicalManagementException onUpdateFail(String pageId, TechnicalException ex) {
        logger.error("An error occurs while trying to update page {}", pageId, ex);
        return new TechnicalManagementException("An error occurs while trying to update page " + pageId, ex);
    }

    private TechnicalManagementException onUpdateFail(String pageId, FetcherException ex) {
        logger.error("An error occurs while trying to update page {}", pageId, ex);
        return new TechnicalManagementException("An error occurs while trying to fetch content. " + ex.getMessage(), ex);
    }

    //    @Override
    //    public boolean isDisplayable(ApiEntity api, boolean pageIsPublished, String username) {
    //        boolean isDisplayable = false;
    //        if (api.getVisibility() == Visibility.PUBLIC && pageIsPublished) {
    //            isDisplayable = true;
    //        } else if (username != null) {
    //            MemberEntity member = membershipService.getUserMember(MembershipReferenceType.API, api.getId(), username);
    //            if (member == null && api.getGroups() != null) {
    //                Iterator<String> groupIdIterator = api.getGroups().iterator();
    //                while (!isDisplayable && groupIdIterator.hasNext()) {
    //                    String groupId = groupIdIterator.next();
    //                    member = membershipService.getUserMember(MembershipReferenceType.GROUP, groupId, username);
    //                    isDisplayable = isDisplayableForMember(member, pageIsPublished);
    //                }
    //            } else {
    //                isDisplayable = isDisplayableForMember(member, pageIsPublished);
    //            }
    //        }
    //        return isDisplayable;
    //    }

    //
    //    private boolean isDisplayableForMember(MemberEntity member, boolean pageIsPublished) {
    //        // if not member => not displayable
    //        if (member == null) {
    //            return false;
    //        }
    //        // if member && published page => displayable
    //        if (pageIsPublished) {
    //            return true;
    //        }
    //
    //        // only members which could modify a page can see an unpublished page
    //        return roleService.hasPermission(member.getPermissions(), ApiPermission.DOCUMENTATION,
    //            new RolePermissionAction[]{RolePermissionAction.UPDATE, RolePermissionAction.CREATE,
    //                RolePermissionAction.DELETE});
    //    }

    @Override
    public void delete(ExecutionContext executionContext, String pageId) {
        try {
            logger.debug("Delete Page : {}", pageId);
            Optional<Page> optPage = pageRepository.findById(pageId);
            if (!optPage.isPresent()) {
                throw new PageNotFoundException(pageId);
            }
            Page page = optPage.get();
            // if the folder is not empty, throw exception
            if (PageType.FOLDER.name().equalsIgnoreCase(page.getType())) {
                List<Page> search = pageRepository.search(
                    new PageCriteria.Builder()
                        .referenceId(page.getReferenceId())
                        .referenceType(page.getReferenceType().name())
                        .parent(page.getId())
                        .build()
                );

                if (!search.isEmpty()) {
                    throw new TechnicalManagementException("Unable to remove the folder. It must be empty before being removed.");
                }
            }

            // if the page is used as a category documentation, throw an exception
            final List<CategoryEntity> categories = categoryService.findByPage(pageId);
            if (categories != null && !categories.isEmpty()) {
                String categoriesKeys = categories.stream().map(CategoryEntity::getKey).collect(Collectors.joining(","));
                throw new PageActionException(
                    PageType.valueOf(page.getType()),
                    "be deleted since it is used in categories [" + categoriesKeys + "]"
                );
            }

            // if the page is used as general condition for a plan,
            // we can't remove it until the plan is closed
            if (page.getReferenceType() != null && page.getReferenceType().equals(PageReferenceType.API)) {
                Optional<GenericPlanEntity> activePlan = planSearchService
                    .findByApi(executionContext, page.getReferenceId())
                    .stream()
                    .filter(plan -> plan.getGeneralConditions() != null)
                    .filter(plan -> // check the page and the parent for translations.
                        (PageType.TRANSLATION.name().equals(page.getType()) && plan.getGeneralConditions().equals(page.getParentId())) ||
                        plan.getGeneralConditions().equals(page.getId())
                    )
                    .filter(p -> PlanStatus.CLOSED != p.getPlanStatus())
                    .findFirst();

                if (activePlan.isPresent()) {
                    throw new PageUsedAsGeneralConditionsException(pageId, page.getName(), "remove", activePlan.get().getName());
                }
            }

            pageRepository.delete(pageId);

            // delete page revisions
            pageRevisionService.deleteAllByPageId(pageId);

            // delete links and translations related to the page
            if (!PageType.LINK.name().equalsIgnoreCase(page.getType()) && !PageType.TRANSLATION.name().equalsIgnoreCase(page.getType())) {
                this.deleteRelatedPages(page.getId());
            }

            createAuditLog(
                executionContext,
                PageReferenceType.API.equals(page.getReferenceType()) ? page.getReferenceId() : null,
                PAGE_DELETED,
                new Date(),
                page,
                null
            );

            // remove from search engine
            searchEngineService.delete(executionContext, convert(page));
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete Page {}", pageId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete Page " + pageId, ex);
        }
    }

    @Override
    public void deleteAllByApi(ExecutionContext executionContext, String apiId) {
        final List<PageEntity> pages = search(
            executionContext.getEnvironmentId(),
            new PageQuery.Builder().api(apiId).build(),
            null,
            false,
            false
        );
        pages.sort(
            new Comparator<PageEntity>() {
                @Override
                public int compare(PageEntity p0, PageEntity p1) {
                    Integer r0 = PageType.valueOf(p0.getType()).getRemoveOrder();
                    Integer r1 = PageType.valueOf(p1.getType()).getRemoveOrder();
                    if (r0.equals(r1)) {
                        return Integer.compare(p1.getOrder(), p0.getOrder());
                    }
                    return r0.compareTo(r1);
                }
            }
        );
        pages.forEach(pageEntity -> delete(executionContext, pageEntity.getId()));
    }

    @Override
    public int findMaxApiPageOrderByApi(String apiName) {
        try {
            logger.debug("Find Max Order Page for api name : {}", apiName);
            final Integer maxPageOrder = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder(apiName, PageReferenceType.API);
            return maxPageOrder == null ? 0 : maxPageOrder;
        } catch (TechnicalException ex) {
            logger.error("An error occured when searching max order page for api name [{}]", apiName, ex);
            throw new TechnicalManagementException("An error occured when searching max order page for api name " + apiName, ex);
        }
    }

    @Override
    public int findMaxPortalPageOrder(String referenceId) {
        try {
            logger.debug("Find Max Order Portal Page");
            final Integer maxPageOrder = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder(
                referenceId,
                PageReferenceType.ENVIRONMENT
            );
            return maxPageOrder == null ? 0 : maxPageOrder;
        } catch (TechnicalException ex) {
            logger.error("An error occured when searching max order portal page", ex);
            throw new TechnicalManagementException("An error occured when searching max order portal ", ex);
        }
    }

    @Override
    public PageEntity fetch(final ExecutionContext executionContext, String pageId, String contributor) {
        try {
            logger.debug("Fetch page {}", pageId);

            Page pageToUpdate = pageRepository.findById(pageId).orElseThrow(() -> new PageNotFoundException(pageId));

            if (pageToUpdate.getSource() == null) {
                throw new NoFetcherDefinedException(pageId);
            }

            return fetch(executionContext, pageToUpdate, contributor);
        } catch (TechnicalException ex) {
            throw onUpdateFail(pageId, ex);
        }
    }

    private PageEntity fetch(final ExecutionContext executionContext, Page page, String contributor) throws TechnicalException {
        // preserve content & name before the fetch
        // to detect if there are some changes
        Page previousPage = new Page();
        previousPage.setContent(page.getContent());
        previousPage.setName(page.getName());

        UpdatePageEntity updatePageEntity = convertToUpdateEntity(page, false);
        try {
            fetchPage(updatePageEntity);
        } catch (ResourceNotFoundException e) {
            logger.debug("page [{}] was not found in the source, it will be removed in the database too", page.getName());
            pageRepository.delete(page.getId());
            return null;
        } catch (FetcherException e) {
            throw onUpdateFail(page.getId(), e);
        }
        page.setContent(updatePageEntity.getContent());
        page.setMetadata(updatePageEntity.getMetadata());
        page.setUseAutoFetch(updatePageEntity.getUseAutoFetch());
        page.setUpdatedAt(new Date());
        page.setLastContributor(contributor);

        List<String> messages = validateSafeContent(executionContext, page);
        Page updatedPage = pageRepository.update(page);
        if (isSwaggerOrMarkdown(updatedPage.getType()) && pageHasChanged(updatedPage, previousPage)) {
            createPageRevision(updatedPage);
        }

        createAuditLog(
            executionContext,
            PageReferenceType.API.equals(page.getReferenceType()) ? page.getReferenceId() : null,
            PAGE_UPDATED,
            page.getUpdatedAt(),
            page,
            page
        );
        PageEntity pageEntity = convert(updatedPage);
        pageEntity.setMessages(messages);
        return pageEntity;
    }

    private List<PageEntity> fetchPages(final ExecutionContext executionContext, final String apiId, ImportPageEntity pageEntity) {
        try {
            Fetcher _fetcher = this.getFetcher(convert(pageEntity.getSource()));
            if (_fetcher == null) {
                return emptyList();
            }
            if (!(_fetcher instanceof FilesFetcher)) {
                throw new UnsupportedOperationException("The plugin does not support to import a directory.");
            }
            FilesFetcher fetcher = (FilesFetcher) _fetcher;
            return importDirectory(executionContext, apiId, pageEntity, fetcher);
        } catch (FetcherException ex) {
            logger.error("An error occurs while trying to import a directory", ex);
            throw new TechnicalManagementException("An error occurs while trying import a directory", ex);
        }
    }

    private List<PageEntity> convert(List<Page> pages) {
        if (pages == null) {
            return emptyList();
        }
        return pages.stream().map(this::convert).collect(toList());
    }

    private PageEntity convert(Page page) {
        PageEntity pageEntity;

        if (page.getReferenceId() != null && PageReferenceType.API.equals(page.getReferenceType())) {
            pageEntity = new ApiPageEntity();
            ((ApiPageEntity) pageEntity).setApi(page.getReferenceId());
        } else {
            pageEntity = new PageEntity();
        }

        pageEntity.setId(page.getId());
        pageEntity.setCrossId(page.getCrossId());
        pageEntity.setName(page.getName());
        pageEntity.setHomepage(page.isHomepage());
        pageEntity.setType(page.getType());
        pageEntity.setContent(page.getContent());
        pageEntity.setReferenceType(page.getReferenceType().name());
        pageEntity.setReferenceId(page.getReferenceId());

        if (isJson(page.getContent())) {
            pageEntity.setContentType(MediaType.APPLICATION_JSON);
        } else {
            // Yaml or RAML format ?
            pageEntity.setContentType("text/yaml");
        }

        pageEntity.setLastContributor(page.getLastContributor());
        pageEntity.setLastModificationDate(page.getUpdatedAt());
        pageEntity.setOrder(page.getOrder());
        pageEntity.setPublished(page.isPublished());
        pageEntity.setVisibility(Visibility.valueOf(page.getVisibility()));

        if (page.getSource() != null) {
            pageEntity.setSource(convert(page.getSource()));
        }
        if (page.getConfiguration() != null) {
            pageEntity.setConfiguration(page.getConfiguration());
        }
        if (page.getAttachedMedia() != null) {
            pageEntity.setAttachedMedia(convertMedia(page.getAttachedMedia()));
        }
        pageEntity.setExcludedAccessControls(page.isExcludedAccessControls());
        pageEntity.setAccessControls(PageServiceImpl.convertToEntities(page.getAccessControls()));

        if (page.isExcludedAccessControls() && Visibility.PRIVATE.name().equals(page.getVisibility())) {
            List<String> excludedGroups = emptyList();
            if (page.getAccessControls() != null) {
                excludedGroups =
                    page
                        .getAccessControls()
                        .stream()
                        .filter(accessControl -> AccessControlReferenceType.GROUP.name().equals(accessControl.getReferenceType()))
                        .map(accessControl -> accessControl.getReferenceId())
                        .collect(toList());
            }
            pageEntity.setExcludedGroups(excludedGroups);
        }

        pageEntity.setParentId("".equals(page.getParentId()) ? null : page.getParentId());
        pageEntity.setMetadata(page.getMetadata());

        pageEntity.setParentPath(this.computeParentPath(page, ""));

        return pageEntity;
    }

    private String computeParentPath(Page page, String suffix) {
        final String path = suffix;
        final String parentId = page.getParentId();
        if (!StringUtils.isEmpty(parentId)) {
            try {
                final Optional<Page> optParent = pageRepository.findById(parentId);
                if (optParent.isPresent()) {
                    return this.computeParentPath(optParent.get(), "/" + optParent.get().getName() + path);
                }
            } catch (TechnicalException ex) {
                logger.error("An error occurs while trying to find a page using its ID {}", parentId, ex);
            }
        }
        return path;
    }

    private List<Page> getTranslations(String pageId) {
        try {
            List<Page> searchResult =
                this.pageRepository.search(new PageCriteria.Builder().parent(pageId).type(PageType.TRANSLATION.name()).build());
            searchResult.sort((p1, p2) -> {
                String lang1 = p1.getConfiguration().get(PageConfigurationKeys.TRANSLATION_LANG);
                String lang2 = p2.getConfiguration().get(PageConfigurationKeys.TRANSLATION_LANG);
                return lang1.compareTo(lang2);
            });
            return searchResult;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to search pages", ex);
            throw new TechnicalManagementException("An error occurs while trying to search pages", ex);
        }
    }

    private UpdatePageEntity convertToUpdateEntity(Page page) {
        return convertToUpdateEntity(page, true);
    }

    private UpdatePageEntity convertToUpdateEntity(Page page, boolean removeSensitiveData) {
        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setCrossId(page.getCrossId());
        updatePageEntity.setName(page.getName());
        updatePageEntity.setContent(page.getContent());
        updatePageEntity.setLastContributor(page.getLastContributor());
        updatePageEntity.setOrder(page.getOrder());
        updatePageEntity.setPublished(page.isPublished());
        updatePageEntity.setSource(this.convert(page.getSource(), removeSensitiveData));
        updatePageEntity.setConfiguration(page.getConfiguration());
        updatePageEntity.setHomepage(page.isHomepage());
        updatePageEntity.setExcludedAccessControls(page.isExcludedAccessControls());
        updatePageEntity.setAccessControls(PageServiceImpl.convertToEntities(page.getAccessControls()));
        updatePageEntity.setAttachedMedia(convertMedia(page.getAttachedMedia()));
        updatePageEntity.setParentId("".equals(page.getParentId()) ? null : page.getParentId());
        updatePageEntity.setVisibility(Visibility.valueOf(page.getVisibility()));
        return updatePageEntity;
    }

    private PageSourceEntity convert(PageSource pageSource) {
        return convert(pageSource, true);
    }

    private PageSourceEntity convert(PageSource pageSource, boolean removeSensitiveData) {
        PageSourceEntity entity = null;
        if (pageSource != null) {
            entity = new PageSourceEntity();
            entity.setType(pageSource.getType());
            try {
                Fetcher fetcher = getFetcher(pageSource);
                if (fetcher == null) {
                    logger.error("An error occurs while trying to fetch page source");
                    throw new TechnicalManagementException("An error occurs while trying to fetch page source");
                }
                FetcherConfiguration fetcherConfiguration = fetcher.getConfiguration();
                if (removeSensitiveData) {
                    removeSensitiveData(fetcherConfiguration);
                }
                entity.setConfiguration((new ObjectMapper()).valueToTree(fetcherConfiguration));
            } catch (FetcherException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return entity;
    }

    private void removeSensitiveData(FetcherConfiguration fetcherConfiguration) {
        Field[] fields = fetcherConfiguration.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Sensitive.class)) {
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                try {
                    field.set(fetcherConfiguration, SENSITIVE_DATA_REPLACEMENT);
                } catch (IllegalAccessException e) {
                    logger.error("Error while removing fetcher sensitive data", e);
                }
                field.setAccessible(accessible);
            }
        }
    }

    private void mergeSensitiveData(FetcherConfiguration originalFetcherConfiguration, Page page) throws FetcherException {
        Fetcher fetcher = getFetcher(page.getSource());
        if (fetcher == null) {
            logger.error("An error occurs while trying to fetch page source");
            throw new TechnicalManagementException("An error occurs while trying to fetch page source");
        }
        FetcherConfiguration updatedFetcherConfiguration = fetcher.getConfiguration();
        boolean updated = false;

        Field[] fields = originalFetcherConfiguration.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Sensitive.class)) {
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                try {
                    Object updatedValue = field.get(updatedFetcherConfiguration);
                    if (SENSITIVE_DATA_REPLACEMENT.equals(updatedValue)) {
                        updated = true;
                        field.set(updatedFetcherConfiguration, field.get(originalFetcherConfiguration));
                    }
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    logger.error("Error while merging original fetcher sensitive data to new fetcher", e);
                }
                field.setAccessible(accessible);
            }
        }
        if (updated) {
            page.getSource().setConfiguration((new ObjectMapper()).valueToTree(updatedFetcherConfiguration).toString());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private List<String> validateSafeContent(ExecutionContext executionContext, Page page) {
        String apiId = null;
        if (PageReferenceType.API.equals(page.getReferenceType())) {
            apiId = page.getReferenceId();
        }

        return validateSafeContent(executionContext, convert(page), apiId);
    }

    @Override
    public List<String> validateSafeContent(ExecutionContext executionContext, PageEntity pageEntity, String apiId) {
        if (pageEntity != null) {
            var pageType = pageEntity.getType();

            if (pageEntity.getParentId() != null && PageType.TRANSLATION.name().equals(pageType)) {
                final Optional<Page> optParent;
                try {
                    optParent = pageRepository.findById(pageEntity.getParentId());
                    if (optParent.isPresent()) {
                        pageType = optParent.get().getType();
                    }
                } catch (TechnicalException e) {
                    logger.error("An error occurs while trying to fetch parent page");
                }
            }

            if (markdownSanitize && PageType.MARKDOWN.name().equals(pageType)) {
                this.transformWithTemplate(executionContext, pageEntity, apiId);
                if (!CollectionUtils.isEmpty(pageEntity.getMessages())) {
                    return Arrays.asList(pageEntity.getMessages().toString());
                }
                HtmlSanitizer.SanitizeInfos sanitizeInfos = HtmlSanitizer.isSafe(pageEntity.getContent());
                if (!sanitizeInfos.isSafe()) {
                    throw new PageContentUnsafeException(sanitizeInfos.getRejectedMessage());
                }
            } else if (PageType.SWAGGER.name().equals(pageEntity.getType()) && pageEntity.getContent() != null) {
                OAIDescriptor openApiDescriptor = new OAIParser().parse(pageEntity.getContent());
                if (openApiDescriptor != null && openApiDescriptor.getMessages() != null) {
                    return openApiDescriptor.getMessages();
                }
            }
        }
        return new ArrayList<>();
    }

    private void validateSafeSource(PageSourceEntity source) {
        if (importConfiguration.isAllowImportFromPrivate() || source == null || source.getConfiguration() == null) {
            return;
        }

        Map<String, String> map;

        try {
            map = new ObjectMapper().readValue(source.getConfiguration(), new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            throw new InvalidDataException("Source is invalid", e);
        }

        Optional<String> urlOpt = map
            .entrySet()
            .stream()
            .filter(e -> e.getKey().equals("repository") || e.getKey().matches(".*[uU]rl"))
            .map(Map.Entry::getValue)
            .findFirst();

        if (!urlOpt.isPresent()) {
            // There is no source to validate.
            return;
        }

        // Validate the url is allowed.
        UrlSanitizerUtils.checkAllowed(urlOpt.get(), importConfiguration.getImportWhitelist(), false);
    }

    private void createAuditLog(
        ExecutionContext executionContext,
        String apiId,
        Audit.AuditEvent event,
        Date createdAt,
        Page oldValue,
        Page newValue
    ) {
        String pageId = oldValue != null ? oldValue.getId() : newValue.getId();
        if (apiId == null) {
            auditService.createAuditLog(executionContext, Collections.singletonMap(PAGE, pageId), event, createdAt, oldValue, newValue);
        } else {
            auditService.createApiAuditLog(
                executionContext,
                apiId,
                Collections.singletonMap(PAGE, pageId),
                event,
                createdAt,
                oldValue,
                newValue
            );
        }
    }

    private PageCriteria queryToCriteria(PageQuery query, String environmentId) {
        final PageCriteria.Builder builder = new PageCriteria.Builder();
        if (query != null) {
            builder.homepage(query.getHomepage());
            if (query.getApi() != null) {
                builder.referenceId(query.getApi());
                builder.referenceType(PageReferenceType.API.name());
            } else {
                builder.referenceId(environmentId);
                builder.referenceType(PageReferenceType.ENVIRONMENT.name());
            }
            builder.name(query.getName());
            builder.parent(query.getParent());

            builder.published(query.getPublished());
            if (!isAuthenticated()) {
                builder.visibility(Visibility.PUBLIC.name());
            }
            if (query.getType() != null) {
                builder.type(query.getType().name());
            }
            builder.rootParent(query.getRootParent());
        }
        return builder.build();
    }

    @Override
    public Map<SystemFolderType, String> initialize(ExecutionContext executionContext) {
        Map<SystemFolderType, String> result = new HashMap<>();

        result.put(SystemFolderType.HEADER, createSystemFolder(executionContext, null, SystemFolderType.HEADER, 1).getId());
        result.put(SystemFolderType.TOPFOOTER, createSystemFolder(executionContext, null, SystemFolderType.TOPFOOTER, 2).getId());
        result.put(SystemFolderType.FOOTER, createSystemFolder(executionContext, null, SystemFolderType.FOOTER, 3).getId());
        return result;
    }

    @Override
    public PageEntity createAsideFolder(final ExecutionContext executionContext, String apiId) {
        return createSystemFolder(executionContext, apiId, SystemFolderType.ASIDE, 0);
    }

    @Override
    public PageEntity createSystemFolder(
        final ExecutionContext executionContext,
        String apiId,
        SystemFolderType systemFolderType,
        int order
    ) {
        NewPageEntity newSysFolder = new NewPageEntity();
        newSysFolder.setName(systemFolderType.folderName());
        newSysFolder.setOrder(order);
        newSysFolder.setPublished(true);
        newSysFolder.setType(PageType.SYSTEM_FOLDER);
        newSysFolder.setVisibility(Visibility.PUBLIC);
        return createPage(executionContext, apiId, newSysFolder);
    }

    @Override
    public boolean shouldHaveRevision(String pageType) {
        PageType type = PageType.valueOf(pageType);
        switch (type) {
            case MARKDOWN:
            case SWAGGER:
            case TRANSLATION:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Optional<PageEntity> attachMedia(String pageId, String mediaId, String mediaName) {
        try {
            final Optional<Page> optPage = pageRepository.findById(pageId);
            if (optPage.isPresent()) {
                final Page page = optPage.get();
                if (page.getAttachedMedia() == null) {
                    page.setAttachedMedia(new ArrayList<>());
                }
                page.getAttachedMedia().add(new PageMedia(mediaId, mediaName, new Date()));
                final Page updatedPage = pageRepository.update(page);
                return Optional.of(convert(updatedPage));
            } else {
                return Optional.empty();
            }
        } catch (TechnicalException ex) {
            throw onUpdateFail(pageId, ex);
        }
    }

    @Override
    public boolean folderHasPublishedChildren(String folderId) {
        try {
            var count = pageRepository.countByParentIdAndIsPublished(folderId);
            return count > 0;
        } catch (TechnicalException e) {
            logger.error("An error occurs while trying to count Pages with parentId {}", folderId, e);
            throw new TechnicalManagementException("An error occurred while evaluating if folder has children");
        }
    }

    @Override
    public boolean isMediaUsedInPages(ExecutionContext executionContext, String mediaHash) {
        try {
            final int pageSize = 100;
            int pageNumber = 0;
            io.gravitee.common.data.domain.Page<Page> pages;
            do {
                var pageable = new PageableImpl(pageNumber, pageSize);
                pages = pageRepository.findAll(convert(pageable));
                var pageList = pages.getContent();

                boolean mediaUsed = pageList.stream().anyMatch(page -> isMediaUsedInPage(page, mediaHash));
                if (mediaUsed) {
                    return true;
                }
                ++pageNumber;
            } while (!pages.getContent().isEmpty());
            return false;
        } catch (TechnicalException e) {
            logger.error("An error occurred while checking if media is used in pages", e);
            throw new TechnicalManagementException("An error occurred while checking if media is used in pages", e);
        }
    }

    private boolean isMediaUsedInPage(Page page, String mediaHash) {
        var mediaList = page.getAttachedMedia();
        if (mediaList != null && mediaList.stream().anyMatch(media -> media.getMediaHash().equals(mediaHash))) {
            return true;
        }

        String pageContent = page.getContent();
        return pageContent != null && pageContent.contains(mediaHash);
    }

    @Override
    public PageEntity createWithDefinition(final ExecutionContext executionContext, String apiId, String pageDefinition) {
        try {
            final NewPageEntity newPage = convertToEntity(pageDefinition);
            JsonNode jsonNode = objectMapper.readTree(pageDefinition);
            return createPage(executionContext, apiId, newPage, (jsonNode.get("id") != null ? jsonNode.get("id").asText() : null));
        } catch (JsonProcessingException e) {
            logger.error("An error occurs while trying to JSON deserialize the Page {}", pageDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the Page definition.");
        }
    }

    @Override
    public void createOrUpdatePages(final ExecutionContext executionContext, List<PageEntity> pages, String apiId) {
        final PageServiceImpl.PageEntityTreeNode pageEntityTreeNode = new PageServiceImpl.PageEntityTreeNode(new PageEntity());
        pageEntityTreeNode.appendListToTree(pages);
        createOrUpdateChildrenPages(executionContext, apiId, null, pageEntityTreeNode.children);
    }

    @Override
    public void createOrUpdateSwaggerPage(
        final ExecutionContext executionContext,
        String apiId,
        ImportSwaggerDescriptorEntity swaggerDescriptor,
        boolean isForCreation
    ) {
        if (swaggerDescriptor != null && swaggerDescriptor.isWithDocumentation()) {
            List<PageEntity> apiDocs =
                this.search(executionContext.getEnvironmentId(), new PageQuery.Builder().api(apiId).type(PageType.SWAGGER).build());

            if (isForCreation || (apiDocs == null || apiDocs.isEmpty())) {
                final NewPageEntity page = new NewPageEntity();
                page.setName("Swagger");
                page.setType(SWAGGER);
                page.setOrder(1);
                if (INLINE.equals(swaggerDescriptor.getType())) {
                    page.setContent(swaggerDescriptor.getPayload());
                } else {
                    final PageSourceEntity source = new PageSourceEntity();
                    page.setSource(source);
                    source.setType("http-fetcher");
                    source.setConfiguration(objectMapper.convertValue(singletonMap("url", swaggerDescriptor.getPayload()), JsonNode.class));
                }
                this.createPage(executionContext, apiId, page);
            } else if (apiDocs.size() == 1) {
                PageEntity pageToUpdate = apiDocs.get(0);
                final UpdatePageEntity page = new UpdatePageEntity();
                page.setName(pageToUpdate.getName());
                page.setOrder(pageToUpdate.getOrder());
                page.setHomepage(pageToUpdate.isHomepage());
                page.setPublished(pageToUpdate.isPublished());
                page.setParentId(pageToUpdate.getParentId());
                page.setConfiguration(pageToUpdate.getConfiguration());
                if (INLINE.equals(swaggerDescriptor.getType())) {
                    page.setContent(swaggerDescriptor.getPayload());
                } else {
                    final PageSourceEntity source = new PageSourceEntity();
                    page.setSource(source);
                    source.setType("http-fetcher");
                    source.setConfiguration(objectMapper.convertValue(singletonMap("url", swaggerDescriptor.getPayload()), JsonNode.class));
                }
                this.update(executionContext, pageToUpdate.getId(), page);
            }
        }
    }

    private NewPageEntity convertToEntity(String pageDefinition) throws JsonProcessingException {
        return objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(pageDefinition, NewPageEntity.class);
    }

    private void createOrUpdateChildrenPages(
        final ExecutionContext executionContext,
        String apiId,
        String parentId,
        List<PageEntityTreeNode> children
    ) {
        for (final PageServiceImpl.PageEntityTreeNode child : children) {
            PageEntity pageEntityToImport = child.data;
            pageEntityToImport.setParentId(parentId);
            pageEntityToImport.setReferenceId(apiId);
            pageEntityToImport.setReferenceType(PageReferenceType.API.name());
            PageEntity createdOrUpdatedPage;

            if (SYSTEM_FOLDER.name().equals(pageEntityToImport.getType())) {
                findSystemFolder(executionContext.getEnvironmentId(), apiId)
                    .ifPresent(sysFolder -> {
                        if (!sysFolder.getId().equals(pageEntityToImport.getId())) {
                            logger.warn(
                                "An existing system folder has been found for API [{}] on environment [{}] with another ID, the existing system folder will be updated",
                                apiId,
                                executionContext.getEnvironmentId()
                            );
                        }
                        pageEntityToImport.setId(sysFolder.getId());
                    });
            }

            try {
                findById(pageEntityToImport.getId());
                createdOrUpdatedPage =
                    update(executionContext, pageEntityToImport.getId(), pageConverter.toUpdatePageEntity(pageEntityToImport));
            } catch (PageNotFoundException e) {
                createdOrUpdatedPage =
                    createPage(executionContext, apiId, pageConverter.toNewPageEntity(pageEntityToImport), pageEntityToImport.getId());
            }

            if (child.children != null && !child.children.isEmpty()) {
                this.createOrUpdateChildrenPages(executionContext, apiId, createdOrUpdatedPage.getId(), child.children);
            }
        }
    }

    private enum PageSituation {
        ROOT,
        IN_ROOT,
        IN_FOLDER_IN_ROOT,
        IN_FOLDER_IN_FOLDER,
        SYSTEM_FOLDER,
        IN_SYSTEM_FOLDER,
        IN_FOLDER_IN_SYSTEM_FOLDER,
        TRANSLATION,
    }

    static class PageEntityTreeNode {

        PageEntity data;
        PageEntityTreeNode parent;

        List<PageEntityTreeNode> children;

        public PageEntityTreeNode(PageEntity data) {
            this.data = data;
            this.children = new LinkedList<>();
        }

        public PageEntityTreeNode addChild(PageEntity child) {
            PageEntityTreeNode childNode = new PageEntityTreeNode(child);
            childNode.parent = this;
            this.children.add(childNode);
            return childNode;
        }

        private PageEntityTreeNode findById(String id) {
            if (id.equals(data.getId())) {
                return this;
            }
            for (PageEntityTreeNode child : children) {
                PageEntityTreeNode result = child.findById(id);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        public PageEntityTreeNode appendListToTree(List<PageEntity> pagesList) {
            List<PageEntity> orphans = new ArrayList<>();
            for (PageEntity newPage : pagesList) {
                if (newPage.getParentId() == null || newPage.getParentId().isEmpty()) {
                    this.addChild(newPage);
                } else {
                    PageEntityTreeNode parentNode = this.findById(newPage.getParentId());
                    if (parentNode != null) {
                        parentNode.addChild(newPage);
                    } else {
                        orphans.add(newPage);
                    }
                }
            }
            if (!orphans.isEmpty() && orphans.size() < pagesList.size()) {
                appendListToTree(orphans);
            }

            return this;
        }
    }
}
