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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.model.Info;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InfoResource extends AbstractResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() throws IOException {
        Info info = new Info();

        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("api.properties"));

        info.setName(properties.getProperty("api.name"));
        info.setVersion(properties.getProperty("api.version"));

        return Response.ok(info).build();
    }

}
