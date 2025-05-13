/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;

import io.gravitee.rest.api.portal.rest.model.Info;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
class InfoResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "info";
    }

    @Test
    void shouldGetInfo() {
        final Response response = target().request().get();
        assertEquals(OK_200, response.getStatus());

        Info info = response.readEntity(Info.class);
        assertEquals("API_NAME", info.getName());
        assertEquals("API_VERSION", info.getVersion());
    }
}
