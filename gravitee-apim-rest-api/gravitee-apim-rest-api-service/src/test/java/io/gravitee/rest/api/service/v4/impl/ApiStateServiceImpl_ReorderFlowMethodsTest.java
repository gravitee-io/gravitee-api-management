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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test cases for the reorderFlowMethods functionality in ApiStateServiceImpl
 *
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiStateServiceImpl_ReorderFlowMethodsTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private EventService eventService;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private ApiValidationService apiValidationService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private ApiMapper apiMapper;

    @Mock
    private PlanSearchService planSearchService;

    @InjectMocks
    private SynchronizationService synchronizationService = Mockito.spy(new SynchronizationService(this.objectMapper));

    private ApiStateServiceImpl apiStateService;

    @Before
    public void setUp() {
        GenericApiMapper genericApiMapper = mock(GenericApiMapper.class);
        apiStateService =
            new ApiStateServiceImpl(
                apiSearchService,
                apiRepository,
                apiMapper,
                genericApiMapper,
                apiNotificationService,
                primaryOwnerService,
                auditService,
                eventService,
                eventLatestRepository,
                objectMapper,
                apiMetadataService,
                apiValidationService,
                planSearchService,
                apiConverter,
                synchronizationService
            );
    }

    @Test
    public void should_handle_null_flows() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        reorderFlowMethodsMethod.invoke(apiStateService, null, null);
    }

    @Test
    public void should_handle_empty_flows() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        List<Flow> emptyOldFlows = Collections.emptyList();
        List<Flow> emptyNewFlows = Collections.emptyList();

        reorderFlowMethodsMethod.invoke(apiStateService, emptyOldFlows, emptyNewFlows);
    }

    @Test
    public void should_handle_flows_without_selectors() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        Flow oldFlow = Flow.builder().name("flow1").build();
        Flow newFlow = Flow.builder().name("flow1").build();

        List<Flow> oldFlows = Collections.singletonList(oldFlow);
        List<Flow> newFlows = Collections.singletonList(newFlow);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);
    }

    @Test
    public void should_reorder_http_methods() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        HttpSelector oldHttpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT)))
            .build();

        HttpSelector newHttpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST)))
            .build();

        Flow oldFlow = Flow.builder().name("flow1").build();
        Flow newFlow = Flow.builder().name("flow1").build();

        oldFlow.setSelectors(Collections.singletonList(oldHttpSelector));
        newFlow.setSelectors(Collections.singletonList(newHttpSelector));

        List<Flow> oldFlows = Collections.singletonList(oldFlow);
        List<Flow> newFlows = Collections.singletonList(newFlow);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);

        HttpSelector reorderedSelector = (HttpSelector) oldFlow.getSelectors().get(0);
        List<HttpMethod> reorderedMethods = new ArrayList<>(reorderedSelector.getMethods());

        assertThat(reorderedMethods.get(0)).isEqualTo(HttpMethod.GET);
        assertThat(reorderedMethods.get(1)).isEqualTo(HttpMethod.PUT);
        assertThat(reorderedMethods.get(2)).isEqualTo(HttpMethod.POST);
    }

    @Test
    public void should_handle_multiple_selectors() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        HttpSelector oldHttpSelector1 = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET)))
            .build();

        HttpSelector oldHttpSelector2 = HttpSelector
            .builder()
            .path("/users")
            .pathOperator(Operator.EQUALS)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.GET)))
            .build();

        HttpSelector newHttpSelector1 = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST)))
            .build();

        HttpSelector newHttpSelector2 = HttpSelector
            .builder()
            .path("/users")
            .pathOperator(Operator.EQUALS)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)))
            .build();

        Flow oldFlow = Flow.builder().name("flow1").build();
        Flow newFlow = Flow.builder().name("flow1").build();

        oldFlow.setSelectors(Arrays.asList(oldHttpSelector1, oldHttpSelector2));
        newFlow.setSelectors(Arrays.asList(newHttpSelector1, newHttpSelector2));

        List<Flow> oldFlows = Collections.singletonList(oldFlow);
        List<Flow> newFlows = Collections.singletonList(newFlow);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);

        HttpSelector reorderedSelector1 = (HttpSelector) oldFlow.getSelectors().get(0);
        List<HttpMethod> reorderedMethods1 = new ArrayList<>(reorderedSelector1.getMethods());

        assertThat(reorderedMethods1.get(0)).isEqualTo(HttpMethod.GET);
        assertThat(reorderedMethods1.get(1)).isEqualTo(HttpMethod.POST);

        HttpSelector reorderedSelector2 = (HttpSelector) oldFlow.getSelectors().get(1);
        List<HttpMethod> reorderedMethods2 = new ArrayList<>(reorderedSelector2.getMethods());

        assertThat(reorderedMethods2.get(0)).isEqualTo(HttpMethod.GET);
        assertThat(reorderedMethods2.get(1)).isEqualTo(HttpMethod.PUT);
        assertThat(reorderedMethods2.get(2)).isEqualTo(HttpMethod.DELETE);
    }

    @Test
    public void should_handle_non_http_selectors() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        Selector nonHttpSelector = mock(Selector.class);

        HttpSelector httpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET)))
            .build();

        HttpSelector referenceHttpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST)))
            .build();

        Flow oldFlow = Flow.builder().name("flow1").build();
        Flow newFlow = Flow.builder().name("flow1").build();

        oldFlow.setSelectors(Arrays.asList(nonHttpSelector, httpSelector));
        newFlow.setSelectors(Arrays.asList(nonHttpSelector, referenceHttpSelector));

        List<Flow> oldFlows = Collections.singletonList(oldFlow);
        List<Flow> newFlows = Collections.singletonList(newFlow);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);

        HttpSelector reorderedSelector = (HttpSelector) oldFlow.getSelectors().get(1);
        List<HttpMethod> reorderedMethods = new ArrayList<>(reorderedSelector.getMethods());

        assertThat(reorderedMethods.get(0)).isEqualTo(HttpMethod.GET);
        assertThat(reorderedMethods.get(1)).isEqualTo(HttpMethod.POST);
    }

    @Test
    public void should_handle_http_selectors_with_null_or_empty_methods() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        HttpSelector httpSelectorWithNullMethods = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(null)
            .build();

        HttpSelector httpSelectorWithEmptyMethods = HttpSelector
            .builder()
            .path("/users")
            .pathOperator(Operator.EQUALS)
            .methods(Collections.emptySet())
            .build();

        HttpSelector referenceHttpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST)))
            .build();

        Flow oldFlow = Flow.builder().name("flow1").build();
        Flow newFlow = Flow.builder().name("flow1").build();

        oldFlow.setSelectors(Arrays.asList(httpSelectorWithNullMethods, httpSelectorWithEmptyMethods));
        newFlow.setSelectors(Collections.singletonList(referenceHttpSelector));

        List<Flow> oldFlows = Collections.singletonList(oldFlow);
        List<Flow> newFlows = Collections.singletonList(newFlow);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);
    }

    @Test
    public void should_handle_different_method_sets() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        HttpSelector oldHttpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET)))
            .build();

        HttpSelector newHttpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.PUT, HttpMethod.DELETE)))
            .build();

        Flow oldFlow = Flow.builder().name("flow1").build();
        Flow newFlow = Flow.builder().name("flow1").build();

        oldFlow.setSelectors(Collections.singletonList(oldHttpSelector));
        newFlow.setSelectors(Collections.singletonList(newHttpSelector));

        List<Flow> oldFlows = Collections.singletonList(oldFlow);
        List<Flow> newFlows = Collections.singletonList(newFlow);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);

        HttpSelector unchangedSelector = (HttpSelector) oldFlow.getSelectors().get(0);
        List<HttpMethod> unchangedMethods = new ArrayList<>(unchangedSelector.getMethods());

        assertThat(unchangedMethods.get(0)).isEqualTo(HttpMethod.POST);
        assertThat(unchangedMethods.get(1)).isEqualTo(HttpMethod.GET);
    }

    @Test
    public void should_handle_different_path_or_pathOperator() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        HttpSelector oldHttpSelector = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET)))
            .build();

        HttpSelector newHttpSelector = HttpSelector
            .builder()
            .path("/users")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST)))
            .build();

        Flow oldFlow = Flow.builder().name("flow1").build();
        Flow newFlow = Flow.builder().name("flow1").build();

        oldFlow.setSelectors(Collections.singletonList(oldHttpSelector));
        newFlow.setSelectors(Collections.singletonList(newHttpSelector));

        List<Flow> oldFlows = Collections.singletonList(oldFlow);
        List<Flow> newFlows = Collections.singletonList(newFlow);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);

        HttpSelector unchangedSelector = (HttpSelector) oldFlow.getSelectors().get(0);
        List<HttpMethod> unchangedMethods = new ArrayList<>(unchangedSelector.getMethods());

        assertThat(unchangedMethods.get(0)).isEqualTo(HttpMethod.POST);
        assertThat(unchangedMethods.get(1)).isEqualTo(HttpMethod.GET);
    }

    @Test
    public void should_handle_multiple_flows() throws Exception {
        Method reorderFlowMethodsMethod = ApiStateServiceImpl.class.getDeclaredMethod("reorderFlowMethods", List.class, List.class);
        reorderFlowMethodsMethod.setAccessible(true);

        HttpSelector oldHttpSelector1 = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.POST, HttpMethod.GET)))
            .build();

        HttpSelector newHttpSelector1 = HttpSelector
            .builder()
            .path("/api")
            .pathOperator(Operator.STARTS_WITH)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST)))
            .build();

        HttpSelector oldHttpSelector2 = HttpSelector
            .builder()
            .path("/users")
            .pathOperator(Operator.EQUALS)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.DELETE, HttpMethod.PUT, HttpMethod.GET)))
            .build();

        HttpSelector newHttpSelector2 = HttpSelector
            .builder()
            .path("/users")
            .pathOperator(Operator.EQUALS)
            .methods(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)))
            .build();

        Flow oldFlow1 = Flow.builder().name("flow1").build();
        Flow newFlow1 = Flow.builder().name("flow1").build();
        Flow oldFlow2 = Flow.builder().name("flow2").build();
        Flow newFlow2 = Flow.builder().name("flow2").build();

        oldFlow1.setSelectors(Collections.singletonList(oldHttpSelector1));
        newFlow1.setSelectors(Collections.singletonList(newHttpSelector1));
        oldFlow2.setSelectors(Collections.singletonList(oldHttpSelector2));
        newFlow2.setSelectors(Collections.singletonList(newHttpSelector2));

        List<Flow> oldFlows = Arrays.asList(oldFlow1, oldFlow2);
        List<Flow> newFlows = Arrays.asList(newFlow1, newFlow2);

        reorderFlowMethodsMethod.invoke(apiStateService, oldFlows, newFlows);

        HttpSelector reorderedSelector1 = (HttpSelector) oldFlow1.getSelectors().get(0);
        List<HttpMethod> reorderedMethods1 = new ArrayList<>(reorderedSelector1.getMethods());

        assertThat(reorderedMethods1.get(0)).isEqualTo(HttpMethod.GET);
        assertThat(reorderedMethods1.get(1)).isEqualTo(HttpMethod.POST);

        HttpSelector reorderedSelector2 = (HttpSelector) oldFlow2.getSelectors().get(0);
        List<HttpMethod> reorderedMethods2 = new ArrayList<>(reorderedSelector2.getMethods());

        assertThat(reorderedMethods2.get(0)).isEqualTo(HttpMethod.GET);
        assertThat(reorderedMethods2.get(1)).isEqualTo(HttpMethod.PUT);
        assertThat(reorderedMethods2.get(2)).isEqualTo(HttpMethod.DELETE);
    }
}
