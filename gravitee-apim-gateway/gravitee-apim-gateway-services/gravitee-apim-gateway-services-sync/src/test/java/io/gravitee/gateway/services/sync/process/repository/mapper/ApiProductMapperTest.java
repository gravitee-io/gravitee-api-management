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

import static io.gravitee.repository.management.model.Event.EventProperties.API_PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Organization;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    private ApiProductMapper cut;

    @BeforeEach
    void setUp() {
        cut = new ApiProductMapper(objectMapper, new EnvironmentService(environmentRepository, organizationRepository));
    }

    @Nested
    class ToIdTest {

        @Test
        void should_extract_api_product_id_from_event_properties() {
            Event event = new Event();
            event.setId("event-id");
            event.setProperties(Map.of(API_PRODUCT_ID.getValue(), "api-product-123"));

            cut
                .toId(event)
                .test()
                .assertValue(id -> {
                    assertThat(id).isEqualTo("api-product-123");
                    return true;
                })
                .assertComplete();
        }

        @Test
        void should_return_empty_when_no_properties() {
            Event event = new Event();
            event.setId("event-id");
            event.setProperties(null);

            cut.toId(event).test().assertComplete().assertNoValues();
        }

        @Test
        void should_return_empty_when_api_product_id_missing() {
            Event event = new Event();
            event.setId("event-id");
            event.setProperties(Map.of("other-property", "value"));

            cut.toId(event).test().assertComplete().assertNoValues();
        }
    }

    @Nested
    class ToTest {

        @SneakyThrows
        @Test
        void should_map_api_product_with_all_fields() {
            Organization organization = new Organization();
            organization.setId("org-id");
            organization.setHrids(List.of("org-hrid"));
            when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));

            Environment environment = new Environment();
            environment.setId("env-id");
            environment.setHrids(List.of("env-hrid"));
            environment.setOrganizationId(organization.getId());
            when(environmentRepository.findById("env-id")).thenReturn(Optional.of(environment));

            Event event = new Event();
            final Date createdDate = new Date();
            event.setCreatedAt(createdDate);

            // Create API Product payload matching MAPI structure
            String payload = objectMapper.writeValueAsString(
                Map.of(
                    "id",
                    "api-product-123",
                    "name",
                    "Test API Product",
                    "description",
                    "Test Description",
                    "version",
                    "1.0",
                    "apiIds",
                    Set.of("api-1", "api-2", "api-3"),
                    "environmentId",
                    "env-id",
                    "environmentHrid",
                    "env-hrid",
                    "organizationId",
                    "org-id",
                    "organizationHrid",
                    "org-hrid"
                )
            );
            event.setPayload(payload);

            cut
                .to(event)
                .test()
                .assertValue(reactableApiProduct -> {
                    assertThat(reactableApiProduct.getId()).isEqualTo("api-product-123");
                    assertThat(reactableApiProduct.getName()).isEqualTo("Test API Product");
                    assertThat(reactableApiProduct.getDescription()).isEqualTo("Test Description");
                    assertThat(reactableApiProduct.getVersion()).isEqualTo("1.0");
                    assertThat(reactableApiProduct.getApiIds()).containsExactlyInAnyOrder("api-1", "api-2", "api-3");
                    assertThat(reactableApiProduct.getEnvironmentId()).isEqualTo("env-id");
                    assertThat(reactableApiProduct.getEnvironmentHrid()).isEqualTo("env-hrid");
                    assertThat(reactableApiProduct.getOrganizationId()).isEqualTo("org-id");
                    assertThat(reactableApiProduct.getOrganizationHrid()).isEqualTo("org-hrid");
                    assertThat(reactableApiProduct.getDeployedAt()).isEqualTo(createdDate);
                    assertThat(reactableApiProduct.enabled()).isTrue();
                    assertThat(reactableApiProduct.dependencies(String.class)).isNotNull().isEmpty();
                    return true;
                })
                .assertComplete();
        }

        @SneakyThrows
        @Test
        void should_map_api_product_with_minimal_fields() {
            Organization organization = new Organization();
            organization.setId("org-id");
            organization.setHrids(List.of("org-hrid"));
            when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));

            Environment environment = new Environment();
            environment.setId("env-id");
            environment.setHrids(List.of("env-hrid"));
            environment.setOrganizationId(organization.getId());
            when(environmentRepository.findById("env-id")).thenReturn(Optional.of(environment));

            Event event = new Event();
            event.setCreatedAt(new Date());

            String payload = objectMapper.writeValueAsString(
                Map.of(
                    "id",
                    "api-product-456",
                    "name",
                    "Minimal Product",
                    "version",
                    "2.0",
                    "apiIds",
                    Set.of("api-1"),
                    "environmentId",
                    "env-id"
                )
            );
            event.setPayload(payload);

            cut
                .to(event)
                .test()
                .assertValue(reactableApiProduct -> {
                    assertThat(reactableApiProduct.getId()).isEqualTo("api-product-456");
                    assertThat(reactableApiProduct.getName()).isEqualTo("Minimal Product");
                    assertThat(reactableApiProduct.getDescription()).isNull();
                    assertThat(reactableApiProduct.getVersion()).isEqualTo("2.0");
                    assertThat(reactableApiProduct.getApiIds()).containsExactly("api-1");
                    assertThat(reactableApiProduct.getEnvironmentId()).isEqualTo("env-id");
                    return true;
                })
                .assertComplete();
        }

        @SneakyThrows
        @Test
        void should_return_empty_when_payload_is_invalid_json() {
            Event event = new Event();
            event.setId("event-id");
            event.setPayload("invalid-json");

            cut.to(event).test().assertComplete().assertNoValues();
        }

        @SneakyThrows
        @Test
        void should_return_empty_when_payload_is_null() {
            Event event = new Event();
            event.setId("event-id");
            event.setPayload(null);

            cut.to(event).test().assertComplete().assertNoValues();
        }

        @SneakyThrows
        @Test
        void should_handle_empty_api_ids() {
            Organization organization = new Organization();
            organization.setId("org-id");
            organization.setHrids(List.of("org-hrid"));
            when(organizationRepository.findById(organization.getId())).thenReturn(Optional.of(organization));

            Environment environment = new Environment();
            environment.setId("env-id");
            environment.setHrids(List.of("env-hrid"));
            environment.setOrganizationId(organization.getId());
            when(environmentRepository.findById("env-id")).thenReturn(Optional.of(environment));

            Event event = new Event();
            event.setCreatedAt(new Date());

            String payload = objectMapper.writeValueAsString(
                Map.of("id", "api-product-empty", "name", "Empty Product", "version", "1.0", "apiIds", Set.of(), "environmentId", "env-id")
            );
            event.setPayload(payload);

            cut
                .to(event)
                .test()
                .assertValue(reactableApiProduct -> {
                    assertThat(reactableApiProduct.getId()).isEqualTo("api-product-empty");
                    assertThat(reactableApiProduct.getApiIds()).isEmpty();
                    return true;
                })
                .assertComplete();
        }
    }
}
