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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Organization;
import io.gravitee.repository.management.model.Event;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReactableOrganizationMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private OrganizationMapper cut;

    @BeforeEach
    public void beforeEach() {
        cut = new OrganizationMapper(objectMapper);
    }

    @Test
    void should_map_organization() throws JsonProcessingException {
        Organization organization = new Organization();
        organization.setId("id");
        organization.setName("name");
        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString(organization));
        event.setCreatedAt(new Date());
        cut
            .to(event)
            .test()
            .assertValue(reactableOrganization -> {
                assertThat(reactableOrganization.getId()).isEqualTo("id");
                assertThat(reactableOrganization.getName()).isEqualTo("name");
                assertThat(reactableOrganization.isEnabled()).isTrue();
                assertThat(reactableOrganization.getDeployedAt()).isNotNull();
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_return_empty_with_mapping_error() throws JsonProcessingException {
        Event event = new Event();
        event.setPayload(objectMapper.writeValueAsString("wrong"));
        cut.to(event).test().assertNoValues().assertComplete();
    }
}
