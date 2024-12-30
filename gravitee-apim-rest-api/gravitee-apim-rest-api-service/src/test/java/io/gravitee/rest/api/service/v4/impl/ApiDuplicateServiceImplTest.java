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
package io.gravitee.rest.api.service.v4.impl;

import static assertions.RestApiAssertions.assertThat;
import static fixtures.ApiModelFixtures.aModelHttpApiV4;
import static fixtures.PlanModelFixtures.aKeylessPlanV4;
import static fixtures.PlanModelFixtures.anApiKeyPanV4;
import static io.gravitee.rest.api.model.v4.api.DuplicateOptions.FilteredFieldsEnum.GROUPS;
import static io.gravitee.rest.api.model.v4.api.DuplicateOptions.FilteredFieldsEnum.MEMBERS;
import static io.gravitee.rest.api.model.v4.api.DuplicateOptions.FilteredFieldsEnum.PAGES;
import static io.gravitee.rest.api.model.v4.api.DuplicateOptions.FilteredFieldsEnum.PLANS;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ListenerModelFixtures;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.DuplicateOptions;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.MembershipDuplicateService;
import io.gravitee.rest.api.service.PageDuplicateService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiDuplicateException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

@ExtendWith(MockitoExtension.class)
public class ApiDuplicateServiceImplTest {

    private static final String USERNAME = "admin";
    private static final String API_ID = "source-id";
    private static final String DUPLICATE_API_ID = "duplicate-id";

    private static final PrimaryOwnerEntity NEW_PRIMARY_OWNER = PrimaryOwnerEntity
        .builder()
        .id("userId")
        .displayName(USERNAME)
        .type("USER")
        .build();

    @Mock
    ApiService apiService;

    @Mock
    PageDuplicateService pageDuplicateService;

    @Mock
    PlanService planService;

    @Mock
    MembershipDuplicateService membershipDuplicateService;

    ApiDuplicateServiceImpl service;

    DuplicateOptions duplicateOptions;

    ApiEntity sourceApi;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "fake-uuid");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        service = new ApiDuplicateServiceImpl(apiService, pageDuplicateService, planService, membershipDuplicateService);

        duplicateOptions = DuplicateOptions.builder().contextPath("/my-context-path").filteredFields(List.of(PAGES, MEMBERS)).build();

        sourceApi = aModelHttpApiV4().toBuilder().id(API_ID).plans(Set.of(aKeylessPlanV4(), anApiKeyPanV4())).build();

        GraviteeContext.setCurrentEnvironment("my-env");
        GraviteeContext.setCurrentOrganization("my-org");

        SecurityContextHolder.setContext(
            new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(
                    new UserDetails(USERNAME, "", Collections.emptyList()),
                    "",
                    Collections.emptyList()
                )
            )
        );

        lenient()
            .when(apiService.createWithImport(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class), eq(USERNAME)))
            .thenAnswer(invocation -> {
                ApiEntity apiEntity = invocation.getArgument(1);
                assertThat(apiEntity.getId()).as("ApiEntity to duplicate should not have id defined").isNull();
                assertThat(apiEntity.getCrossId()).as("ApiEntity to duplicate should not have crossId defined").isNull();
                assertThat(apiEntity.getPrimaryOwner()).as("ApiEntity to duplicate should not have primaryOwner defined").isNull();
                return apiEntity.withId(DUPLICATE_API_ID).withPrimaryOwner(NEW_PRIMARY_OWNER);
            });

        lenient()
            .when(planService.createOrUpdatePlan(eq(GraviteeContext.getExecutionContext()), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class HttpListenerApi {

        @Test
        void should_throw_if_no_context_path() {
            assertThatThrownBy(() ->
                    service.duplicate(GraviteeContext.getExecutionContext(), sourceApi, duplicateOptions.withContextPath(null))
                )
                .isInstanceOf(ApiDuplicateException.class)
                .hasMessage("Cannot find a context-path for HTTP Listener");
        }

        @Test
        void should_override_http_listener_path() {
            ApiEntity duplicated = service.duplicate(
                GraviteeContext.getExecutionContext(),
                sourceApi,
                duplicateOptions.withContextPath("/new-path")
            );

            var sourceHttpListener = sourceApi
                .getListeners()
                .stream()
                .filter(l -> l instanceof HttpListener)
                .findFirst()
                .map(l -> (HttpListener) l);
            var duplicatedHttpListener = duplicated
                .getListeners()
                .stream()
                .filter(l -> l instanceof HttpListener)
                .findFirst()
                .map(l -> (HttpListener) l);

            assertThat(duplicatedHttpListener)
                .isNotEmpty()
                .get()
                .extracting(HttpListener::getPaths, Listener::getEntrypoints, Listener::getServers)
                .contains(
                    List.of(Path.builder().path("/new-path").build()),
                    sourceHttpListener.map(Listener::getEntrypoints).orElse(null),
                    sourceHttpListener.map(Listener::getServers).orElse(null)
                );
        }
    }

    @Nested
    class TcpListenerApi {

        @BeforeEach
        void setTcpListener() {
            sourceApi = sourceApi.withListeners(List.of(ListenerModelFixtures.aModelTcpListener()));
        }

        @Test
        void should_throw_if_no_host() {
            assertThatThrownBy(() -> service.duplicate(GraviteeContext.getExecutionContext(), sourceApi, duplicateOptions.withHost(null)))
                .isInstanceOf(ApiDuplicateException.class)
                .hasMessage("Cannot find a host for TCP Listener");
        }

        @Test
        void should_override_http_listener_path() {
            ApiEntity duplicated = service.duplicate(
                GraviteeContext.getExecutionContext(),
                sourceApi,
                duplicateOptions.withHost("new-host")
            );

            var sourceTcpListener = sourceApi
                .getListeners()
                .stream()
                .filter(l -> l instanceof TcpListener)
                .findFirst()
                .map(l -> (TcpListener) l);
            var duplicatedTcpListener = duplicated
                .getListeners()
                .stream()
                .filter(l -> l instanceof TcpListener)
                .findFirst()
                .map(l -> (TcpListener) l);

            assertThat(duplicatedTcpListener)
                .isNotEmpty()
                .get()
                .extracting(TcpListener::getHosts, Listener::getEntrypoints, Listener::getServers)
                .contains(
                    List.of("new-host"),
                    sourceTcpListener.map(Listener::getEntrypoints).orElse(null),
                    sourceTcpListener.map(Listener::getServers).orElse(null)
                );
        }
    }

    @Test
    void should_create_a_new_api_and_keep_the_config() {
        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi,
            duplicateOptions.withContextPath("/new-path")
        );

        assertThat(duplicated)
            .hasId(DUPLICATE_API_ID)
            .hasNoCrossId()
            .hasName(sourceApi.getName())
            .hasDefinitionVersion(sourceApi.getDefinitionVersion())
            .hasType(sourceApi.getType())
            .hasDescription(sourceApi.getDescription())
            .hasOnlyTags(sourceApi.getTags())
            .hasOnlyEndpointGroups(sourceApi.getEndpointGroups())
            .hasAnalytics(sourceApi.getAnalytics())
            .hasOnlyProperties(sourceApi.getProperties())
            .hasOnlyResources(sourceApi.getResources())
            .hasFlowExecution(sourceApi.getFlowExecution())
            .hasOnlyFlows(sourceApi.getFlows())
            .hasOnlyResponseTemplatesKeys(sourceApi.getResponseTemplates().keySet())
            .hasServices(sourceApi.getServices())
            .hasVisibility(sourceApi.getVisibility())
            .hasState(sourceApi.getState())
            .hasPrimaryOwner(NEW_PRIMARY_OWNER)
            .hasPicture(sourceApi.getPicture())
            .hasPictureUrl(sourceApi.getPictureUrl())
            .hasOnlyCategories(sourceApi.getCategories())
            .hasOnlyLabels(sourceApi.getLabels())
            .hasOriginContext(sourceApi.getOriginContext())
            .hasOnlyMetadataKeys(sourceApi.getMetadata().keySet())
            .hasDisableMembershipNotifications(sourceApi.isDisableMembershipNotifications())
            .hasBackground(sourceApi.getBackground())
            .hasBackgroundUrl(sourceApi.getBackgroundUrl())
            .hasLifecycleState(sourceApi.getLifecycleState())
            .hasWorkflowState(sourceApi.getWorkflowState());
    }

    @Test
    void should_override_version_if_provided() {
        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi,
            duplicateOptions.withVersion("0.0.0-draft")
        );

        assertThat(duplicated).hasApiVersion("0.0.0-draft");
    }

    @Test
    void should_keep_source_version_if_not_provided() {
        ApiEntity duplicated = service.duplicate(GraviteeContext.getExecutionContext(), sourceApi, duplicateOptions.withVersion(null));

        assertThat(duplicated).hasApiVersion(sourceApi.getApiVersion());
    }

    @Test
    void should_reset_groups_if_filtered() {
        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi,
            duplicateOptions.withFilteredFields(List.of(PAGES, MEMBERS, GROUPS))
        );

        assertThat(duplicated).hasNoGroup();
    }

    @Test
    void should_keep_source_groups_if_not_filtered() {
        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi,
            duplicateOptions.withFilteredFields(List.of(PLANS, PAGES, MEMBERS))
        );

        assertThat(duplicated).hasOnlyGroups(sourceApi.getGroups());
    }

    @Test
    void should_not_duplicate_pages_if_filtered() {
        service.duplicate(GraviteeContext.getExecutionContext(), sourceApi, duplicateOptions.withFilteredFields(List.of(PAGES, MEMBERS)));

        verify(pageDuplicateService, never()).duplicatePages(any(), any(), any(), any());
    }

    @Test
    void should_duplicate_pages_if_not_filtered() {
        service.duplicate(GraviteeContext.getExecutionContext(), sourceApi, duplicateOptions.withFilteredFields(List.of(MEMBERS)));

        verify(pageDuplicateService).duplicatePages(GraviteeContext.getExecutionContext(), API_ID, DUPLICATE_API_ID, USERNAME);
    }

    @Test
    void should_reset_plans_if_filtered() {
        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi,
            duplicateOptions.withFilteredFields(List.of(PLANS, PAGES, MEMBERS))
        );

        assertThat(duplicated).hasNoPlan();
    }

    @Test
    void should_duplicate_plans_if_not_filtered() {
        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi,
            duplicateOptions.withFilteredFields(List.of(PAGES, MEMBERS))
        );

        assertThat(duplicated)
            .hasOnlyPlans(
                sourceApi.getPlans().stream().map(p -> p.toBuilder().id("fake-uuid").apiId(DUPLICATE_API_ID).build()).collect(toSet())
            );

        verify(planService, times(sourceApi.getPlans().size())).createOrUpdatePlan(any(), any());
    }

    @Test
    void should_duplicate_plans_and_update_general_conditions_page_id_when_pages_also_duplicated() {
        when(pageDuplicateService.duplicatePages(any(), any(), any(), any()))
            .thenReturn(Map.ofEntries(entry("page-1", "dup-page-1"), entry("page-2", "dup-page-2")));

        PlanEntity keylessPlan = aKeylessPlanV4().toBuilder().generalConditions("page-1").build();
        PlanEntity apiKeyPlan = anApiKeyPanV4().toBuilder().generalConditions("page-2").build();
        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi.withPlans(Set.of(keylessPlan, apiKeyPlan)),
            duplicateOptions.withFilteredFields(List.of(MEMBERS))
        );

        assertThat(duplicated)
            .hasOnlyPlans(
                Set.of(
                    keylessPlan.toBuilder().id("fake-uuid").apiId(DUPLICATE_API_ID).generalConditions("dup-page-1").build(),
                    apiKeyPlan.toBuilder().id("fake-uuid").apiId(DUPLICATE_API_ID).generalConditions("dup-page-2").build()
                )
            );

        verify(planService, times(sourceApi.getPlans().size())).createOrUpdatePlan(any(), any());
    }

    @Test
    void should_skip_plan_when_duplicate_fails() {
        PlanEntity keylessPlan = aKeylessPlanV4();
        PlanEntity apiKeyPlan = anApiKeyPanV4();
        when(planService.createOrUpdatePlan(any(), eq(keylessPlan))).thenThrow(new TechnicalManagementException("error"));

        ApiEntity duplicated = service.duplicate(
            GraviteeContext.getExecutionContext(),
            sourceApi.withPlans(Set.of(keylessPlan, apiKeyPlan)),
            duplicateOptions.withFilteredFields(List.of(PAGES, MEMBERS))
        );

        assertThat(duplicated).hasOnlyPlans(Set.of(apiKeyPlan.toBuilder().id("fake-uuid").apiId(DUPLICATE_API_ID).build()));

        verify(planService, times(sourceApi.getPlans().size())).createOrUpdatePlan(any(), any());
    }

    @Test
    void should_not_duplicate_members_if_filtered() {
        service.duplicate(GraviteeContext.getExecutionContext(), sourceApi, duplicateOptions.withFilteredFields(List.of(MEMBERS)));

        verify(membershipDuplicateService, never()).duplicateMemberships(any(), any(), any(), any());
    }

    @Test
    void should_duplicate_members_if_not_filtered() {
        service.duplicate(GraviteeContext.getExecutionContext(), sourceApi, duplicateOptions.withFilteredFields(List.of()));

        verify(membershipDuplicateService)
            .duplicateMemberships(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(DUPLICATE_API_ID), eq(USERNAME));
    }
}
