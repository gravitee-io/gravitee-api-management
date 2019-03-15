/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.utils.UUID;
import io.gravitee.fetcher.api.Fetcher;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.management.fetcher.FetcherConfigurationFactory;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.documentation.PageQuery;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PAGE;
import static io.gravitee.repository.management.model.Page.AuditEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Guillaume Gillon
 * @author GraviteeSource Team
 */
@Component
public class PageServiceImpl extends TransactionalService implements PageService, ApplicationContextAware {

	private static final Gson gson = new Gson();

	private static final Logger logger = LoggerFactory.getLogger(PageServiceImpl.class);

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private ApiService apiService;

	@Autowired
	private SwaggerService swaggerService;

	@Autowired
	private PluginManager<FetcherPlugin> fetcherPluginManager;

	@Autowired
	private FetcherConfigurationFactory fetcherConfigurationFactory;

	@Autowired
	private Configuration freemarkerConfiguration;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private MembershipService membershipService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private AuditService auditService;

	@Autowired
	private SearchEngineService searchEngineService;

	@Override
	public PageEntity findById(String pageId) {
		try {
			logger.debug("Find page by ID: {}", pageId);

			Optional<Page> page = pageRepository.findById(pageId);

			if (page.isPresent()) {
				PageEntity pageEntity = convert(page.get());
				return pageEntity;
			}

			throw new PageNotFoundException(pageId);
		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to find a page using its ID {}", pageId, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to find a page using its ID " + pageId, ex);
		}
	}

	@Override
	public void transformSwagger(PageEntity pageEntity) {
		transformSwagger(pageEntity, null);
    }

	@Override
	public void transformSwagger(PageEntity pageEntity, String apiId) {
		transformUsingConfiguration(pageEntity);
		if (apiId != null) {
			transformWithTemplate(pageEntity, apiId);
		}
	}

	@Override
	public List<PageEntity> search(PageQuery query) {
		try {
			return convert(pageRepository.search(queryToCriteria(query)));
		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to search pages", ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to search pages", ex);
		}
	}

	private void transformUsingConfiguration(final PageEntity pageEntity) {
		if (io.gravitee.repository.management.model.PageType.SWAGGER.name().equalsIgnoreCase(pageEntity.getType())) {
			swaggerService.transform(pageEntity);
		}
	}

	private void transformWithTemplate(final PageEntity pageEntity, final String api) {
		if (pageEntity.getContent() != null) {
			try {
				Template template = new Template(pageEntity.getId(), pageEntity.getContent(), freemarkerConfiguration);

				ApiModelEntity apiEntity = apiService.findByIdForTemplates(api);
				Map<String, Object> model = new HashMap<>();
				model.put("api", apiEntity);

				final String content =
						FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

				pageEntity.setContent(content);
			} catch (IOException | TemplateException ex) {
				logger.error("An error occurs while transforming page content for {}", pageEntity.getId(), ex);
			}
		}
	}

	@Override
	public PageEntity createApiPage(String apiId, NewPageEntity newPageEntity) {
		try {
			logger.debug("Create page {} for API {}", newPageEntity, apiId);

			String id = UUID.toString(UUID.random());
			Optional<Page> checkPage = pageRepository.findById(id);
			if (checkPage.isPresent()) {
				throw new PageAlreadyExistsException(id);
			}

            if (PageType.FOLDER.equals(newPageEntity.getType())) {

                if (newPageEntity.getContent() != null  && newPageEntity.getContent().length() > 0) {
                    throw new PageFolderActionException("have a content");
                }

                if (newPageEntity.isHomepage()) {
                    throw new PageFolderActionException("be affected to the home page");
                }
            }

			Page page = convert(newPageEntity);

			if (page.getSource() != null) {
				String fetchedContent = this.getContentFromFetcher(page.getSource());
				if (fetchedContent != null && !fetchedContent.isEmpty()) {
					page.setContent(fetchedContent);
				}
			}

			page.setId(id);
			page.setApi(apiId);

			// Set date fields
			page.setCreatedAt(new Date());
			page.setUpdatedAt(page.getCreatedAt());

			Page createdPage = pageRepository.create(page);

			//only one homepage is allowed
			onlyOneHomepage(page);
			createAuditLog(apiId, PAGE_CREATED, page.getCreatedAt(), null, page);
			PageEntity pageEntity = convert(createdPage);

			// add document in search engine
			index(pageEntity);

			return pageEntity;
		} catch (TechnicalException | FetcherException ex) {
			logger.error("An error occurs while trying to create {}", newPageEntity, ex);
			throw new TechnicalManagementException("An error occurs while trying create " + newPageEntity, ex);
		}
	}

	@Override
	public PageEntity createPortalPage(NewPageEntity newPageEntity) {
		try {
			logger.debug("Create portal page {}", newPageEntity);

			String id = UUID.toString(UUID.random());
			Optional<Page> checkPage = pageRepository.findById(id);
			if (checkPage.isPresent()) {
				throw new PageAlreadyExistsException(id);
			}

			if (PageType.FOLDER.equals(newPageEntity.getType())) {

			    if (newPageEntity.getContent() != null  && newPageEntity.getContent().length() > 0) {
                    throw new PageFolderActionException("have a content");
                }

                if (newPageEntity.isHomepage()) {
                    throw new PageFolderActionException("be affected to the home page");
                }
            }

			Page page = convert(newPageEntity);

			if (page.getSource() != null) {
				String fetchedContent = this.getContentFromFetcher(page.getSource());
				if (fetchedContent != null && !fetchedContent.isEmpty()) {
					page.setContent(fetchedContent);
				}
			}

			page.setId(id);

			// Set date fields
			page.setCreatedAt(new Date());
			page.setUpdatedAt(page.getCreatedAt());

			Page createdPage = pageRepository.create(page);

			//only one homepage is allowed
			onlyOneHomepage(page);
			createAuditLog(null, PAGE_CREATED, page.getCreatedAt(), null, page);

			PageEntity pageEntity = convert(createdPage);

			// add document in search engine
			index(pageEntity);

			return pageEntity;
		} catch (TechnicalException | FetcherException ex) {
			logger.error("An error occurs while trying to create {}", newPageEntity, ex);
			throw new TechnicalManagementException("An error occurs while trying create " + newPageEntity, ex);
		}
	}

	private void onlyOneHomepage(Page page) throws TechnicalException {
		if(page.isHomepage()) {
			Collection<Page> pages =
					page.getApi() != null ?
							pageRepository.search(new PageCriteria.Builder().api(page.getApi()).homepage(true).build()) :
							pageRepository.search(new PageCriteria.Builder().homepage(true).build());
			pages.stream().
					filter(i -> !i.getId().equals(page.getId())).
					forEach(i -> {
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
    public PageEntity update(String pageId, UpdatePageEntity updatePageEntity) {
        return this.update(pageId, updatePageEntity, false);
    }

    @Override
    public PageEntity update(String pageId, UpdatePageEntity updatePageEntity, boolean partial) {
        try {
			logger.debug("Update Page {}", pageId);

			Optional<Page> optPageToUpdate = pageRepository.findById(pageId);
			if (!optPageToUpdate.isPresent()) {
				throw new PageNotFoundException(pageId);
			}

			Page pageToUpdate = optPageToUpdate.get();
			Page page = null;

            if(partial) {
                page = merge(updatePageEntity, pageToUpdate);
            } else {
                page = convert(updatePageEntity);
			}

            if (page.getSource() != null) {
				try {
					String fetchedContent = this.getContentFromFetcher(page.getSource());
                    if (fetchedContent != null && !fetchedContent.isEmpty()) {
                    	page.setContent(fetchedContent);
					}
				} catch (FetcherException e) {
					throw onUpdateFail(pageId, e);
				}
			}

			page.setId(pageId);
			page.setUpdatedAt(new Date());

			// Copy fields from existing values
			page.setCreatedAt(pageToUpdate.getCreatedAt());
			page.setType(pageToUpdate.getType());
			page.setApi(pageToUpdate.getApi());

			onlyOneHomepage(page);
			// if order change, reorder all pages
			if (page.getOrder() != pageToUpdate.getOrder()) {
				reorderAndSavePages(page);
				return null;
			} else {
				Page updatedPage = pageRepository.update(page);
				createAuditLog(page.getApi(), PAGE_UPDATED, page.getUpdatedAt(), pageToUpdate, page);

				PageEntity pageEntity = convert(updatedPage);

				// update document in search engine
                if(pageToUpdate.isPublished() && !page.isPublished()) {
                	searchEngineService.delete(convert(pageToUpdate));
				} else {
					index(pageEntity);
				}

				return pageEntity;
			}
		} catch (TechnicalException ex) {
            throw onUpdateFail(pageId, ex);
		}
	}

	private void index(PageEntity pageEntity) {
		if (pageEntity.isPublished()) {
			searchEngineService.index(pageEntity);
		}
	}

	private String getContentFromFetcher(PageSource ps) throws FetcherException {
		if (ps.getConfiguration().isEmpty()) {
			return null;
		}
		try {
			FetcherPlugin fetcherPlugin = fetcherPluginManager.get(ps.getType());
			ClassLoader fetcherCL = fetcherPlugin.fetcher().getClassLoader();
			Class<? extends FetcherConfiguration> fetcherConfigurationClass = (Class<? extends FetcherConfiguration>) fetcherCL.loadClass(fetcherPlugin.configuration().getName());
			Class<? extends Fetcher> fetcherClass = (Class<? extends Fetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());
			FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(fetcherConfigurationClass, ps.getConfiguration());
			Fetcher fetcher = fetcherClass.getConstructor(fetcherConfigurationClass).newInstance(fetcherConfigurationInstance);
			// Autowire fetcher
			applicationContext.getAutowireCapableBeanFactory().autowireBean(fetcher);

			StringBuilder sb = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(fetcher.fetch()))) {
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

    private void reorderAndSavePages(final Page pageToReorder) throws TechnicalException {
		PageCriteria.Builder q = new PageCriteria.Builder().api(pageToReorder.getApi());
		if (pageToReorder.getParentId() == null) {
			q.rootParent(Boolean.TRUE);
		} else {
			q.parent(pageToReorder.getParentId());
		}
		final Collection<Page> pages = pageRepository.search(q.build());
        final List<Boolean> increment = asList(true);
        pages.stream()
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
			            } else if (page.getOrder() > pageToReorder.getOrder())  {
				            newOrder = page.getOrder() + (isIncrement? 1 : 0);
			            } else {
				            newOrder = page.getOrder() + (isIncrement? 1 : -1);
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

    @Override
	public void delete(String pageId) {
		try {
			logger.debug("Delete Page : {}", pageId);
			Optional<Page> optPage = pageRepository.findById(pageId);
			if ( !optPage.isPresent()) {
				throw new PageNotFoundException(pageId);
			}

			Page page = optPage.get();

			// if the folder is not empty, throw exception
			if (io.gravitee.repository.management.model.PageType.FOLDER.equals(page.getType()) &&
					pageRepository.search(new PageCriteria.Builder()
							.api(page.getApi())
							.parent(page.getId()).build()).size() > 0 ) {
				throw new TechnicalManagementException("Unable to remove the folder. It must be empty before being removed.");
			}

			pageRepository.delete(pageId);
            createAuditLog(page.getApi(), PAGE_DELETED, new Date(), page, null);

            // remove from search engine
			searchEngineService.delete(convert(page));
		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to delete Page {}", pageId, ex);
			throw new TechnicalManagementException("An error occurs while trying to delete Page " + pageId, ex);
		}
	}

	@Override
	public int findMaxApiPageOrderByApi(String apiName) {
		try {
			logger.debug("Find Max Order Page for api name : {}", apiName);
			final Integer maxPageOrder = pageRepository.findMaxApiPageOrderByApiId(apiName);
			return maxPageOrder == null ? 0 : maxPageOrder;
		} catch (TechnicalException ex) {
			logger.error("An error occured when searching max order page for api name [{}]", apiName, ex);
			throw new TechnicalManagementException("An error occured when searching max order page for api name " + apiName, ex);
		}
	}

	@Override
	public int findMaxPortalPageOrder() {
		try {
			logger.debug("Find Max Order Portal Page");
			final Integer maxPageOrder = pageRepository.findMaxPortalPageOrder();
			return maxPageOrder == null ? 0 : maxPageOrder;
		} catch (TechnicalException ex) {
			logger.error("An error occured when searching max order portal page", ex);
			throw new TechnicalManagementException("An error occured when searching max order portal ", ex);
		}
	}

	@Override
	public boolean isDisplayable(ApiEntity api, boolean pageIsPublished, String username) {
		boolean isDisplayable = false;
		if (api.getVisibility() == Visibility.PUBLIC && pageIsPublished) {
			isDisplayable = true;
		} else if (username != null) {
			MemberEntity member = membershipService.getMember(MembershipReferenceType.API, api.getId(), username, io.gravitee.repository.management.model.RoleScope.API);
			if (member == null && api.getGroups() != null) {
				Iterator<String> groupIdIterator = api.getGroups().iterator();
				while (!isDisplayable && groupIdIterator.hasNext()) {
					String groupId = groupIdIterator.next();
					member = membershipService.getMember(MembershipReferenceType.GROUP, groupId, username, io.gravitee.repository.management.model.RoleScope.API);
					isDisplayable = isDisplayableForMember(member, pageIsPublished);
				}
			} else {
				isDisplayable = isDisplayableForMember(member, pageIsPublished);
			}
		}
		return isDisplayable;
	}

	@Override
	public PageEntity fetch(String pageId, String contributor) {
		try {
			logger.debug("Fetch page {}", pageId);

			Optional<Page> optPageToUpdate = pageRepository.findById(pageId);
			if (!optPageToUpdate.isPresent()) {
				throw new PageNotFoundException(pageId);
			}

			Page page = optPageToUpdate.get();

			if (page.getSource() == null) {
				throw new NoFetcherDefinedException(pageId);
			}

			try {
				String fetchedContent = this.getContentFromFetcher(page.getSource());
				if (fetchedContent != null && !fetchedContent.isEmpty()) {
					page.setContent(fetchedContent);
				}
			} catch (FetcherException e) {
				throw onUpdateFail(pageId, e);
			}

			page.setUpdatedAt(new Date());
			page.setLastContributor(contributor);

			Page updatedPage = pageRepository.update(page);
			createAuditLog(page.getApi(), PAGE_UPDATED, page.getUpdatedAt(), page, page);
			return convert(updatedPage);
		} catch (TechnicalException ex) {
			throw onUpdateFail(pageId, ex);
		}
	}

	private boolean isDisplayableForMember(MemberEntity member, boolean pageIsPublished) {
	    // if not member => not displayable
		if (member == null) {
			return false;
		}
		// if member && published page => displayable
		if (pageIsPublished) {
			return true;
		}

		// only members which could modify a page can see an unpublished page
		return roleService.hasPermission(
				member.getPermissions(),
				ApiPermission.DOCUMENTATION,
				new RolePermissionAction[]{
						RolePermissionAction.UPDATE,
						RolePermissionAction.CREATE,
						RolePermissionAction.DELETE});
	}

	private static Page convert(NewPageEntity newPageEntity) {
		Page page = new Page();

		page.setName(newPageEntity.getName());
		final PageType type = newPageEntity.getType();
		if (type != null) {
			page.setType(io.gravitee.repository.management.model.PageType.valueOf(type.name()));
		}
		page.setContent(newPageEntity.getContent());
		page.setLastContributor(newPageEntity.getLastContributor());
		page.setOrder(newPageEntity.getOrder());
		page.setPublished(newPageEntity.isPublished());
		page.setHomepage(newPageEntity.isHomepage());
		page.setSource(convert(newPageEntity.getSource()));
		page.setConfiguration(newPageEntity.getConfiguration());
		page.setExcludedGroups(newPageEntity.getExcludedGroups());
		page.setParentId("".equals(newPageEntity.getParentId()) ? null : newPageEntity.getParentId());

		return page;
	}

	private static List<PageEntity> convert(List<Page> pages) {
		if (pages == null) {
			return emptyList();
		}

		return pages.stream().map(PageServiceImpl::convert).collect(Collectors.toList());
	}

	private static PageEntity convert(Page page) {
		PageEntity pageEntity;

		if (page.getApi() != null) {
			pageEntity = new ApiPageEntity();
			((ApiPageEntity) pageEntity).setApi(page.getApi());
		} else {
			pageEntity = new PageEntity();
		}

		pageEntity.setId(page.getId());
		pageEntity.setName(page.getName());
		pageEntity.setHomepage(page.isHomepage());
		if (page.getType() != null) {
			pageEntity.setType(page.getType().toString());
		}
		pageEntity.setContent(page.getContent());

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

		if (page.getSource() != null) {
			pageEntity.setSource(convert(page.getSource()));
		}
		if (page.getConfiguration() != null) {
			pageEntity.setConfiguration(page.getConfiguration());
		}
		pageEntity.setExcludedGroups(page.getExcludedGroups());
		pageEntity.setParentId("".equals(page.getParentId()) ? null : page.getParentId());
		return pageEntity;
	}

    private static Page merge(UpdatePageEntity updatePageEntity, Page withUpdatePage) {

        Page page = new Page();

        page.setName(
                updatePageEntity.getName() != null ? updatePageEntity.getName() : withUpdatePage.getName()
        );
        page.setContent(
                updatePageEntity.getContent() != null ? updatePageEntity.getContent() : withUpdatePage.getContent()
        );
        page.setLastContributor(
                updatePageEntity.getLastContributor() != null ? updatePageEntity.getLastContributor() : withUpdatePage.getLastContributor()
        );
        page.setOrder(
                updatePageEntity.getOrder() != null ? updatePageEntity.getOrder() : withUpdatePage.getOrder()
        );
        page.setPublished(
                updatePageEntity.isPublished() != null ? updatePageEntity.isPublished() : withUpdatePage.isPublished()
        );

        PageSource pageSource = convert(updatePageEntity.getSource());
        page.setSource(
                pageSource != null ? pageSource : withUpdatePage.getSource()
        );
        page.setConfiguration(
                updatePageEntity.getConfiguration() != null ? updatePageEntity.getConfiguration() : withUpdatePage.getConfiguration()
        );
        page.setHomepage(
                updatePageEntity.isHomepage() != null ? updatePageEntity.isHomepage() : withUpdatePage.isHomepage()
        );
        page.setExcludedGroups(
                updatePageEntity.getExcludedGroups() != null ? updatePageEntity.getExcludedGroups() : withUpdatePage.getExcludedGroups()
        );
        page.setParentId(
                updatePageEntity.getParentId() != null ?
						updatePageEntity.getParentId().isEmpty() ?
							null :updatePageEntity.getParentId()
						: withUpdatePage.getParentId()

        );

        return page;
    }

    private static Page convert(UpdatePageEntity updatePageEntity) {
		Page page = new Page();

		page.setName(updatePageEntity.getName());
		page.setContent(updatePageEntity.getContent());
        page.setLastContributor(updatePageEntity.getLastContributor());
		page.setOrder(updatePageEntity.getOrder());
		page.setPublished(Boolean.TRUE.equals(updatePageEntity.isPublished()));
		page.setSource(convert(updatePageEntity.getSource()));
        page.setConfiguration(updatePageEntity.getConfiguration());
		page.setHomepage(Boolean.TRUE.equals(updatePageEntity.isHomepage()));
        page.setExcludedGroups(updatePageEntity.getExcludedGroups());
		page.setParentId("".equals(updatePageEntity.getParentId()) ? null : updatePageEntity.getParentId());
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

	private static PageSourceEntity convert(PageSource pageSource) {
		PageSourceEntity entity = null;
		if (pageSource != null) {
			entity = new PageSourceEntity();
			entity.setType(pageSource.getType());
			try {
				entity.setConfiguration((new ObjectMapper()).readTree(pageSource.getConfiguration()));
			} catch (IOException e) {
			    logger.error(e.getMessage(), e);
			}
		}
		return entity;
	}

	@SuppressWarnings("squid:S1166")
	private static boolean isJson(String content) {
		try {
			gson.fromJson(content, Object.class);
			return true;
		} catch(com.google.gson.JsonSyntaxException ex) {
			return false;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private void createAuditLog(String apiId, Audit.AuditEvent event, Date createdAt, Page oldValue, Page newValue) {
		String pageId = oldValue != null ? oldValue.getId() : newValue.getId();
		if (apiId == null ) {
			auditService.createPortalAuditLog(
					Collections.singletonMap(PAGE, pageId),
					event,
					createdAt,
					oldValue,
					newValue
			);
		} else {
			auditService.createApiAuditLog(
					apiId,
					Collections.singletonMap(PAGE, pageId),
					event,
					createdAt,
					oldValue,
					newValue
			);
		}
	}

	private PageCriteria queryToCriteria(PageQuery query) {
		final PageCriteria.Builder builder = new PageCriteria.Builder();
		if (query != null) {
			builder.homepage(query.getHomepage());
			builder.api(query.getApi());
			builder.name(query.getName());
			builder.parent(query.getParent());
			builder.published(query.getPublished());
			if(query.getType() != null) {
				builder.type(query.getType().name());
			}
			builder.rootParent(query.getRootParent());
		}
		return builder.build();
	}
}
