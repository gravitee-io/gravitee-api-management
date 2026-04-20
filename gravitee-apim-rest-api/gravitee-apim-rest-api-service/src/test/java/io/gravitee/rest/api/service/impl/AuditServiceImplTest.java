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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private UserService userService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Nested
    class AnonymizeData {

        @Test
        void no_path_to_anonymize() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            String data = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "123456"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;
            JsonNode diff = mapper.readTree(data);

            auditService.anonymizeData(diff, List.of("/otherPath"));

            assertThat(diff).isEqualTo(mapper.readTree(data));
        }

        @Test
        void one_path_to_anonymize() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            String data = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "123456"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;
            JsonNode diff = mapper.readTree(data);

            auditService.anonymizeData(diff, List.of("/initialAccessToken"));

            String anonymizedData = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "*****"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;

            assertThat(diff).isEqualTo(mapper.readTree(anonymizedData));
        }

        @Test
        void one_path_to_anonymize_present_without_value_field() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            String data = """
                [
                    {
                        "op": "remove",
                        "path": "/clientId"
                    },
                    {
                        "op": "remove",
                        "path": "/clientSecret"
                    },
                    {
                        "op": "add",
                        "path": "/initialAccessToken",
                        "value": "123456"
                    },
                    {
                        "op": "replace",
                        "path": "/initialAccessTokenType",
                        "value": "INITIAL_ACCESS_TOKEN"
                    }
                ]
                """;
            JsonNode diff = mapper.readTree(data);

            auditService.anonymizeData(diff, List.of("/clientId"));

            assertThat(diff).isEqualTo(mapper.readTree(data));
        }
    }

    @Nested
    class Search {

        private static final String ORG_ID = "org-id";
        private static final String ENV_ID = "env-id";
        private static final String DASHBOARD_ID = "dash-id";
        private static final String DASHBOARD_NAME = "My Dashboard";
        private static final String USER_ID = "user-id";

        private final ExecutionContext executionContext = new ExecutionContext(ORG_ID, ENV_ID);

        @BeforeEach
        void setUp() throws UserNotFoundException {
            when(userService.findById(any(), any())).thenThrow(new UserNotFoundException(USER_ID));
        }

        private Audit dashboardAudit() {
            Audit audit = new Audit();
            audit.setId("audit-id");
            audit.setReferenceType(Audit.AuditReferenceType.DASHBOARD);
            audit.setReferenceId(DASHBOARD_ID);
            audit.setUser(USER_ID);
            audit.setEvent("DASHBOARD_CREATED");
            audit.setPatch("[]");
            return audit;
        }

        @Test
        void should_resolve_dashboard_name_in_metadata() throws TechnicalException {
            Dashboard dashboard = Dashboard.builder().id(DASHBOARD_ID).name(DASHBOARD_NAME).build();
            when(auditRepository.search(any(), any())).thenReturn(new Page<>(List.of(dashboardAudit()), 0, 1, 1));
            when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(Optional.of(dashboard));

            var result = auditSearch();

            assertThat(result.getMetadata()).containsEntry("DASHBOARD:" + DASHBOARD_ID + ":name", DASHBOARD_NAME);
        }

        @Test
        void should_fallback_to_reference_id_when_dashboard_not_found() throws TechnicalException {
            when(auditRepository.search(any(), any())).thenReturn(new Page<>(List.of(dashboardAudit()), 0, 1, 1));
            when(dashboardRepository.findById(DASHBOARD_ID)).thenReturn(Optional.empty());

            var result = auditSearch();

            assertThat(result.getMetadata()).containsEntry("DASHBOARD:" + DASHBOARD_ID + ":name", DASHBOARD_ID);
        }

        @Test
        void should_fallback_to_reference_id_on_technical_exception() throws TechnicalException {
            when(auditRepository.search(any(), any())).thenReturn(new Page<>(List.of(dashboardAudit()), 0, 1, 1));
            when(dashboardRepository.findById(DASHBOARD_ID)).thenThrow(new TechnicalException("db error"));

            var result = auditSearch();

            assertThat(result.getMetadata()).containsEntry("DASHBOARD:" + DASHBOARD_ID + ":name", DASHBOARD_ID);
        }

        private io.gravitee.common.data.domain.MetadataPage<io.gravitee.rest.api.model.audit.AuditEntity> auditSearch() {
            AuditQuery query = new AuditQuery();
            query.setPage(1);
            query.setSize(10);
            return auditService.search(executionContext, query);
        }
    }
}
