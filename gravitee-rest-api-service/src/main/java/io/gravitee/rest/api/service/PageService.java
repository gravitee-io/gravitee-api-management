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
package io.gravitee.rest.api.service;

import java.util.List;
import java.util.Map;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.documentation.PageQuery;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Guillaume GILLON
 * @author GraviteeSource Team
 */
public interface PageService {

	Page<PageEntity> findAll(Pageable pageable);

	PageEntity findById(String pageId);

	PageEntity findById(String pageId, String acceptedLocale);

	List<PageEntity> search(PageQuery query);

    List<PageEntity> search(PageQuery query, boolean withTranslations);

	List<PageEntity> search(PageQuery query, String acceptedLocale);

	void transformSwagger(PageEntity pageEntity);

	void transformSwagger(PageEntity pageEntity, String apiId);

	PageEntity createPage(String apiId, NewPageEntity page);

	PageEntity createPage(NewPageEntity page);
	
	PageEntity update(String pageId, UpdatePageEntity updatePageEntity);

	PageEntity update(String pageId, UpdatePageEntity updatePageEntity, boolean partial);

	void delete(String pageId);
	
	int findMaxApiPageOrderByApi(String apiId);

	int findMaxPortalPageOrder();

	boolean isDisplayable(ApiEntity api, boolean isPagePublished, String username);

	void fetchAll(PageQuery query, String contributor);
	
	PageEntity fetch(String pageId, String contributor);

	List<PageEntity> importFiles(ImportPageEntity pageEntity);

	List<PageEntity> importFiles(String apiId, ImportPageEntity pageEntity);

	void transformWithTemplate(PageEntity pageEntity, String api);

	PageEntity create(String apiId, PageEntity pageEntity);

	Map<SystemFolderType, String> initialize(String environmentId);

	PageEntity createSystemFolder(String apiId, SystemFolderType systemFolderType, int order, String environmentId);

	/**
	 * Check if the page is used as GeneralCondition by an active Plan for the given ApiID
	 *
	 * @param page
	 * @param apiId
	 * @return
	 */
	boolean isPageUsedAsGeneralConditions(PageEntity page, String apiId);

	boolean shouldHaveRevision(String pageType);
}
