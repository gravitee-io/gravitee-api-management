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

import static io.gravitee.repository.management.model.Group.AuditEvent.GROUP_CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Calls the audit service through its Spring proxy, as the other services do, to check the audit
 * is attributed to the user of the calling thread.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AuditService_AuthenticatedUserTest.TestConfig.class)
class AuditService_AuthenticatedUserTest {

    private static final String USERNAME = "console-user";

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        reset(auditRepository);
        UserDetails userDetails = new UserDetails(USERNAME, "", Collections.emptyList());
        SecurityContextHolder.setContext(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(userDetails, null)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_record_authenticated_user_when_called_through_proxy() throws Exception {
        auditService.createAuditLog(
            new ExecutionContext("DEFAULT", "DEFAULT"),
            AuditService.AuditLogData.builder()
                .properties(Collections.singletonMap(Audit.AuditProperties.GROUP, "group-id"))
                .event(GROUP_CREATED)
                .createdAt(new Date())
                .oldValue(null)
                .newValue(Collections.singletonMap("name", "group"))
                .build()
        );

        ArgumentCaptor<Audit> audit = ArgumentCaptor.forClass(Audit.class);
        verify(auditRepository, timeout(5000)).create(audit.capture());
        assertThat(audit.getValue().getUser()).isEqualTo(USERNAME);
    }

    @Test
    void should_record_authenticated_user_for_organization_audit() throws Exception {
        auditService.createOrganizationAuditLog(
            new ExecutionContext("DEFAULT", null),
            AuditService.AuditLogData.builder()
                .properties(Collections.singletonMap(Audit.AuditProperties.GROUP, "group-id"))
                .event(GROUP_CREATED)
                .createdAt(new Date())
                .oldValue(null)
                .newValue(Collections.singletonMap("name", "group"))
                .build()
        );

        ArgumentCaptor<Audit> audit = ArgumentCaptor.forClass(Audit.class);
        verify(auditRepository, timeout(5000)).create(audit.capture());
        assertThat(audit.getValue().getUser()).isEqualTo(USERNAME);
    }

    @Configuration
    @EnableAsync
    static class TestConfig {

        @Bean
        AuditService auditService() {
            return new AuditServiceImpl();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new GraviteeMapper();
        }

        @Bean
        AuditRepository auditRepository() {
            return mock(AuditRepository.class);
        }

        @Bean
        PageRepository pageRepository() {
            return mock(PageRepository.class);
        }

        @Bean
        PlanRepository planRepository() {
            return mock(PlanRepository.class);
        }

        @Bean
        MetadataRepository metadataRepository() {
            return mock(MetadataRepository.class);
        }

        @Bean
        GroupRepository groupRepository() {
            return mock(GroupRepository.class);
        }

        @Bean
        ApiRepository apiRepository() {
            return mock(ApiRepository.class);
        }

        @Bean
        EnvironmentRepository environmentRepository() {
            return mock(EnvironmentRepository.class);
        }

        @Bean
        OrganizationRepository organizationRepository() {
            return mock(OrganizationRepository.class);
        }

        @Bean
        ApplicationRepository applicationRepository() {
            return mock(ApplicationRepository.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }
}
