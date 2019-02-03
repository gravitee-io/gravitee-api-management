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
package io.gravitee.management.rest.resource;

import io.gravitee.management.model.ViewEntity;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractViewResource extends AbstractResource {

    @Context
    protected UriInfo uriInfo;

    protected ViewEntity setPicture(ViewEntity viewEntity, boolean fromRoot) {
        final UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        final UriBuilder uriBuilder = ub.path(fromRoot ? viewEntity.getId() + "/picture" : "picture");
        if (viewEntity.getPicture() != null) {
            // force browser to get if updated
            uriBuilder.queryParam("hash", viewEntity.getPicture().hashCode());
        }
        viewEntity.setPictureUrl(uriBuilder.build().toString());
        viewEntity.setPicture(null);

        return viewEntity;
    }

}
