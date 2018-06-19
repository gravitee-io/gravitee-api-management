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
package io.gravitee.management.service;

import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.documentation.PageQuery;

import java.util.List;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume GILLON 
 */
public interface PageService {

	List<PageListItem> findApiPagesByApi(String apiId);

	List<PageListItem> findApiPagesByApiAndHomepage(String apiId, Boolean homepage, Boolean flatMode);

	List<PageListItem> findPortalPagesByHomepage(Boolean homepage, Boolean flatMode);

	PageEntity findById(String pageId);

	List<PageEntity> search(PageQuery query);

	PageEntity findById(String pageId, boolean transform);

	PageEntity createApiPage(String apiId, NewPageEntity page);

	PageEntity createPortalPage(NewPageEntity page);
	
	PageEntity update(String pageId, UpdatePageEntity updatePageEntity);
	
	void delete(String pageId);
	
	int findMaxApiPageOrderByApi(String apiId);

	int findMaxPortalPageOrder();

	boolean isDisplayable(ApiEntity api, boolean isPagePublished, String username);
}
