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
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.exceptions.PageAlreadyExistsException;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.plugin.fetcher.FetcherPluginManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageConfiguration;
import io.gravitee.repository.management.model.PageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PageServiceImpl extends TransactionalService implements PageService {

	private static final Gson gson = new Gson();

	private static final Logger logger = LoggerFactory.getLogger(PageServiceImpl.class);

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private ApiService apiService;

	@Autowired
	private FetcherPluginManager fetcherPluginManager;

	@Autowired
	private FetcherConfigurationFactory fetcherConfigurationFactory;

	@Autowired
	private Configuration freemarkerConfiguration;

	@Override
	public List<PageListItem> findApiPagesByApi(String apiId) {
	    return findApiPagesByApiAndHomepage(apiId, null);
    }

	@Override
	public List<PageListItem> findPortalPages() {
	    return findPortalPagesByHomepage(null);
    }

	@Override
	public List<PageListItem> findApiPagesByApiAndHomepage(String apiId, Boolean homepage) {
		try {
			final Collection<Page> pages;
			if (homepage == null) {
				pages = pageRepository.findApiPageByApiId(apiId);
			} else {
				pages = pageRepository.findApiPageByApiIdAndHomepage(apiId, homepage);
			}
			if (pages == null) {
				return emptyList();
			}

			return pages.stream()
					.map(this::reduce)
					.sorted(Comparator.comparingInt(PageListItem::getOrder))
					.collect(Collectors.toList());

		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to get API pages using api ID {}", apiId, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to get API pages using api ID " + apiId, ex);
		}
	}

	@Override
	public List<PageListItem> findPortalPagesByHomepage(Boolean homepage) {
		try {
			final Collection<Page> pages;
			if (homepage == null) {
				pages = pageRepository.findPortalPages();
			} else {
				pages = pageRepository.findPortalPageByHomepage(homepage);
			}
			if (pages == null) {
				return emptyList();
			}

			return pages.stream()
					.map(this::reduce)
					.sorted(Comparator.comparingInt(PageListItem::getOrder))
					.collect(Collectors.toList());

		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to get Portal pages", ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to get Portal pages", ex);
		}
	}

	@Override
	public PageEntity findById(String pageId) {
		return findById(pageId, false);
	}

	@Override
	public PageEntity findById(String pageId, boolean transform) {
		try {
			logger.debug("Find page by ID: {}", pageId);

			Optional<Page> page = pageRepository.findById(pageId);

			if (page.isPresent()) {
				PageEntity pageEntity = convert(page.get());
				if (transform) {
					transformWithTemplate(pageEntity, page.get().getApi());
				}

				return pageEntity;
			}

			throw new PageNotFoundException(pageId);
		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to find a page using its ID {}", pageId, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to find a page using its ID " + pageId, ex);
		}
	}

	private void transformWithTemplate(PageEntity pageEntity, String api) {
		try {
			Template template = new Template(pageEntity.getId(), pageEntity.getContent(), freemarkerConfiguration);

			ApiEntity apiEntity = apiService.findById(api);
			Map<String, Object> model = new HashMap<>();
			model.put("api", apiEntity);

			final String content =
					FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

			pageEntity.setContent(content);
		} catch (IOException | TemplateException ex) {
			logger.error("An error occurs while transforming page content for {}", pageEntity.getId(), ex);
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
			return convert(createdPage);
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
			return convert(createdPage);
		} catch (TechnicalException | FetcherException ex) {
			logger.error("An error occurs while trying to create {}", newPageEntity, ex);
			throw new TechnicalManagementException("An error occurs while trying create " + newPageEntity, ex);
		}
	}

	private void onlyOneHomepage(Page page) throws TechnicalException {
		if(page.isHomepage()) {
			Collection<Page> pages =
					page.getApi() != null ?
					pageRepository.findApiPageByApiIdAndHomepage(page.getApi(), true) :
					pageRepository.findPortalPageByHomepage(true);
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
		try {
			logger.debug("Update Page {}", pageId);

			Optional<Page> optPageToUpdate = pageRepository.findById(pageId);
			if (!optPageToUpdate.isPresent()) {
				throw new PageNotFoundException(pageId);
			}

			Page pageToUpdate = optPageToUpdate.get();
			Page page = convert(updatePageEntity);

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
				return convert(updatedPage);
			}
		} catch (TechnicalException ex) {
            throw onUpdateFail(pageId, ex);
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
		final Collection<Page> pages = pageRepository.findApiPageByApiId(pageToReorder.getApi());
        final List<Boolean> increment = asList(true);
        pages.stream()
            .sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
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
		return new TechnicalManagementException("An error occurs while trying to update page " + pageId, ex);
	}

    @Override
	public void delete(String pageName) {
		try {
			logger.debug("Delete PAGE : {}", pageName);
			pageRepository.delete(pageName);
		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to delete PAGE {}", pageName, ex);
			throw new TechnicalManagementException("An error occurs while trying to delete PAGE " + pageName, ex);
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

	private PageListItem reduce(Page page) {
		PageListItem pageItem = new PageListItem();

		pageItem.setId(page.getId());
		pageItem.setName(page.getName());
		pageItem.setType(PageType.valueOf(page.getType().toString()));
		pageItem.setOrder(page.getOrder());
		pageItem.setLastContributor(page.getLastContributor());
		pageItem.setPublished(page.isPublished());
		pageItem.setHomepage(page.isHomepage());
		pageItem.setSource(convert(page.getSource()));
		pageItem.setConfiguration(convert(page.getConfiguration()));

		return pageItem;
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
		page.setConfiguration(convert(newPageEntity.getConfiguration()));

		return page;
	}

	private static PageEntity convert(Page page) {
		PageEntity pageEntity = new PageEntity();

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
			pageEntity.setConfiguration(convert(page.getConfiguration()));
		}
		return pageEntity;
	}

	private static Page convert(UpdatePageEntity updatePageEntity) {
		Page page = new Page();

		page.setName(updatePageEntity.getName());
		page.setContent(updatePageEntity.getContent());
        page.setLastContributor(updatePageEntity.getLastContributor());
		page.setOrder(updatePageEntity.getOrder());
		page.setPublished(updatePageEntity.isPublished());
		page.setSource(convert(updatePageEntity.getSource()));
        page.setConfiguration(convert(updatePageEntity.getConfiguration()));
        page.setHomepage(updatePageEntity.isHomepage());
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

	private static PageConfiguration convert(PageConfigurationEntity pageConfigurationEntity){
		PageConfiguration configuration = null;
		if(pageConfigurationEntity != null) {
			configuration = new PageConfiguration();
			configuration.setTryIt(pageConfigurationEntity.isTryIt());
			configuration.setTryItURL(pageConfigurationEntity.getTryItURL());
		}
		return configuration;
	}

	private static PageConfigurationEntity convert(PageConfiguration pageConfiguration){
		PageConfigurationEntity configurationEntity = null;
		if(pageConfiguration != null) {
			configurationEntity = new PageConfigurationEntity();
			configurationEntity.setTryIt(pageConfiguration.isTryIt());
			configurationEntity.setTryItURL(pageConfiguration.getTryItURL());
		}
		return configurationEntity;
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

}
