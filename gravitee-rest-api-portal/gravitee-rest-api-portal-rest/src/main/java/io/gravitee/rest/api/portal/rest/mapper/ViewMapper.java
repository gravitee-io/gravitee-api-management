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

import javax.ws.rs.core.UriBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.portal.rest.model.View;
import io.gravitee.rest.api.portal.rest.model.ViewLinks;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ViewMapper {
    @Autowired
    UserMapper userMapper;
    
    public View convert(ViewEntity viewEntity, UriBuilder baseUriBuilder) {
        final View view = new View();

        view.setDefaultView(viewEntity.isDefaultView());
        view.setDescription(viewEntity.getDescription());
        view.setId(viewEntity.getId());
        view.setName(viewEntity.getName());
        view.setOrder(viewEntity.getOrder());
        view.setTotalApis(viewEntity.getTotalApis());
        
        ViewLinks viewLinks = new ViewLinks();
        String basePath = PortalApiLinkHelper.viewsURL(baseUriBuilder.clone(), viewEntity.getId());
        String highlightApi = viewEntity.getHighlightApi();
        if(highlightApi != null) {
            viewLinks.setHighlightedApi(PortalApiLinkHelper.apisURL(baseUriBuilder.clone(), highlightApi));
        }
        viewLinks.setPicture(basePath+"/picture");
        viewLinks.setSelf(basePath);
        view.setLinks(viewLinks);
        return view;
    }

}
