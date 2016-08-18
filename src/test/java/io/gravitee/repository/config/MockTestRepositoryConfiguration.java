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
package io.gravitee.repository.config;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.model.*;
import org.springframework.context.annotation.Bean;

import java.util.*;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

public class MockTestRepositoryConfiguration {

    @Bean
    public TestRepositoryInitializer testRepositoryInitializer() {
        return mock(TestRepositoryInitializer.class);
    }

    @Bean
    public ApiKeyRepository apiKeyRepository() throws Exception {
        final ApiKeyRepository apiKeyRepository = mock(ApiKeyRepository.class);

        final ApiKey apiKey = mock(ApiKey.class);
        when(apiKey.getKey()).thenReturn("apiKey");
        when(apiKey.getExpiration()).thenReturn(parse("11/02/2016"));
        when(apiKeyRepository.retrieve(anyString())).thenReturn(empty());
        when(apiKeyRepository.retrieve("d449098d-8c31-4275-ad59-8dd707865a33")).thenReturn(of(apiKey));
        when(apiKeyRepository.retrieve("apiKey")).thenReturn(of(apiKey));
        when(apiKey.getApi()).thenReturn("");
        when(apiKeyRepository.findByApplication("application1")).thenReturn(newSet(apiKey, mock(ApiKey.class)));
        when(apiKeyRepository.findByApplicationAndApi("application1", "api1")).thenReturn(newSet(apiKey));

        return apiKeyRepository;
    }

    @Bean
    public ApiRepository apiRepository() throws Exception {
        final ApiRepository apiRepository = mock(ApiRepository.class);

        final Api api = mock(Api.class);
        when(apiRepository.findByMember("toto", MembershipType.PRIMARY_OWNER, Visibility.PRIVATE)).thenReturn(newSet(api));
        when(apiRepository.findByMember("toto", MembershipType.PRIMARY_OWNER, null)).thenReturn(newSet(api, mock(Api.class)));
        when(apiRepository.findById(anyString())).thenReturn(empty());
        when(apiRepository.findById("api1")).thenReturn(of(api));

        final Set<Api> apis = newSet(api, mock(Api.class));
        when(apiRepository.findAll()).thenReturn(apis);
        doAnswer(invocation -> apis.remove(api)).when(apiRepository).delete("api1");

        final Api newApi = mock(Api.class);
        when(newApi.getVersion()).thenReturn("1");
        when(newApi.getLifecycleState()).thenReturn(LifecycleState.STOPPED);
        when(newApi.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(newApi.getDefinition()).thenReturn("{}");
        when(newApi.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(newApi.getUpdatedAt()).thenReturn(parse("12/02/2016"));
        when(apiRepository.findById("sample-new")).thenReturn(of(newApi));

        return apiRepository;
    }

    @Bean
    public ApplicationRepository applicationRepository() throws Exception {
        final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        final Application application = mock(Application.class);
        when(applicationRepository.findByUser("po", MembershipType.PRIMARY_OWNER))
                .thenReturn(newSet(application, mock(Application.class), mock(Application.class)));
        when(applicationRepository.findByUser("user", null)).thenReturn(newSet(application, mock(Application.class), mock(Application.class)));
        when(applicationRepository.findById("application-sample")).thenReturn(of(application));

        final Set<Application> applications = newSet(application, mock(Application.class), mock(Application.class));
        when(applicationRepository.findAll()).thenReturn(applications);
        doAnswer(invocation -> applications.remove(application)).when(applicationRepository).delete("deleted-app");
        when(applicationRepository.findById("deleted-app")).thenReturn(empty());

        final Application newApplication = mock(Application.class);
        when(newApplication.getName()).thenReturn("created-app");
        when(newApplication.getDescription()).thenReturn("Application description");
        when(newApplication.getType()).thenReturn("type");
        when(newApplication.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(newApplication.getUpdatedAt()).thenReturn(parse("12/02/2016"));

        when(applicationRepository.findById("created-app")).thenReturn(of(newApplication));

        final Application updatedApplication = mock(Application.class);
        when(updatedApplication.getName()).thenReturn("updated-app");
        when(updatedApplication.getDescription()).thenReturn("Updated description");
        when(updatedApplication.getType()).thenReturn("update-type");
        when(updatedApplication.getCreatedAt()).thenReturn(null);
        when(updatedApplication.getUpdatedAt()).thenReturn(parse("22/02/2016"));

        when(applicationRepository.findById("updated-app")).thenReturn(of(updatedApplication));

        return applicationRepository;
    }

    @Bean
    public EventRepository eventRepository() throws Exception {
        final EventRepository eventRepository = mock(EventRepository.class);

        final Event event1 = mock(Event.class);
        final Event event2 = mock(Event.class);
        final Event event3 = mock(Event.class);
        final Event event4 = mock(Event.class);
        final Event event5 = mock(Event.class);
        final Event event6 = mock(Event.class);
        final Page<Event> pageEvent = mock(Page.class);
        final Page<Event> pageEvent2 = mock(Page.class);
        final Page<Event> pageEvent3 = mock(Page.class);
        final Page<Event> pageEvent4 = mock(Page.class);
        final Page<Event> pageEvent5 = mock(Page.class);

        Map<String, String> eventProperties = new HashMap<>();
        eventProperties.put("api_id", "api-1");

        Map<String, String> eventProperties2 = new HashMap<>();
        eventProperties.put("api_id", "api-4");

        when(eventRepository.findByProperty(Event.EventProperties.API_ID.getValue(), "api-1"))
                .thenReturn(newSet(event1, event2));
        when(event1.getId()).thenReturn("event1");
        when(event1.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(event1.getType()).thenReturn(EventType.PUBLISH_API);
        when(event1.getPayload()).thenReturn("{}");
        when(event1.getProperties()).thenReturn(eventProperties);
        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.UNPUBLISH_API);
        when(event2.getCreatedAt()).thenReturn(parse("12/02/2016"));
        when(event2.getProperties()).thenReturn(eventProperties);
        when(event3.getId()).thenReturn("event3");
        when(event3.getType()).thenReturn(EventType.PUBLISH_API);
        when(event3.getCreatedAt()).thenReturn(parse("13/02/2016"));
        when(event4.getId()).thenReturn("event4");
        when(event4.getType()).thenReturn(EventType.STOP_API);
        when(event4.getCreatedAt()).thenReturn(parse("14/02/2016"));
        when(event4.getProperties()).thenReturn(eventProperties2);
        when(event5.getId()).thenReturn("event5");
        when(event5.getType()).thenReturn(EventType.START_API);
        when(event5.getCreatedAt()).thenReturn(parse("15/02/2016"));
        when(event6.getId()).thenReturn("event6");
        when(event6.getType()).thenReturn(EventType.START_API);
        when(event6.getCreatedAt()).thenReturn(parse("16/02/2016"));


        when(eventRepository.findByType(Arrays.asList(EventType.PUBLISH_API, EventType.UNPUBLISH_API)))
                .thenReturn(newSet(event1, event2, event3));

        when(eventRepository.findById("event1")).thenReturn(of(event1));

        when(eventRepository.create(any(Event.class))).thenReturn(event1);

        Map<String, Object> values = new HashMap<>();
        values.put("type", EventType.START_API.toString());
        when(pageEvent.getTotalElements()).thenReturn(2l);
        when(pageEvent.getContent()).thenReturn(Arrays.asList(event5, event6));
        when(eventRepository.search(values, 1451606400000l, 1470157767000l, 0, 10)).thenReturn(pageEvent);

        values = new HashMap<>();
        values.put("type", Arrays.asList(EventType.START_API.toString(), EventType.STOP_API.toString()));
        when(pageEvent2.getTotalElements()).thenReturn(3l);
        when(pageEvent2.getContent()).thenReturn(Arrays.asList(event4, event5, event6));
        when(eventRepository.search(values, 1451606400000l, 1470157767000l, 0, 10)).thenReturn(pageEvent2);

        values = new HashMap<>();
        values.put("properties.api_id", "api-1");
        when(pageEvent3.getTotalElements()).thenReturn(2l);
        when(pageEvent3.getContent()).thenReturn(Arrays.asList(event1, event2));
        when(eventRepository.search(values, 1451606400000l, 1470157767000l, 0, 10)).thenReturn(pageEvent3);

        values = new HashMap<>();
        values.put("type", EventType.START_API.toString());
        when(pageEvent4.getTotalElements()).thenReturn(0l);
        when(pageEvent4.getContent()).thenReturn(Collections.emptyList());
        when(eventRepository.search(values, 1420070400000l, 1422748800000l, 0, 10)).thenReturn(pageEvent4);

        values = new HashMap<>();
        values.put("type", Arrays.asList(EventType.START_API.toString(), EventType.STOP_API.toString()));
        values.put("properties.api_id", "api-3");
        when(pageEvent5.getTotalElements()).thenReturn(1l);
        when(pageEvent5.getContent()).thenReturn(Arrays.asList(event4));
        when(eventRepository.search(values, 1451606400000l, 1470157767000l, 0, 10)).thenReturn(pageEvent5);

        return eventRepository;
    }

    @Bean
    public UserRepository userRepository() throws Exception {
        final UserRepository userRepository = mock(UserRepository.class);

        final User user = mock(User.class);
        when(userRepository.findAll()).thenReturn(newSet(user, mock(User.class), mock(User.class),
                mock(User.class), mock(User.class), mock(User.class)));
        when(userRepository.create(any(User.class))).thenReturn(user);
        when(userRepository.findByUsername("createuser1")).thenReturn(of(user));
        when(user.getUsername()).thenReturn("createuser1");
        when(user.getEmail()).thenReturn("createuser1@gravitee.io");

        return userRepository;
    }
}
