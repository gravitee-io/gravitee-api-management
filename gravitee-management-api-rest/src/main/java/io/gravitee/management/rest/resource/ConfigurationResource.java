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

import io.swagger.annotations.Api;

import javax.ws.rs.Path;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/configuration")
@Api(tags = {"Configuration"})
public class ConfigurationResource {

    @Context
    private ResourceContext resourceContext;

    @Path("views")
    public ViewsResource getViewResource() {
        return resourceContext.getResource(ViewsResource.class);
    }

    @Path("groups")
    public GroupsResource getGroupResource() {
        return resourceContext.getResource(GroupsResource.class);
    }

    @Path("tags")
    public TagsResource getTagResource() {
        return resourceContext.getResource(TagsResource.class);
    }
}
