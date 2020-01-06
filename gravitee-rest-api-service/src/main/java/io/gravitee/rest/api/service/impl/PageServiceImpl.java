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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.utils.UUID;
import io.gravitee.fetcher.api.*;
import io.gravitee.plugin.core.api.PluginManager;
import io.gravitee.plugin.fetcher.FetcherPlugin;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.management.fetcher.FetcherConfigurationFactory;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.descriptor.GraviteeDescriptorEntity;
import io.gravitee.rest.api.model.descriptor.GraviteeDescriptorPageEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.search.SearchEngineService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PAGE;
import static io.gravitee.repository.management.model.Page.AuditEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

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
	@Autowired
	private MetadataService metadataService;

	@Autowired
	private GraviteeDescriptorService graviteeDescriptorService;

	private enum PageSituation {
	    ROOT, IN_ROOT, IN_FOLDER_IN_ROOT, IN_FOLDER_IN_FOLDER, SYSTEM_FOLDER, IN_SYSTEM_FOLDER, IN_FOLDER_IN_SYSTEM_FOLDER;
	}

	private PageSituation getPageSituation(String pageId) throws TechnicalException {
	    if (pageId == null) {
	        return PageSituation.ROOT;
	    } else {
	        Optional<Page> optionalPage = pageRepository.findById(pageId);
	        if (optionalPage.isPresent()) {
	            Page page = optionalPage.get();
	            if (io.gravitee.repository.management.model.PageType.SYSTEM_FOLDER == page.getType()) {
	                return PageSituation.SYSTEM_FOLDER;
	            }

                String parentId = page.getParentId();
                if (parentId == null) {
                    return PageSituation.IN_ROOT;
                }

                Optional<Page> optionalParent = pageRepository.findById(parentId);
                if (optionalParent.isPresent()) {
                    Page parentPage = optionalParent.get();
                    if (io.gravitee.repository.management.model.PageType.SYSTEM_FOLDER == parentPage.getType()) {
                        return PageSituation.IN_SYSTEM_FOLDER;
                    }

                    if (io.gravitee.repository.management.model.PageType.FOLDER == parentPage.getType()) {
                        String grandParentId = parentPage.getParentId();
                        if (grandParentId == null) {
                            return PageSituation.IN_FOLDER_IN_ROOT;
                        }

                        Optional<Page> optionalGrandParent = pageRepository.findById(grandParentId);
                        if (optionalGrandParent.isPresent()) {
                            Page grandParentPage = optionalGrandParent.get();
                            if (io.gravitee.repository.management.model.PageType.SYSTEM_FOLDER == grandParentPage.getType()) {
                                return PageSituation.IN_FOLDER_IN_SYSTEM_FOLDER;
                            }
                            if (io.gravitee.repository.management.model.PageType.FOLDER == grandParentPage.getType()) {
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
	public PageEntity findById(String pageId) {
		try {
			logger.debug("Find page by ID: {}", pageId);

			Optional<Page> page = pageRepository.findById(pageId);

			if (page.isPresent()) {
				return convert(page.get());
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
		String apiId = null;
		if (pageEntity instanceof ApiPageEntity) {
			apiId = ((ApiPageEntity) pageEntity).getApi();
		}
		transformSwagger(pageEntity, apiId);
    }

	@Override
	public void transformSwagger(PageEntity pageEntity, String apiId) {
		transformUsingConfiguration(pageEntity);
		if (apiId != null) {
			transformWithTemplate(pageEntity, apiId);
		}
	}

	@Override
	public List<PageEntity> search(final PageQuery query) {
		try {
			final List<PageEntity> pages = convert(pageRepository.search(queryToCriteria(query)));

			if (query != null && query.getPublished() != null && query.getPublished()) {
				// remove child of unpublished folders
				return pages.stream()
					.filter(page -> {
						if (page.getParentId() != null) {
							final Optional<PageEntity> optionalPage =
									pages.stream().filter(p -> p.getId().equals(page.getParentId())).findFirst();
							return optionalPage.map(PageEntity::isPublished).orElse(false);
						}
						return true;
					})
					.collect(toList());
			}

			return pages;
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

	@Override
	public void transformWithTemplate(final PageEntity pageEntity, final String api) {
		if (pageEntity.getContent() != null) {
			try {
				final Template template = new Template(pageEntity.getId(), pageEntity.getContent(), freemarkerConfiguration);
				final Map<String, Object> model = new HashMap<>();
				if (api == null) {
					final List<MetadataEntity> metadataList = metadataService.findAllDefault();
					if (metadataList != null) {
						final Map<String, String> mapMetadata = new HashMap<>(metadataList.size());
						metadataList.forEach(metadata -> mapMetadata.put(metadata.getKey(), metadata.getValue()));
						model.put("metadata", mapMetadata);
					}
				} else {
					ApiModelEntity apiEntity = apiService.findByIdForTemplates(api);
					model.put("api", apiEntity);
				}

				final String content = processTemplateIntoString(template, model);

				pageEntity.setContent(content);
			} catch (IOException | TemplateException ex) {
				logger.error("An error occurs while transforming page content for {}", pageEntity.getId(), ex);
			}
		}
	}

	@Override
	public PageEntity createPage(String apiId, NewPageEntity newPageEntity) {
		try {
			logger.debug("Create page {} for API {}", newPageEntity, apiId);

			String id = UUID.toString(UUID.random());

            if (PageType.FOLDER.equals(newPageEntity.getType())) {

                if (newPageEntity.getContent() != null  && newPageEntity.getContent().length() > 0) {
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

            if (PageType.LINK.equals(newPageEntity.getType())) {
                String resourceRef = newPageEntity.getConfiguration().get("resourceRef");
                String resourceType = newPageEntity.getConfiguration().get("resourceType");
                if("root".equals(resourceRef) || "external".equals(resourceType) || "view".equals(resourceType)) {
                    newPageEntity.setPublished(true);
                } else {
                    Optional<Page> optionalRelatedPage = pageRepository.findById(resourceRef);
                    if(optionalRelatedPage.isPresent()) {
                        Page relatedPage = optionalRelatedPage.get();
                        checkLinkRelatedPageType(relatedPage);
                        newPageEntity.setPublished(relatedPage.isPublished());
                    }
                }
            }

            if (PageType.SWAGGER == newPageEntity.getType() || PageType.MARKDOWN == newPageEntity.getType()) {
                PageSituation newPageParentSituation = getPageSituation(newPageEntity.getParentId());
                if (newPageParentSituation == PageSituation.SYSTEM_FOLDER
                        || newPageParentSituation == PageSituation.IN_SYSTEM_FOLDER) {
                    throw new PageActionException(newPageEntity.getType(), "be created under a system folder");
                }
            }

			Page page = convert(newPageEntity);

			if (page.getSource() != null) {
				fetchPage(page);
			}


			page.setId(id);
			if(StringUtils.isEmpty(apiId)) {
			    page.setReferenceId(GraviteeContext.getCurrentEnvironment());
                page.setReferenceType(PageReferenceType.ENVIRONMENT);
			} else {
			    page.setReferenceId(apiId);
			    page.setReferenceType(PageReferenceType.API);
			}
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


    private void checkLinkRelatedPageType(Page relatedPage) throws TechnicalException {
        PageSituation relatedPageSituation = getPageSituation(relatedPage.getId());

        if (io.gravitee.repository.management.model.PageType.LINK.equals(relatedPage.getType())
                || io.gravitee.repository.management.model.PageType.SYSTEM_FOLDER.equals(relatedPage.getType())
                || (io.gravitee.repository.management.model.PageType.FOLDER.equals(relatedPage.getType())
                        && relatedPageSituation == PageSituation.IN_SYSTEM_FOLDER)) {
            throw new PageActionException(PageType.LINK,
                    "be related to a Link, a System folder or a folder in a System folder");
        }
    }

	@Override
	public PageEntity createPage(NewPageEntity newPageEntity) {
		return this.createPage(null, newPageEntity);
	}

	@Override
	public PageEntity create(final String apiId, final PageEntity pageEntity) {
		final NewPageEntity newPageEntity = convert(pageEntity);
		newPageEntity.setLastContributor(null);
		return createPage(apiId, newPageEntity);
	}

	private void onlyOneHomepage(Page page) throws TechnicalException {
		if(page.isHomepage()) {
			Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceId(page.getReferenceId()).referenceType(page.getReferenceType().name()).homepage(true).build());
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

			io.gravitee.repository.management.model.PageType pageType = pageToUpdate.getType();

            if (updatePageEntity.isPublished() != null && updatePageEntity.isPublished().booleanValue() != pageToUpdate.isPublished()) {
                if (!io.gravitee.repository.management.model.PageType.LINK.equals(pageType)) {
    		        // update all the relatedLinksPublicationState.
    		        this.changeLinksPublicationStatus(pageId, updatePageEntity.isPublished());
                }
			}

            if (io.gravitee.repository.management.model.PageType.LINK == pageType && updatePageEntity.getConfiguration() != null) {
                String newResourceRef = updatePageEntity.getConfiguration().get("resourceRef");
                String actualResourceRef = pageToUpdate.getConfiguration().get("resourceRef");

                if(!newResourceRef.equals(actualResourceRef)) {
                    String resourceType = updatePageEntity.getConfiguration().get("resourceType");
                    if("root".equals(newResourceRef) || "external".equals(resourceType) || "view".equals(resourceType)) {
                        updatePageEntity.setPublished(true);
                    } else {
                        Optional<Page> optionalRelatedPage = pageRepository.findById(newResourceRef);
                        if(optionalRelatedPage.isPresent()) {
                            Page relatedPage = optionalRelatedPage.get();
                            checkLinkRelatedPageType(relatedPage);
                            updatePageEntity.setPublished(relatedPage.isPublished());
                        }
                    }
                } else {
                 // can not publish or unpublish a Link. LINK publication state is changed when the related page is updated.
                    updatePageEntity.setPublished(pageToUpdate.isPublished());
                }
            }

			if (updatePageEntity.getParentId() != null && !updatePageEntity.getParentId().equals(pageToUpdate.getParentId())) {
			    checkUpdatedPageSituation(updatePageEntity, pageType, pageId);
            }

            if(partial) {
                page = merge(updatePageEntity, pageToUpdate);
            } else {
                page = convert(updatePageEntity);
			}

            if (page.getSource() != null) {
				try {
					fetchPage(page);
				} catch (FetcherException e) {
					throw onUpdateFail(pageId, e);
				}
			}

			page.setId(pageId);
			page.setUpdatedAt(new Date());

			// Copy fields from existing values
			page.setCreatedAt(pageToUpdate.getCreatedAt());
			page.setType(pageType);
			page.setReferenceId(pageToUpdate.getReferenceId());
			page.setReferenceType(pageToUpdate.getReferenceType());

			onlyOneHomepage(page);
			// if order change, reorder all pages
			if (page.getOrder() != pageToUpdate.getOrder()) {
				reorderAndSavePages(page);
				return null;
			} else {
				Page updatedPage = pageRepository.update(page);
				createAuditLog(page.getReferenceId(), PAGE_UPDATED, page.getUpdatedAt(), pageToUpdate, page);

				PageEntity pageEntity = convert(updatedPage);

				// update document in search engine
                if(pageToUpdate.isPublished() && !page.isPublished()) {
                	searchEngineService.delete(convert(pageToUpdate), false);
				} else {
					index(pageEntity);
				}

				return pageEntity;
			}
		} catch (TechnicalException ex) {
            throw onUpdateFail(pageId, ex);
		}
	}

    private void checkUpdatedPageSituation(UpdatePageEntity updatePageEntity, io.gravitee.repository.management.model.PageType pageType, String pageId) throws TechnicalException {
        PageSituation newParentSituation = getPageSituation(updatePageEntity.getParentId());
        switch (pageType) {
            case SYSTEM_FOLDER:
                if (newParentSituation != PageSituation.ROOT) {
                    throw new PageActionException(PageType.SYSTEM_FOLDER, " be moved in this folder");
                }
                break;
            case MARKDOWN:
                if (newParentSituation == PageSituation.SYSTEM_FOLDER || newParentSituation == PageSituation.IN_SYSTEM_FOLDER ) {
                    throw new PageActionException(PageType.MARKDOWN, " be moved in a system folder or in a folder of a system folder");
                }
                break;
            case SWAGGER:
                if (newParentSituation == PageSituation.SYSTEM_FOLDER || newParentSituation == PageSituation.IN_SYSTEM_FOLDER ) {
                    throw new PageActionException(PageType.SWAGGER, " be moved in a system folder or in a folder of a system folder");
                }
                break;
            case FOLDER:
                PageSituation folderSituation = getPageSituation(pageId);
                if (folderSituation == PageSituation.IN_SYSTEM_FOLDER && newParentSituation != PageSituation.SYSTEM_FOLDER) {
                    throw new PageActionException(PageType.FOLDER, " be moved anywhere other than in a system folder");
                } else if (folderSituation != PageSituation.IN_SYSTEM_FOLDER && newParentSituation == PageSituation.SYSTEM_FOLDER) {
                    throw new PageActionException(PageType.FOLDER, " be moved in a system folder");
                }
                break;
            case LINK:
                if (newParentSituation != PageSituation.SYSTEM_FOLDER && newParentSituation != PageSituation.IN_SYSTEM_FOLDER ) {
                    throw new PageActionException(PageType.LINK, " be moved anywhere other than in a system folder or in a folder of a system folder");
                }
                break;
            default:
                break;
        }

    }


    private void changeLinksPublicationStatus(String pageId, Boolean published) {
        try {
            this.pageRepository.search(new PageCriteria.Builder().type("LINK").build()).stream()
                    .filter(p -> p.getConfiguration() != null && pageId.equals(p.getConfiguration().get("resourceRef")))
                    .forEach(p -> {
                        try {
                            p.setPublished(published);
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

    private void deleteRelatedLinks(String pageId) {
        try {
            this.pageRepository.search(new PageCriteria.Builder().type("LINK").build()).stream()
                    .filter(p -> p.getConfiguration() != null && pageId.equals(p.getConfiguration().get("resourceRef")))
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

    private void index(PageEntity pageEntity) {
		if (pageEntity.isPublished()) {
			searchEngineService.index(pageEntity, false);
        }

}
	private void fetchPage(final Page page) throws FetcherException {
		Fetcher fetcher = this.getFetcher(page.getSource());
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
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new FetcherException(e.getMessage(), e);
			}
		}
	}

	@SuppressWarnings({"Duplicates", "unchecked"})
	private Fetcher getFetcher(PageSource ps) throws FetcherException {
		if (ps.getConfiguration().isEmpty()) {
			return null;
		}
		try {
			FetcherPlugin fetcherPlugin = fetcherPluginManager.get(ps.getType());
			ClassLoader fetcherCL = fetcherPlugin.fetcher().getClassLoader();
			Fetcher fetcher;
			if (fetcherPlugin.configuration().getName().equals(FilepathAwareFetcherConfiguration.class.getName())) {
				Class<? extends FetcherConfiguration> fetcherConfigurationClass =
						(Class<? extends FetcherConfiguration>) fetcherCL.loadClass(fetcherPlugin.configuration().getName());
				Class<? extends FilesFetcher> fetcherClass =
						(Class<? extends FilesFetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());
				FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(fetcherConfigurationClass, ps.getConfiguration());
				fetcher = fetcherClass.getConstructor(fetcherConfigurationClass).newInstance(fetcherConfigurationInstance);
			} else {
				Class<? extends FetcherConfiguration> fetcherConfigurationClass =
						(Class<? extends FetcherConfiguration>) fetcherCL.loadClass(fetcherPlugin.configuration().getName());
				Class<? extends Fetcher> fetcherClass =
						(Class<? extends Fetcher>) fetcherCL.loadClass(fetcherPlugin.clazz());
				FetcherConfiguration fetcherConfigurationInstance = fetcherConfigurationFactory.create(fetcherConfigurationClass, ps.getConfiguration());
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
	public List<PageEntity> importFiles(ImportPageEntity pageEntity) {
		return importFiles(null, pageEntity);
	}

	@Override
	public List<PageEntity> importFiles(String apiId, ImportPageEntity pageEntity) {
		upsertRootPage(apiId, pageEntity);

		try {
			Page page = convert(pageEntity);
			Fetcher _fetcher = this.getFetcher(page.getSource());
			if (_fetcher == null) {
				return emptyList();
			}

			if (!(_fetcher instanceof FilesFetcher)) {
				throw new UnsupportedOperationException("The plugin does not support to import a directory.");
			}

			FilesFetcher fetcher = (FilesFetcher) _fetcher;

			return importDirectory(apiId, pageEntity, fetcher);

		} catch (FetcherException ex) {
			logger.error("An error occurs while trying to import a directory",ex);
			throw new TechnicalManagementException("An error occurs while trying import a directory" , ex);
		}
	}

	private List<PageEntity> importDescriptor(final String apiId, final ImportPageEntity descriptorPageEntity, final FilesFetcher fetcher, final GraviteeDescriptorEntity descriptorEntity) {
		if (descriptorEntity.getDocumentation() == null || descriptorEntity.getDocumentation().getPages() == null || descriptorEntity.getDocumentation().getPages().isEmpty()) {
			return emptyList();
		}

		Map<String, String> parentsIdByPath = new HashMap<>();
		List<PageEntity> createdPages = new ArrayList<>();
		int order = 0;
		for (GraviteeDescriptorPageEntity descriptorPage : descriptorEntity.getDocumentation().getPages()) {
			NewPageEntity newPage = getPageFromPath(descriptorPage.getSrc());
			if (newPage == null) {
				logger.warn("Unable to find a source file to import. Please fix the descriptor content.");
			} else {
				if (descriptorPage.getName() != null && !descriptorPage.getName().isEmpty()) {
					newPage.setName(descriptorPage.getName());
				}

				newPage.setHomepage(descriptorPage.isHomepage());
				newPage.setLastContributor(descriptorPageEntity.getLastContributor());
				newPage.setPublished(descriptorPageEntity.isPublished());
				newPage.setSource(descriptorPageEntity.getSource());
				newPage.setOrder(order++);

				String parentPath = descriptorPage.getDest() == null || descriptorPage.getDest().isEmpty()
						? getParentPathFromFilePath(descriptorPage.getSrc())
						: descriptorPage.getDest();

				try {
				createdPages.addAll(
						upsertPageAndParentFolders(
								parentPath,
								newPage,
								parentsIdByPath,
								fetcher,
								apiId,
								descriptorPage.getSrc()));
				} catch (TechnicalException ex) {
					logger.error("An error occurs while trying to import a gravitee descriptor", ex);
					throw new TechnicalManagementException("An error occurs while trying to import a gravitee descriptor", ex);
				}
			}
		}
		return createdPages;
	}

	private List<PageEntity> importDirectory(String apiId, ImportPageEntity pageEntity, FilesFetcher fetcher) {
		try {
			String[] files = fetcher.files();

			// if a gravitee descriptor is present, import it.
			Optional<String> optDescriptor = Arrays.stream(files)
					.filter(f -> f.endsWith(graviteeDescriptorService.descriptorName()))
					.findFirst();
			if (optDescriptor.isPresent()) {
				try {
				    fetcher.getConfiguration().setFilepath(optDescriptor.get());
					final Resource resource = fetcher.fetch();
					final GraviteeDescriptorEntity descriptorEntity = graviteeDescriptorService.read(getResourceContentAsString(resource));
					return importDescriptor(apiId, pageEntity, fetcher, descriptorEntity);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new FetcherException(e.getMessage(), e);
				}
			}

			Map<String, String> parentsIdByPath = new HashMap<>();

			List<PageEntity> createdPages = new ArrayList<>();
			// for each files returned by the fetcher
			int order = 0;
			for (String file : files) {
				NewPageEntity pageFromPath = getPageFromPath(file);
				if (pageFromPath != null) {
					pageFromPath.setLastContributor(pageEntity.getLastContributor());
					pageFromPath.setPublished(pageEntity.isPublished());
					pageFromPath.setSource(pageEntity.getSource());
					pageFromPath.setOrder(order++);
					try {
						createdPages.addAll(
								upsertPageAndParentFolders(
										getParentPathFromFilePath(file),
										pageFromPath,
										parentsIdByPath,
										fetcher,
										apiId,
										file));
					} catch (TechnicalException ex) {
						logger.error("An error occurs while trying to import a directory", ex);
						throw new TechnicalManagementException("An error occurs while trying to import a directory", ex);
					}
				}
			}
			return createdPages;
		} catch (FetcherException ex) {
			logger.error("An error occurs while trying to import a directory",ex);
			throw new TechnicalManagementException("An error occurs while trying import a directory" , ex);
		}
	}

	private NewPageEntity getPageFromPath(String path) {
		if (path != null) {
			String[] extensions = path.split("\\.");
			if (extensions.length > 0) {
				PageType supportedPageType = getSupportedPageType(extensions[extensions.length - 1]);
				// if the file is supported by gravitee
				if (supportedPageType != null) {
					String[] pathElements = path.split("/");
					if (pathElements.length > 0) {
						String filename = pathElements[pathElements.length - 1];
						NewPageEntity newPage = new NewPageEntity();
						newPage.setName(filename.substring(0, filename.lastIndexOf(".")));
						newPage.setType(supportedPageType);
						return newPage;
					}
				}
			}
		}
		logger.warn("Unable to extract Page informations from :[" + path + "]");
		return null;
	}

	private String getParentPathFromFilePath(String filePath){
	    if (filePath != null && !filePath.isEmpty()) {
			String[] pathElements = filePath.split("/");
			if (pathElements.length > 0) {
				StringJoiner stringJoiner = new StringJoiner("/");
				for (int i = 0; i < pathElements.length -1; i++) {
					stringJoiner.add(pathElements[i]);
				}
				return stringJoiner.toString();
			}
		}

		return "/";
	}

	private List<PageEntity> upsertPageAndParentFolders(
			final String parentPath,
			final NewPageEntity newPageEntity,
			final Map<String, String> parentsIdByPath,
			final FilesFetcher fetcher,
			final String apiId,
			final String src) throws TechnicalException {

		ObjectMapper mapper = new ObjectMapper();
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
									.build());
					PageEntity folder;
					if(pages.isEmpty()) {
						NewPageEntity newPage = new NewPageEntity();
						newPage.setParentId(parentId);
						newPage.setPublished(newPageEntity.isPublished());
						newPage.setLastContributor(newPageEntity.getLastContributor());
						newPage.setName(pathElement);
						newPage.setType(PageType.FOLDER);
						folder = this.createPage(apiId, newPage);
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
						.build());
		if (pages.isEmpty()) {
			newPageEntity.setParentId(parentId);
			FilepathAwareFetcherConfiguration configuration = fetcher.getConfiguration();
			configuration.setFilepath(src);
			newPageEntity.getSource().setConfiguration(mapper.valueToTree(configuration));
			createdPages.add(this.createPage(apiId, newPageEntity));
		} else {
			Page page = pages.get(0);
			UpdatePageEntity updatePage = convertToUpdateEntity(page);
			updatePage.setLastContributor(newPageEntity.getLastContributor());
			updatePage.setPublished(newPageEntity.isPublished());
			updatePage.setOrder(newPageEntity.getOrder());
			updatePage.setHomepage(newPageEntity.isHomepage());
			FilepathAwareFetcherConfiguration configuration = fetcher.getConfiguration();
			configuration.setFilepath(src);
			updatePage.setSource(newPageEntity.getSource());
			updatePage.getSource().setConfiguration(mapper.valueToTree(configuration));
			createdPages.add(this.update(page.getId(), updatePage, false));
		}
		return createdPages;
	}

	private void upsertRootPage(String apiId, ImportPageEntity rootPage) {
		try {
			// root page exists ?
			List<Page> searchResult = pageRepository.search(new PageCriteria.Builder()
			        .referenceId(apiId)
                    .referenceType(PageReferenceType.API.name())
					.type(PageType.ROOT.name())
					.build());

			Page page = convert(rootPage);
			page.setReferenceId(apiId);
			if (searchResult.isEmpty()) {
				page.setId(UUID.toString(UUID.random()));
				pageRepository.create(page);
			} else {
				page.setId(searchResult.get(0).getId());
				pageRepository.update(page);
			}
		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to save the configuration",ex);
			throw new TechnicalManagementException("An error occurs while trying to save the configuration" , ex);
		}
	}

	private PageType getSupportedPageType(String extension) {
		for (PageType pageType: PageType.values()) {
			if (pageType.extensions().contains(extension.toLowerCase())) {
				return pageType;
			}
		}
		return null;
	}

	private void reorderAndSavePages(final Page pageToReorder) throws TechnicalException {
		PageCriteria.Builder q = new PageCriteria.Builder().referenceId(pageToReorder.getReferenceId()).referenceType(pageToReorder.getReferenceType().name());
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
							.referenceId(page.getReferenceId())
                            .referenceType(PageReferenceType.API.name())
							.parent(page.getId()).build()).size() > 0 ) {
				throw new TechnicalManagementException("Unable to remove the folder. It must be empty before being removed.");
			}

			pageRepository.delete(pageId);
			// delete links related to the page
			if (!io.gravitee.repository.management.model.PageType.LINK.equals(page.getType())) {
			    this.deleteRelatedLinks(pageId);
			}

            createAuditLog(page.getReferenceId(), PAGE_DELETED, new Date(), page, null);

            // remove from search engine
			searchEngineService.delete(convert(page), false);
		} catch (TechnicalException ex) {
			logger.error("An error occurs while trying to delete Page {}", pageId, ex);
			throw new TechnicalManagementException("An error occurs while trying to delete Page " + pageId, ex);
		}
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
	public int findMaxPortalPageOrder() {
		try {
			logger.debug("Find Max Order Portal Page");
			final Integer maxPageOrder = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder(GraviteeContext.getCurrentEnvironment(), PageReferenceType.ENVIRONMENT);
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
				fetchPage(page);
			} catch (FetcherException e) {
				throw onUpdateFail(pageId, e);
			}

			page.setUpdatedAt(new Date());
			page.setLastContributor(contributor);

			Page updatedPage = pageRepository.update(page);
			createAuditLog(page.getReferenceId(), PAGE_UPDATED, page.getUpdatedAt(), page, page);
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

	private NewPageEntity convert(final PageEntity pageEntity) {
		final NewPageEntity newPageEntity = new NewPageEntity();
		newPageEntity.setName(pageEntity.getName());
		newPageEntity.setOrder(pageEntity.getOrder());
		newPageEntity.setPublished(pageEntity.isPublished());
		newPageEntity.setSource(pageEntity.getSource());
		newPageEntity.setType(PageType.valueOf(pageEntity.getType()));
		newPageEntity.setParentId(pageEntity.getParentId());
		newPageEntity.setHomepage(pageEntity.isHomepage());
		newPageEntity.setContent(pageEntity.getContent());
		newPageEntity.setConfiguration(pageEntity.getConfiguration());
		newPageEntity.setExcludedGroups(pageEntity.getExcludedGroups());
		newPageEntity.setLastContributor(pageEntity.getLastContributor());
		return newPageEntity;
	}

	private static Page convert(ImportPageEntity importPageEntity) {
		Page page = new Page();

		final PageType type = importPageEntity.getType();
		if (type != null) {
			page.setType(io.gravitee.repository.management.model.PageType.valueOf(type.name()));
		}
		page.setLastContributor(importPageEntity.getLastContributor());
		page.setPublished(importPageEntity.isPublished());
		page.setSource(convert(importPageEntity.getSource()));
		page.setConfiguration(importPageEntity.getConfiguration());
		page.setExcludedGroups(importPageEntity.getExcludedGroups());

		return page;
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
		pageEntity.setMetadata(page.getMetadata());
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

	private static UpdatePageEntity convertToUpdateEntity(Page page) {
		UpdatePageEntity updatePageEntity = new UpdatePageEntity();

		updatePageEntity.setName(page.getName());
		updatePageEntity.setContent(page.getContent());
		updatePageEntity.setLastContributor(page.getLastContributor());
		updatePageEntity.setOrder(page.getOrder());
		updatePageEntity.setPublished(page.isPublished());
		updatePageEntity.setSource(convert(page.getSource()));
		updatePageEntity.setConfiguration(page.getConfiguration());
		updatePageEntity.setHomepage(page.isHomepage());
		updatePageEntity.setExcludedGroups(page.getExcludedGroups());
		updatePageEntity.setParentId("".equals(page.getParentId()) ? null : page.getParentId());
		return updatePageEntity;
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
			if(query.getApi() != null) {
    			builder.referenceId(query.getApi());
    			builder.referenceType(PageReferenceType.API.name());
			} else {
			    builder.referenceId(GraviteeContext.getCurrentEnvironment());
                builder.referenceType(PageReferenceType.ENVIRONMENT.name());
			}
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
