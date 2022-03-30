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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/groups";
    }

    @Test
    public void shouldCreate() {
        reset(groupService);

        NewGroupEntity newGroupEntity = new NewGroupEntity();
        newGroupEntity.setName("my-group-name");

        GroupEntity createdGroup = new GroupEntity();
        createdGroup.setId("my-group-id");
        doReturn(createdGroup).when(groupService).create(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = envTarget().request().post(Entity.json(newGroupEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path("my-group-id").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
