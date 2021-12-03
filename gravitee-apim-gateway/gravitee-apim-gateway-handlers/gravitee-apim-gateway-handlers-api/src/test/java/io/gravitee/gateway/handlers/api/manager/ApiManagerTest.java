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
package io.gravitee.gateway.handlers.api.manager;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.Properties;
import io.gravitee.el.TemplateContext;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.cache.standalone.StandaloneCache;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiManagerTest {

    @InjectMocks
    private ApiManager apiManager = new ApiManagerImpl();

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventManager eventManager;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private DataEncryptor dataEncryptor;

    @Before
    public void setUp() {
        apiManager = spy(new ApiManagerImpl());
        MockitoAnnotations.initMocks(this);

        ((ApiManagerImpl) apiManager).setApis(new StandaloneCache<>("api_manager_test", new CacheConfiguration()));
        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        when(gatewayConfiguration.hasMatchingTags(any())).thenCallRealMethod();
    }

    @Test
    public void shouldNotDeployDisableApi() throws Exception {
        final Api api = buildTestApi();
        api.setEnabled(false);

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithoutPlan() throws Exception {
        final Api api = buildTestApi();

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
        assertEquals(0, apiManager.apis().size());
    }

    @Test
    public void shouldDeployApiWithPlan() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
        assertEquals(1, apiManager.apis().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotDeployApi_invalidTag() throws Exception {
        shouldDeployApiWithTags("test,!test", new String[] {});
    }

    @Test
    public void shouldDeployApiWithTagOnGatewayWithoutTag() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));
        api.setTags(new HashSet<>(singletonList("test")));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithTagOnGatewayTagExclusion() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));
        api.setTags(new HashSet<>(Arrays.asList("product", "international")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("product", "!international")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithTagOnGatewayWithoutTag() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("product")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    private void shouldDeployApiWithTags(final String tags, final String[] apiTags) throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));
        api.setTags(new HashSet<>(Arrays.asList(apiTags)));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList(tags.split(","))));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldDeployApiWithPlanMatchingTag() throws Exception {
        final Api api = buildTestApi();
        api.setTags(new HashSet<>(singletonList("test")));

        final Plan mockedPlan = mock(Plan.class);
        when(mockedPlan.getTags()).thenReturn(new HashSet<>(singletonList("test")));
        api.setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void shouldNotDeployApiWithoutPlanMatchingTag() throws Exception {
        final Api api = buildTestApi();
        api.setTags(new HashSet<>(singletonList("test")));

        final Plan mockedPlan = mock(Plan.class);
        when(mockedPlan.getTags()).thenReturn(new HashSet<>(singletonList("test2")));
        api.setPlans(singletonList(mockedPlan));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        apiManager.register(api);

        verify(eventManager, never()).publishEvent(ReactorEvent.DEPLOY, api);
    }

    @Test
    public void test_deployApiWithTag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithTagExcluded() throws Exception {
        shouldDeployApiWithTags("!test", new String[] { "toto" });
    }

    @Test
    public void test_deployApiWithUpperCasedTag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "Test" });
    }

    @Test
    public void test_deployApiWithAccentTag() throws Exception {
        shouldDeployApiWithTags("test,toto", new String[] { "tést" });
    }

    @Test
    public void test_deployApiWithUpperCasedAndAccentTag() throws Exception {
        shouldDeployApiWithTags("test", new String[] { "Tést" });
    }

    @Test
    public void test_deployApiWithTagExclusion() throws Exception {
        shouldDeployApiWithTags("test,!toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithSpaceAfterComma() throws Exception {
        shouldDeployApiWithTags("test, !toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithSpaceBeforeComma() throws Exception {
        shouldDeployApiWithTags("test ,!toto", new String[] { "test" });
    }

    @Test
    public void test_deployApiWithSpaceBeforeTag() throws Exception {
        shouldDeployApiWithTags(" test,!toto", new String[] { "test" });
    }

    @Test
    public void shouldUpdateApi() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final Api api2 = buildTestApi();
        Instant deployDateInst = api.getDeployedAt().toInstant().plus(Duration.ofHours(1));
        api2.setDeployedAt(Date.from(deployDateInst));
        api2.setPlans(singletonList(mockedPlan));

        apiManager.register(api2);

        verify(eventManager).publishEvent(ReactorEvent.UPDATE, api);
    }

    @Test
    public void shouldNotUpdateApi() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final Api api2 = buildTestApi();
        Instant deployDateInst = api.getDeployedAt().toInstant().minus(Duration.ofHours(1));
        api2.setDeployedAt(Date.from(deployDateInst));

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
    }

    @Test
    public void shouldUndeployApi_noMoreMatchingTag() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));
        api.setTags(new HashSet<>(singletonList("test")));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("test")));

        apiManager.register(api);

        final Api api2 = buildTestApi();
        api2.setDeployedAt(new Date());
        api2.setTags(new HashSet<>(singletonList("other-tag")));

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldUndeployApi() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        apiManager.unregister(api.getId());

        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldNotUndeployUnknownApi() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        apiManager.unregister("unknown-api");

        verify(eventManager, never()).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldUndeployApi_noMorePlan() throws Exception {
        final Api api = buildTestApi();
        final Plan mockedPlan = mock(Plan.class);

        api.setPlans(singletonList(mockedPlan));

        apiManager.register(api);

        verify(eventManager).publishEvent(ReactorEvent.DEPLOY, api);

        final Api api2 = buildTestApi();
        api2.setDeployedAt(new Date(api.getDeployedAt().getTime() + 100));
        api2.setPlans(Collections.<Plan>emptyList());

        apiManager.register(api2);

        verify(eventManager, never()).publishEvent(ReactorEvent.UPDATE, api);
        verify(eventManager).publishEvent(ReactorEvent.UNDEPLOY, api);
    }

    @Test
    public void shouldDecryptApiPropertiesOnDeployment() throws Exception {
        final Api api = buildTestApi();

        Properties properties = new Properties();
        properties.setProperties(
            List.of(
                new Property("key1", "plain value 1", false),
                new Property("key2", "value2Base64encrypted", true),
                new Property("key3", "value3Base64encrypted", true)
            )
        );
        api.setProperties(properties);

        when(dataEncryptor.decrypt("value2Base64encrypted")).thenReturn("plain value 2");
        when(dataEncryptor.decrypt("value3Base64encrypted")).thenReturn("plain value 3");

        apiManager.register(api);

        verify(dataEncryptor, times(2)).decrypt(any());
        assertEquals(Map.of("key1", "plain value 1", "key2", "plain value 2", "key3", "plain value 3"), api.getProperties().getValues());
    }

    /*
    @Test
    public void test_twiceWithTwoApis_apiToRemove() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        apiManager.register(mockApi);

        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test-2").updatedAt(new Date()).definition("test2").build();
        final Api mockApi2 = mockApi(api2);
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);

        when(eventRepository.search(
                any(EventCriteria.class)
        )).thenReturn(singletonList(mockEvent));

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(argThat(api1 -> api1.getId().equals(mockApi.getId()) || api2.getId().equals(mockApi2.getId())));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(api.getId());
        verify(apiManager, never()).undeploy(api2.getId());
    }

    @Test
    public void test_twiceWithTwoApis_apiToUpdate() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test").updatedAt(Date.from(updateDateInst)).definition("test2").build();

        final Api mockApi = mockApi(api);
        mockApi(api2);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);

        List<Event> events = new ArrayList<>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(events, 0, 0, 1));

        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        syncManager.refresh();

        when(eventRepository.search(
                any(EventCriteria.class)
        )).thenReturn(singletonList(mockEvent2));

        final Api apiDefinition = new Api(mockApi);
        apiDefinition.setEnabled(api.getLifecycleState() == LifecycleState.STARTED);
        apiDefinition.setDeployedAt(api.getDeployedAt());
        when(apiManager.get(api.getId())).thenReturn(apiDefinition);

        syncManager.refresh();

        verify(apiManager).deploy(apiDefinition);
        verify(apiManager).update(apiDefinition);
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_api_noUpdate() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test").updatedAt(api.getUpdatedAt()).definition("test").build();

        final Api mockApi = mockApi(api);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);

        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);

        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(events, 0, 0, 1));

        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        syncManager.refresh();

        when(eventRepository.search(
                any(EventCriteria.class)
        )).thenReturn(singletonList(mockEvent2));

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(new Api(mockApi));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_not_deployApiWithTagExclusion() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);
        mockApi.setTags(new HashSet<>(Arrays.asList(new String[]{"test"})));

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(singletonList("!test")));
        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        syncManager.refresh();

        verify(apiManager, never()).deploy(new Api(mockApi));
        verify(apiManager, never()).update(any(Api.class));
    }

    @Test
    public void test_deployApiWithTagInclusionExclusion() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
//        api.setTags(new HashSet<>(Arrays.asList(new String[]{"test", "toto"})));

        final Api mockApi = mockApi(api);

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("!test", "toto")));
        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        syncManager.refresh();

        verify(apiManager).deploy(new Api(mockApi));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_not_deployApiWithoutTag() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("test", "toto")));
        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        syncManager.refresh();

        verify(apiManager, never()).deploy(new Api(mockApi));
        verify(apiManager, never()).update(any(Api.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore
    public void shouldNotDeployBecauseWrongConfiguration() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(gatewayConfiguration.shardingTags()).thenReturn(Optional.of(Arrays.asList("test", "!test")));
        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));
        when(apiManager.apis()).thenReturn(Collections.singleton(new Api(mockApi)));

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.search(
                eq(new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .property(Event.EventProperties.API_ID.getValue(), api.getId())
                        .build()), any()
        )).thenReturn((new Page(singletonList(mockEvent), 0, 0, 0)));

        syncManager.refresh();
    }

    @Test
    public void test_not_deployApiWithoutEvent() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(Collections.emptyList(), 0, 0, 0));

        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        syncManager.refresh();

        verify(apiManager, never()).deploy(new Api(mockApi));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_deployOnlyOneApiWithTwoApisAndOneEvent() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder().id("api-test-2").updatedAt(new Date()).definition("test2").build();

        final Api mockApi = mockApi(api);

        final List<io.gravitee.repository.management.model.Api> apis = new ArrayList<>();
        apis.add(api);
        apis.add(api2);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(
                new Page<>(Collections.emptyList(), 0, 0, 0),
                new Page<>(singletonList(mockEvent), 0, 0, 1));

        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(apis);

        syncManager.refresh();

        verify(apiManager).deploy(argThat(api1 -> api1.getId().equals(mockApi.getId())));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_shouldUndeployIfLastEventIsUnpublishAPI() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api, EventType.UNPUBLISH_API);

        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1), new Page<>(singletonList(mockEvent2), 0, 0, 1));

        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        syncManager.refresh();

        when(eventRepository.search(
                any(EventCriteria.class)
        )).thenReturn(singletonList(mockEvent2));

        syncManager.refresh();

        verify(apiManager).deploy(new Api(mockApi));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager).undeploy(mockApi.getId());
    }


    @Test
    public void test_shouldUpdateIfLastEventIsStartAPI() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
                .id("api-test")
                .updatedAt(new Date())
                .definition("test")
                .lifecycleState(LifecycleState.STOPPED)
                .build();

        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder()
                .id("api-test")
                .updatedAt(Date.from(updateDateInst))
                .definition("test")
                .lifecycleState(LifecycleState.STARTED)
                .build();

        final Api mockApi = mockApi(api);
        mockApi(api2);
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.START_API);

        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        syncManager.refresh();

        final Api apiDefinition = new Api(mockApi);
        apiDefinition.setEnabled(api.getLifecycleState() == LifecycleState.STARTED);
        apiDefinition.setDeployedAt(api.getDeployedAt());
        when(apiManager.get(api.getId())).thenReturn(apiDefinition);

        when(eventRepository.search(
                any(EventCriteria.class)
        )).thenReturn(singletonList(mockEvent2));

        syncManager.refresh();

        verify(apiManager).deploy(new Api(mockApi));
        verify(apiManager).update(new Api(mockApi));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_shouldUpdateIfLastEventIsStopAPI() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
                .id("api-test")
                .updatedAt(new Date())
                .definition("test")
                .lifecycleState(LifecycleState.STARTED)
                .build();

        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder()
                .id("api-test")
                .updatedAt(Date.from(updateDateInst))
                .definition("test")
                .lifecycleState(LifecycleState.STOPPED)
                .build();

        final Api mockApi = mockApi(api);
        mockApi(api2);
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.STOP_API);

        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                any(EventCriteria.class),
                any(Pageable.class)
        )).thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        when(apiRepository.search(null, new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())).thenReturn(singletonList(api));

        syncManager.refresh();

        when(eventRepository.search(
                any(EventCriteria.class)
        )).thenReturn(singletonList(mockEvent));

        final Api apiDefinition = new Api(mockApi);
        apiDefinition.setEnabled(api.getLifecycleState() == LifecycleState.STARTED);
        apiDefinition.setDeployedAt(api.getDeployedAt());
        when(apiManager.get(api.getId())).thenReturn(apiDefinition);

        syncManager.refresh();

        verify(apiManager).deploy(new Api(mockApi));
        verify(apiManager).update(new Api(mockApi));
        verify(apiManager, never()).undeploy(any(String.class));
    }
     */

    private Api mockApi(final io.gravitee.repository.management.model.Api api) throws Exception {
        return mockApi(api, new String[] {});
    }

    private Api mockApi(final io.gravitee.repository.management.model.Api api, final String[] tags) throws Exception {
        final Api mockApi = new Api();
        mockApi.setId(api.getId());
        mockApi.setName(api.getName());
        mockApi.setTags(new HashSet<>(Arrays.asList(tags)));
        when(objectMapper.readValue(api.getDefinition(), Api.class)).thenReturn(mockApi);
        return mockApi;
    }

    private Api buildTestApi() {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(singletonList(mock(VirtualHost.class)));
        return new ApiBuilder().id("api-test").name("api-name-test").proxy(proxy).deployedAt(new Date()).build();
    }

    class ApiBuilder {

        private final Api api = new Api();

        public ApiBuilder id(String id) {
            this.api.setId(id);
            return this;
        }

        public ApiBuilder name(String name) {
            this.api.setName(name);
            return this;
        }

        public ApiBuilder proxy(Proxy proxy) {
            this.api.setProxy(proxy);
            return this;
        }

        public ApiBuilder deployedAt(Date updatedAt) {
            this.api.setDeployedAt(updatedAt);
            return this;
        }

        public Api build() {
            api.setEnabled(true);

            return this.api;
        }
    }
}
