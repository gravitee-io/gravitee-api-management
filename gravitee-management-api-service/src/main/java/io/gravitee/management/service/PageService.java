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

import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.PageListItem;
import io.gravitee.management.model.UpdatePageEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PageService {

	List<PageListItem> findByApi(String apiId);

	List<PageListItem> findByApiAndHomepage(String apiId, Boolean homepage);

	PageEntity findById(String pageId);

	PageEntity findById(String pageId, boolean transform);
	
	PageEntity create(String apiId, NewPageEntity page);
	
	PageEntity update(String pageId, UpdatePageEntity updatePageEntity);
	
	void delete(String pageId);
	
	int findMaxPageOrderByApi(String apiId);
}
