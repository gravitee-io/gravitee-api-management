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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageConfiguration;
import io.gravitee.repository.management.model.PageSource;
import io.gravitee.repository.mongodb.management.internal.model.PageConfigurationMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageSourceMongo;
import io.gravitee.repository.mongodb.management.internal.page.PageMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoPageRepository implements PageRepository {

	private final static Logger logger = LoggerFactory.getLogger(MongoPageRepository.class);

	@Autowired
	private PageMongoRepository internalPageRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public Collection<Page> findByApi(String apiId) throws TechnicalException {
		logger.debug("Find pages by api {}", apiId);

		List<PageMongo> pages = internalPageRepo.findByApi(apiId);
		Set<Page> res = mapper.collection2set(pages, PageMongo.class, Page.class);

		logger.debug("Find pages by api {} - Done", apiId);
		return res;
	}

	@Override
	public Optional<Page> findById(String pageId) throws TechnicalException {
		logger.debug("Find page by ID [{}]", pageId);

		PageMongo page = internalPageRepo.findOne(pageId);
		Page res = mapper.map(page, Page.class);

		logger.debug("Find page by ID [{}] - Done", pageId);
		return Optional.ofNullable(res);
	}

	@Override
	public Page create(Page page) throws TechnicalException {
		logger.debug("Create page [{}]", page.getName());
		
		PageMongo pageMongo = mapper.map(page, PageMongo.class);
		PageMongo createdPageMongo = internalPageRepo.insert(pageMongo);
		
		Page res = mapper.map(createdPageMongo, Page.class);
		
		logger.debug("Create page [{}] - Done", page.getName());
		
		return res;
	}

	@Override
	public Page update(Page page) throws TechnicalException {
		if(page == null || page.getName() == null){
			throw new IllegalStateException("Page to update must have a name");
		}
		
		// Search team by name
		PageMongo pageMongo = internalPageRepo.findOne(page.getId());
		
		if(pageMongo == null){
			throw new IllegalStateException(String.format("No page found with name [%s]", page.getId()));
		}
		
		try{
			//Update
			pageMongo.setName(page.getName());
			pageMongo.setContent(page.getContent());
			pageMongo.setLastContributor(page.getLastContributor());
			pageMongo.setUpdatedAt(page.getUpdatedAt());
			pageMongo.setOrder(page.getOrder());
			pageMongo.setPublished(page.isPublished());
			if(page.getSource() != null) {
				pageMongo.setSource(convert(page.getSource()));
			} else {
				pageMongo.setSource(null);
			}
			if(page.getConfiguration() != null) {
				pageMongo.setConfiguration(convert(page.getConfiguration()));
			} else {
				pageMongo.setConfiguration(null);
			}
			
			PageMongo pageMongoUpdated = internalPageRepo.save(pageMongo);
			return mapper.map(pageMongoUpdated, Page.class);

		}catch(Exception e){

			logger.error("An error occured when updating page",e);
			throw new TechnicalException("An error occured when updating page");
		}
	}

	@Override
	public void delete(String pageId) throws TechnicalException {
		try{
			internalPageRepo.delete(pageId);
		}catch(Exception e){
			logger.error("An error occured when deleting page [{}]", pageId, e);
			throw new TechnicalException("An error occured when deleting page");
		}
	}

	@Override
	public Integer findMaxPageOrderByApi(String apiId) throws TechnicalException {
		try{
			return internalPageRepo.findMaxPageOrderByApi(apiId);
		}catch(Exception e){
			logger.error("An error occured when searching max order page for api name [{}]", apiId, e);
			throw new TechnicalException("An error occured when searching max order page for api name");
		}
	}

	private PageSourceMongo convert(PageSource pageSource) {
		PageSourceMongo pageSourceMongo = new PageSourceMongo();
		pageSourceMongo.setType(pageSource.getType());
		pageSourceMongo.setConfiguration(pageSource.getConfiguration());
		return pageSourceMongo;
	}

	private PageConfigurationMongo convert(PageConfiguration pageConfiguration) {
		PageConfigurationMongo pageConfigurationMongo = new PageConfigurationMongo();
		pageConfigurationMongo.setTryIt(pageConfiguration.isTryIt());
		pageConfigurationMongo.setTryItURL(pageConfiguration.getTryItURL());
        return pageConfigurationMongo;
	}
}
