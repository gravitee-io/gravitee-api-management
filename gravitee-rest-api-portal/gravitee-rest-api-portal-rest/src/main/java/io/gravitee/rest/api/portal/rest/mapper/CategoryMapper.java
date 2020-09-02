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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.portal.rest.model.Category;
import io.gravitee.rest.api.portal.rest.model.CategoryLinks;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CategoryMapper {
    @Autowired
    UserMapper userMapper;
    
    public Category convert(CategoryEntity categoryEntity, UriBuilder baseUriBuilder) {
        final Category category = new Category();

        category.setDescription(categoryEntity.getDescription());
        category.setId(categoryEntity.getKey());
        category.setName(categoryEntity.getName());
        category.setOrder(categoryEntity.getOrder());
        category.setPage(categoryEntity.getPage());
        category.setTotalApis(categoryEntity.getTotalApis());
        
        CategoryLinks categoryLinks = new CategoryLinks();
        String basePath = PortalApiLinkHelper.categoriesURL(baseUriBuilder.clone(), categoryEntity.getId());
        String highlightApi = categoryEntity.getHighlightApi();
        if(highlightApi != null) {
            categoryLinks.setHighlightedApi(PortalApiLinkHelper.apisURL(baseUriBuilder.clone(), highlightApi));
        }
        final String hash = categoryEntity.getUpdatedAt() == null ? "" : String.valueOf(categoryEntity.getUpdatedAt().getTime());
        categoryLinks.setPicture(basePath+"/picture?" + hash);
        categoryLinks.setBackground(basePath+"/background?" + hash);
        categoryLinks.setSelf(basePath);
        category.setLinks(categoryLinks);
        return category;
    }

}
