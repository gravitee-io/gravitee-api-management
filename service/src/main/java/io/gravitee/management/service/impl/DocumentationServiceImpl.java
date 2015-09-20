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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.DocumentationService;
import io.gravitee.management.service.exceptions.PageAlreadyExistsException;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.api.PageRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Page;
import io.gravitee.repository.model.PageType;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class DocumentationServiceImpl extends TransactionalService implements DocumentationService {

	private final Logger LOGGER = LoggerFactory.getLogger(DocumentationServiceImpl.class);

	@Autowired
	private PageRepository pageRepository;

	@Override
	public List<PageEntity> findByApiName(String apiName) {
		try {
			Set<Page> pages = pageRepository.findByApiName(apiName);
			List<PageEntity> pageEntities = new ArrayList<PageEntity>(pages.size());

			for (Page page : pages) {
				pageEntities.add(convert(page));
			}
			Collections.sort(pageEntities, PageEntity.PageOrderComparator);
			Collections.reverse(pageEntities);
			return pageEntities;
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to find an PAGES using its api name {}", apiName, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to find an PAGES using its api name " + apiName, ex);
		}
	}

	@Override
	public Optional<PageEntity> findByName(String pageName) {
		try {
			LOGGER.debug("Find PAGE by name: {}", pageName);
			return pageRepository.findByName(pageName).map(DocumentationServiceImpl::convert);
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to find an PAGE using its name {}", pageName, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to find an PAGE using its name " + pageName, ex);
		}
	}

	@Override
	public PageEntity createPage(NewPageEntity newPageEntity) {
		try {
			LOGGER.debug("Create {}", newPageEntity);
			Optional<PageEntity> checkPage = findByName(newPageEntity.getName());
			if (checkPage.isPresent()) {
				throw new PageAlreadyExistsException(newPageEntity.getName());
			}

			Page page = convert(newPageEntity);

			// Set date fields
			page.setCreatedAt(new Date());
			page.setUpdatedAt(page.getCreatedAt());

			Page createdPage = pageRepository.create(page);
			return convert(createdPage);
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to create {}", newPageEntity, ex);
			throw new TechnicalManagementException("An error occurs while trying create " + newPageEntity, ex);
		}
	}

	@Override
	public PageEntity updatePage(String pageName, UpdatePageEntity updatePageEntity) {
		try {
			LOGGER.debug("Update Page {}", pageName);

			Optional<Page> optPageToUpdate = pageRepository.findByName(pageName);
			if (!optPageToUpdate.isPresent()) {
				throw new PageNotFoundException(pageName);
			}

			Page pageToUpdate = optPageToUpdate.get();
			Page page = convert(updatePageEntity);

			page.setName(pageName);
			page.setUpdatedAt(new Date());

			// Copy fields from existing values
			page.setCreatedAt(pageToUpdate.getCreatedAt());
			page.setType(pageToUpdate.getType());
			page.setApiName(pageToUpdate.getApiName());

			Page updatedPage = pageRepository.update(page);
			return convert(updatedPage);

		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to update page {}", pageName, ex);
			throw new TechnicalManagementException("An error occurs while trying to update page " + pageName, ex);
		}
	}

	@Override
	public void deletePage(String pageName) {
		try {
			LOGGER.debug("Delete PAGE : {}", pageName);
			pageRepository.delete(pageName);
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to delete PAGE {}", pageName, ex);
			throw new TechnicalManagementException("An error occurs while trying to delete PAGE " + pageName, ex);
		}
	}
	
	@Override
	public int findMaxPageOrderByApiName(String apiName) {
		try {
			LOGGER.debug("Find Max Order Page for api name : {}", apiName);
			return pageRepository.findMaxPageOrderByApiName(apiName);
		} catch (TechnicalException ex) {
			LOGGER.error("An error occured when searching max order page for api name [{}]", apiName, ex);
			throw new TechnicalManagementException("An error occured when searching max order page for api name " + apiName, ex);
		}
	}

	private static Page convert(NewPageEntity newPageEntity) {
		Page page = new Page();

		page.setName(newPageEntity.getName());
		page.setType(PageType.valueOf(newPageEntity.getType()));
		page.setTitle(newPageEntity.getTitle());
		page.setContent(newPageEntity.getContent());
		page.setLastContributor(newPageEntity.getLastContributor());
		page.setOrder(newPageEntity.getOrder());
		page.setApiName(newPageEntity.getApiName());
		return page;
	}

	private static PageEntity convert(Page page) {
		PageEntity pageEntity = new PageEntity();

		pageEntity.setName(page.getName());
		pageEntity.setType(page.getType().toString());
		pageEntity.setTitle(page.getTitle());
		pageEntity.setContent(page.getContent());
		pageEntity.setLastContributor(page.getLastContributor());
		pageEntity.setOrder(page.getOrder());
		pageEntity.setApiName(page.getApiName());
		return pageEntity;
	}

	private static Page convert(UpdatePageEntity updatePageEntity) {
		Page page = new Page();

		page.setTitle(updatePageEntity.getTitle());
		page.setContent(updatePageEntity.getContent());
		page.setLastContributor(updatePageEntity.getLastContributor());

		return page;
	}
}
