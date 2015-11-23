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

import static java.util.Collections.emptyList;
import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.PageService;
import io.gravitee.management.service.IdGenerator;
import io.gravitee.management.service.exceptions.PageAlreadyExistsException;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class PageServiceImpl extends TransactionalService implements PageService {

	private final Logger LOGGER = LoggerFactory.getLogger(PageServiceImpl.class);

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private IdGenerator idGenerator;

	@Override
	public List<PageEntity> findByApi(String apiName) {
		try {
			final Collection<Page> pages = pageRepository.findByApi(apiName);

			if (pages == null || pages.isEmpty()) {
				return emptyList();
			}

			final List<PageEntity> pageEntities = new ArrayList<>(pages.size());

			pageEntities.addAll(pages.stream()
					.map(PageServiceImpl::convert)
					.sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
					.collect(Collectors.toSet())
			);

			return pageEntities;
		} catch (TechnicalException ex) {
			LOGGER.error("An error occurs while trying to find an PAGES using its api name {}", apiName, ex);
			throw new TechnicalManagementException(
					"An error occurs while trying to find an PAGES using its api name " + apiName, ex);
		}
	}

	@Override
	public Optional<PageEntity> findById(String pageName) {
		try {
			LOGGER.debug("Find PAGE by name: {}", pageName);
			return pageRepository.findById(pageName).map(PageServiceImpl::convert);
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
			Optional<PageEntity> checkPage = findById(newPageEntity.getName());
			if (checkPage.isPresent()) {
				throw new PageAlreadyExistsException(newPageEntity.getName());
			}

			Page page = convert(newPageEntity);

			page.setId(idGenerator.generate(page.getName()));

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

			Optional<Page> optPageToUpdate = pageRepository.findById(pageName);
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
			page.setApi(pageToUpdate.getApi());
			page.setOrder(pageToUpdate.getOrder());

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
	public int findMaxPageOrderByApi(String apiName) {
		try {
			LOGGER.debug("Find Max Order Page for api name : {}", apiName);
			final Integer maxPageOrder = pageRepository.findMaxPageOrderByApi(apiName);
			return maxPageOrder == null ? 0 : maxPageOrder;
		} catch (TechnicalException ex) {
			LOGGER.error("An error occured when searching max order page for api name [{}]", apiName, ex);
			throw new TechnicalManagementException("An error occured when searching max order page for api name " + apiName, ex);
		}
	}

	private static Page convert(NewPageEntity newPageEntity) {
		Page page = new Page();

		page.setName(newPageEntity.getName());
		final String type = newPageEntity.getType();
		if (type != null) {
			page.setType(type);
		}
		page.setTitle(newPageEntity.getTitle());
		page.setContent(newPageEntity.getContent());
		page.setLastContributor(newPageEntity.getLastContributor());
		page.setOrder(newPageEntity.getOrder());
		page.setApi(newPageEntity.getApiName());
		return page;
	}

	private static PageEntity convert(Page page) {
		PageEntity pageEntity = new PageEntity();

		pageEntity.setId(page.getId());
		pageEntity.setName(page.getName());
		if (page.getType() != null) {
			pageEntity.setType(page.getType().toString());
		}
		pageEntity.setTitle(page.getTitle());
		pageEntity.setContent(page.getContent());
		pageEntity.setLastContributor(page.getLastContributor());
		pageEntity.setOrder(page.getOrder());
		pageEntity.setApi(page.getApi());
		return pageEntity;
	}

	private static Page convert(UpdatePageEntity updatePageEntity) {
		Page page = new Page();

		page.setTitle(updatePageEntity.getTitle());
		page.setContent(updatePageEntity.getContent());
		page.setLastContributor(updatePageEntity.getLastContributor());

		return page;
	}

	public IdGenerator getIdGenerator() {
		return idGenerator;
	}

	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}
}
