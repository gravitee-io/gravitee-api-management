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
package io.gravitee.rest.api.management.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class GroupsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/groups";
    }

    @Test
    public void test() {
        when(groupService.search(eq(GraviteeContext.getExecutionContext()), any(), any()))
            .thenReturn(new Page<>(List.of(GroupEntity.builder().id("gr1").build(), GroupEntity.builder().id("gr2").build()), 1, 2, 12));
        final Response response = envTarget().path("paginated").request().get();
        Map<String, Object> responseBody = response.readEntity(new GenericType<>() {});
        assertAll(
            () -> assertThat(((List<?>) responseBody.get("content")).size()).isEqualTo(2),
            () -> assertThat(responseBody.get("pageNumber")).isEqualTo(1),
            () -> assertThat(responseBody.get("pageElements")).isEqualTo(2),
            () -> assertThat(responseBody.get("totalElements")).isEqualTo(12)
        );
    }
}
