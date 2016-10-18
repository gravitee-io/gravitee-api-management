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
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
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

        final Api groupedApi = mock(Api.class);
        when(groupedApi.getGroup()).thenReturn("api-group");
        when(apiRepository.findById("grouped-api")).thenReturn(of(groupedApi));

        return apiRepository;
    }

    @Bean
    public ApplicationRepository applicationRepository() throws Exception {
        final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        final Application application = mock(Application.class);
        when(applicationRepository.findById("application-sample")).thenReturn(of(application));

        final Set<Application> applications = newSet(application, mock(Application.class), mock(Application.class), mock(Application.class));
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

        final Application groupedApplication = mock(Application.class);
        when(groupedApplication.getGroup()).thenReturn("application-group");
        when(applicationRepository.findById("grouped-app")).thenReturn(of(groupedApplication));

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
        final Page<Event> pageEvent6 = mock(Page.class);
        final Page<Event> pageEvent7 = mock(Page.class);

        Map<String, String> eventProperties = new HashMap<>();
        eventProperties.put("api_id", "api-1");

        Map<String, String> eventProperties2 = new HashMap<>();
        eventProperties.put("api_id", "api-4");

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

        when(eventRepository.findById("event1")).thenReturn(of(event1));

        when(eventRepository.create(any(Event.class))).thenReturn(event1);

        when(pageEvent.getTotalElements()).thenReturn(2L);
        when(pageEvent.getContent()).thenReturn(Arrays.asList(event6, event5));
        when(eventRepository.search(
                new EventCriteria.Builder().from(1451606400000L).to(1470157767000L).types(EventType.START_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build())).thenReturn(pageEvent);

        when(pageEvent2.getTotalElements()).thenReturn(3L);
        when(pageEvent2.getContent()).thenReturn(Arrays.asList(event6, event5, event4));
        when(eventRepository.search(
                new EventCriteria.Builder().from(1451606400000L).to(1470157767000L).types(EventType.START_API, EventType.STOP_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build())).thenReturn(pageEvent2);

        when(pageEvent3.getTotalElements()).thenReturn(2L);
        when(pageEvent3.getContent()).thenReturn(Arrays.asList(event2, event1));
        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), "api-1")
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build())).thenReturn(pageEvent3);

        when(pageEvent4.getTotalElements()).thenReturn(0L);
        when(pageEvent4.getContent()).thenReturn(Collections.emptyList());
        when(eventRepository.search(
                new EventCriteria.Builder().from(1420070400000L).to(1422748800000L).types(EventType.START_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build())).thenReturn(pageEvent4);

        when(pageEvent5.getTotalElements()).thenReturn(1L);
        when(pageEvent5.getContent()).thenReturn(Collections.singletonList(event4));
        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), "api-3")
                        .types(EventType.START_API, EventType.STOP_API)
                        .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build())).thenReturn(pageEvent5);

        when(pageEvent6.getTotalElements()).thenReturn(2L);
        when(pageEvent6.getContent()).thenReturn(Arrays.asList(event2, event1));
        when(eventRepository.search(
                new EventCriteria.Builder()
                .from(1451606400000L).to(1470157767000L)
                .property(Event.EventProperties.API_ID.getValue(), "api-1")
                .build(),
                null)).thenReturn(pageEvent6);

        when(pageEvent7.getTotalElements()).thenReturn(3L);
        when(pageEvent7.getContent()).thenReturn(Arrays.asList(event4, event2, event1));
        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                        .build(),
                null)).thenReturn(pageEvent7);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .from(1451606400000L).to(1470157767000L)
                        .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                        .build())).thenReturn(Arrays.asList(event4, event2, event1));

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .property(Event.EventProperties.API_ID.getValue(), Arrays.asList("api-1", "api-3"))
                        .build())).thenReturn(Arrays.asList(event4, event2, event1));

        return eventRepository;
    }

    @Bean
    public UserRepository userRepository() throws Exception {
        final UserRepository userRepository = mock(UserRepository.class);

        final User user = mock(User.class);
        when(user.getPassword()).thenReturn("New pwd");
        final User user4 = mock(User.class);
        when(userRepository.findAll()).thenReturn(newSet(user, mock(User.class), mock(User.class),
                mock(User.class), mock(User.class), mock(User.class)));
        when(userRepository.create(any(User.class))).thenReturn(user);
        when(userRepository.findByUsername("createuser1")).thenReturn(of(user));
        when(userRepository.findByUsername("user0")).thenReturn(of(user));
        when(userRepository.findByUsernames(Arrays.asList("user0", "user4"))).thenReturn(new HashSet<>(Arrays.asList(user,user4)));
        when(user.getUsername()).thenReturn("createuser1");
        when(user.getEmail()).thenReturn("createuser1@gravitee.io");

        return userRepository;
    }

    @Bean
    public ViewRepository viewRepository() throws Exception {
        final ViewRepository viewRepository = mock(ViewRepository.class);

        final View view = mock(View.class);
        when(view.getName()).thenReturn("View name");
        when(view.getDescription()).thenReturn("Description for the new view");

        final View view2 = mock(View.class);
        when(view2.getName()).thenReturn("New product");

        final Set<View> views = newSet(view, view2, mock(View.class));
        final Set<View> viewsAfterDelete = newSet(view, view2);
        final Set<View> viewsAfterAdd = newSet(view, view2, mock(View.class), mock(View.class));

        when(viewRepository.findAll()).thenReturn(views, viewsAfterAdd, views, viewsAfterDelete, views);

        when(viewRepository.create(any(View.class))).thenReturn(view);

        when(viewRepository.findById("new-view")).thenReturn(of(view));
        when(viewRepository.findById("products")).thenReturn(of(view2));

        return viewRepository;
    }


    @Bean
    public GroupRepository groupRepository() throws Exception {
        final GroupRepository groupRepository = mock(GroupRepository.class);

        final Group createGroup = new Group();
        createGroup.setId("1");
        createGroup.setAdministrators(Collections.emptyList());
        when(groupRepository.create(any())).thenReturn(createGroup);

        final Group group_application_1 = new Group();
        group_application_1.setId("group-application-1");
        group_application_1.setName("group-application-1");
        group_application_1.setAdministrators(Arrays.asList("user1", "user2"));
        when(groupRepository.findByType(Group.Type.APPLICATION)).thenReturn(Collections.singleton(group_application_1));

        return groupRepository;
    }

    @Bean
    public MembershipRepository membershipRepository() throws Exception {
        final MembershipRepository repo = mock(MembershipRepository.class);

        Membership m1 = mock(Membership.class);
        when(m1.getUserId()).thenReturn("user1");
        when(m1.getReferenceType()).thenReturn(MembershipReferenceType.API);
        when(m1.getType()).thenReturn("OWNER");
        when(m1.getReferenceId()).thenReturn("api1");
        Membership m2 = new Membership("user2", "api2", MembershipReferenceType.API);
        m2.setType("OWNER");
        Membership m3 = new Membership("user3", "api3", MembershipReferenceType.API);
        m3.setType("USER");

        when(repo.findById("user1", MembershipReferenceType.API, "api1"))
                .thenReturn(of(m1));
        when(repo.findByReferenceAndMembershipType(eq(MembershipReferenceType.API), eq("api1"), any()))
                .thenReturn(Collections.singleton(m1));
        when(repo.findByUserAndReferenceType("user1", MembershipReferenceType.API))
                .thenReturn(Collections.singleton(m1));
        when(repo.findByUserAndReferenceTypeAndMembershipType("user1", MembershipReferenceType.API, "OWNER"))
                .thenReturn(Collections.singleton(m1));
        when(repo.findByReferencesAndMembershipType(MembershipReferenceType.API, Arrays.asList("api2", "api3"), null))
                .thenReturn(new HashSet<>(Arrays.asList(m2, m3)));
        when(repo.findByReferencesAndMembershipType(MembershipReferenceType.API, Arrays.asList("api2", "api3"), "OWNER"))
                .thenReturn(new HashSet<>(Collections.singletonList(m2)));

        return repo;
    }
}
