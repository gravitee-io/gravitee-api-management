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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.ApiHeaderService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class EnvironmentServiceTest {

    @InjectMocks
    private EnvironmentServiceImpl cut;

    @Mock
    private EnvironmentRepository mockEnvironmentRepository;

    @Mock
    private OrganizationService mockOrganizationService;

    @Mock
    private ApiHeaderService mockAPIHeaderService;

    @Mock
    private PageService mockPageService;

    @Mock
    private MembershipService mockMembershipService;

    @AfterEach
    public void afterEach() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class FindById {

        @Test
        public void shouldFindByUserById() throws TechnicalException {
            when(mockEnvironmentRepository.findById(any())).thenReturn(Optional.of(getEnvironment("envId", Arrays.asList("env1", "1env"))));

            EnvironmentEntity environment = cut.findById("envId");

            assertNotNull(environment);
        }

        @Test
        public void shouldThrowAnError() throws TechnicalException {
            when(mockEnvironmentRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(EnvironmentNotFoundException.class, () -> cut.findById("envId"));
        }
    }

    @Nested
    class FindByUserAndIdOrHridTest {

        @Test
        public void shouldFindByUserFilteredByEnvId() throws TechnicalException {
            when(mockEnvironmentRepository.findByOrganization(any())).thenReturn(getEnvironments());

            List<EnvironmentEntity> environments = cut.findByUserAndIdOrHrid(GraviteeContext.getCurrentOrganization(), null, "envId");

            assertThat(environments).hasSize(1);
        }

        @Test
        public void shouldFindByUserIdFilteredByEnvId() throws TechnicalException {
            when(mockEnvironmentRepository.findByOrganization(any())).thenReturn(getEnvironments());
            MembershipEntity member = mock(MembershipEntity.class);
            when(member.getReferenceId()).thenReturn("envId");

            when(
                mockMembershipService.getMembershipsByMemberAndReference(
                    MembershipMemberType.USER,
                    "userId",
                    MembershipReferenceType.ENVIRONMENT
                )
            )
                .thenReturn(Set.of(member));

            List<EnvironmentEntity> environments = cut.findByUserAndIdOrHrid(GraviteeContext.getCurrentOrganization(), "userId", "envId");

            assertThat(environments).hasSize(1);
        }

        @Test
        public void shouldFindByUserFilteredByEnvHrid() throws TechnicalException {
            when(mockEnvironmentRepository.findByOrganization(any())).thenReturn(getEnvironments());

            List<EnvironmentEntity> environments = cut.findByUserAndIdOrHrid(GraviteeContext.getCurrentOrganization(), null, "2env");

            assertThat(environments).hasSize(1);
        }

        @Test
        public void shouldFindByUserFilteredByEnvHridAndId_noResult() throws TechnicalException {
            when(mockEnvironmentRepository.findByOrganization(any())).thenReturn(getEnvironments());

            List<EnvironmentEntity> environments = cut.findByUserAndIdOrHrid(GraviteeContext.getCurrentOrganization(), null, "fake-env");

            assertThat(environments).isEmpty();
        }
    }

    @Nested
    class FindByOrgAndIdOrHridTest {

        @Test
        public void shouldFindByOrgAndId() throws TechnicalException {
            Environment env1 = new Environment();
            env1.setId("envId");
            env1.setOrganizationId("org1");
            env1.setHrids(Arrays.asList("env1", "1env"));

            when(mockEnvironmentRepository.findByOrganization(GraviteeContext.getCurrentOrganization())).thenReturn(Set.of(env1));

            EnvironmentEntity environment = cut.findByOrgAndIdOrHrid(GraviteeContext.getCurrentOrganization(), "envId");

            assertThat(environment).isNotNull();
            assertThat(environment.getId()).isEqualTo("envId");
        }

        @Test
        public void shouldFindByOrgAndHrid() throws TechnicalException {
            Environment env1 = new Environment();
            env1.setId("envId");
            env1.setOrganizationId("org1");
            env1.setHrids(Arrays.asList("env1", "1env"));

            when(mockEnvironmentRepository.findByOrganization(eq(GraviteeContext.getCurrentOrganization()))).thenReturn(Set.of(env1));

            EnvironmentEntity environment = cut.findByOrgAndIdOrHrid(GraviteeContext.getCurrentOrganization(), "1env");

            assertThat(environment).isNotNull();
            assertThat(environment.getId()).isEqualTo("envId");
        }

        @Test
        public void shouldNotFind() throws TechnicalException {
            when(mockEnvironmentRepository.findByOrganization(eq(GraviteeContext.getCurrentOrganization())))
                .thenReturn(Collections.emptySet());

            assertThrows(
                EnvironmentNotFoundException.class,
                () -> cut.findByOrgAndIdOrHrid(GraviteeContext.getCurrentOrganization(), "1env")
            );
        }

        @Test
        public void shouldThrowAnError() throws TechnicalException {
            when(mockEnvironmentRepository.findByOrganization(GraviteeContext.getCurrentOrganization())).thenReturn(getEnvironments());

            assertThrows(IllegalStateException.class, () -> cut.findByOrgAndIdOrHrid(GraviteeContext.getCurrentOrganization(), "env1"));
        }
    }

    @Nested
    class CreateTest {

        @Test
        public void shouldCreateEnvironment() throws TechnicalException {
            when(mockOrganizationService.findById(any())).thenReturn(null);
            when(mockEnvironmentRepository.findById(any())).thenReturn(Optional.empty());

            UpdateEnvironmentEntity env1 = new UpdateEnvironmentEntity();
            env1.setHrids(Arrays.asList("envhrid"));
            env1.setName("env_name");
            env1.setDescription("env_desc");

            Environment createdEnv = new Environment();
            createdEnv.setId("env_id");
            when(mockEnvironmentRepository.create(any())).thenReturn(createdEnv);

            EnvironmentEntity environment = cut.createOrUpdate("DEFAULT", "env_id", env1);

            assertNotNull("result is null", environment);
            verify(mockEnvironmentRepository, times(1))
                .create(
                    argThat(arg ->
                        arg != null &&
                        arg.getHrids().equals(Arrays.asList("envhrid")) &&
                        arg.getName().equals("env_name") &&
                        arg.getDescription().equals("env_desc") &&
                        arg.getOrganizationId().equals("DEFAULT")
                    )
                );
            verify(mockEnvironmentRepository, never()).update(any());
            ExecutionContext executionContext = new ExecutionContext("DEFAULT", "env_id");
            verify(mockAPIHeaderService, times(1)).initialize(executionContext);
            verify(mockPageService, times(1)).initialize(executionContext);
        }

        @Test
        public void shouldUpdateEnvironment() throws TechnicalException {
            when(mockOrganizationService.findById(any())).thenReturn(null);
            when(mockEnvironmentRepository.findById(any())).thenReturn(Optional.of(new Environment()));

            UpdateEnvironmentEntity env1 = new UpdateEnvironmentEntity();
            env1.setHrids(Arrays.asList("envhrid"));
            env1.setName("env_name");
            env1.setDescription("env_desc");

            Environment updatedEnv = new Environment();
            when(mockEnvironmentRepository.update(any())).thenReturn(updatedEnv);

            EnvironmentEntity environment = cut.createOrUpdate("DEFAULT", "env_id", env1);

            assertNotNull("result is null", environment);
            verify(mockEnvironmentRepository, times(1))
                .update(
                    argThat(arg ->
                        arg != null &&
                        arg.getHrids().equals(Arrays.asList("envhrid")) &&
                        arg.getName().equals("env_name") &&
                        arg.getDescription().equals("env_desc") &&
                        arg.getOrganizationId().equals("DEFAULT")
                    )
                );
            verify(mockEnvironmentRepository, never()).create(any());
            ExecutionContext executionContext = new ExecutionContext("DEFAULT", "env_id");
            verify(mockAPIHeaderService, never()).initialize(executionContext);
            verify(mockPageService, never()).initialize(executionContext);
        }

        @Test
        public void shouldHaveBadOrganizationExceptionWhenNoOrganizationInEntity() {
            when(mockOrganizationService.findById("UNKNOWN")).thenThrow(new OrganizationNotFoundException("UNKNOWN"));

            assertThrows(OrganizationNotFoundException.class, () -> cut.createOrUpdate("UNKNOWN", "env_id", new UpdateEnvironmentEntity()));
        }
    }

    @Nested
    class GetDefaultOrInitializeTest {

        @Test
        public void shouldReturnDefaultEnvironment() throws TechnicalException {
            Environment existingDefault = new Environment();
            existingDefault.setId(GraviteeContext.getDefaultEnvironment());
            when(mockEnvironmentRepository.findById(eq(GraviteeContext.getDefaultEnvironment()))).thenReturn(Optional.of(existingDefault));

            EnvironmentEntity environment = cut.getDefaultOrInitialize();

            assertThat(environment.getId()).isEqualTo(GraviteeContext.getDefaultEnvironment());
        }

        @Test
        public void shouldCreateDefaultEnvironment() throws TechnicalException {
            when(mockEnvironmentRepository.findById(eq(GraviteeContext.getDefaultEnvironment()))).thenReturn(Optional.empty());

            EnvironmentEntity environment = cut.getDefaultOrInitialize();

            assertThat(environment.getId()).isEqualTo(GraviteeContext.getDefaultEnvironment());
            assertThat(environment.getName()).isEqualTo("Default environment");
            assertThat(environment.getHrids()).isEqualTo(Collections.singletonList("default"));
            assertThat(environment.getDescription()).isEqualTo("Default environment");
            assertThat(environment.getOrganizationId()).isEqualTo(GraviteeContext.getDefaultOrganization());
        }

        @Test
        public void shouldCatchExceptionIfThrow() throws TechnicalException {
            when(mockEnvironmentRepository.findById(eq(GraviteeContext.getDefaultEnvironment()))).thenThrow(new TechnicalException(""));

            assertThrows(TechnicalManagementException.class, () -> cut.getDefaultOrInitialize());
        }
    }

    public Set<Environment> getEnvironments() {
        HashSet<Environment> envSet = new HashSet<>();
        envSet.add(getEnvironment("envId", Arrays.asList("env1", "1env")));
        envSet.add(getEnvironment("env2Id", Arrays.asList("env2", "2env")));
        envSet.add(getEnvironment("env1", Arrays.asList("another_hrid")));

        return envSet;
    }

    @NotNull
    private Environment getEnvironment(String envId, List<String> hrids) {
        Environment env1 = new Environment();
        env1.setId(envId);
        env1.setHrids(hrids);
        return env1;
    }
}
