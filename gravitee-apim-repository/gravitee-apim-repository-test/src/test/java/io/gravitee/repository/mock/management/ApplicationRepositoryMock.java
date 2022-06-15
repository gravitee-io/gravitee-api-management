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
package io.gravitee.repository.mock.management;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationRepositoryMock extends AbstractRepositoryMock<ApplicationRepository> {

    public ApplicationRepositoryMock() {
        super(ApplicationRepository.class);
    }

    @Override
    protected void prepare(ApplicationRepository applicationRepository) throws Exception {
        final Application application = mock(Application.class);
        when(application.getId()).thenReturn("application-sample");
        when(application.getEnvironmentId()).thenReturn("DEFAULT");
        when(application.getType()).thenReturn(ApplicationType.SIMPLE);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("client_id", "my-client-id");
        metadata.put("type", "Web");
        when(application.getMetadata()).thenReturn(metadata);
        when(applicationRepository.findById("application-sample")).thenReturn(of(application));

        final Set<Application> allApplications = newSet(
            application,
            mock(Application.class),
            mock(Application.class),
            mock(Application.class),
            mock(Application.class),
            mock(Application.class),
            mock(Application.class),
            mock(Application.class),
            mock(Application.class),
            mock(Application.class)
        );
        final Set<Application> allApplicationsForDefaultEnvironment = newSet(
            application,
            mock(Application.class),
            mock(Application.class),
            mock(Application.class),
            mock(Application.class)
        );

        when(applicationRepository.findAll()).thenReturn(allApplications);
        when(applicationRepository.findAllByEnvironment("DEFAULT")).thenReturn(allApplicationsForDefaultEnvironment);

        doAnswer(invocation -> allApplications.remove(application)).when(applicationRepository).delete("deleted-app");
        when(applicationRepository.findById("deleted-app")).thenReturn(empty());

        final Application newApplication = mock(Application.class);
        when(newApplication.getName()).thenReturn("created-app");
        when(newApplication.getEnvironmentId()).thenReturn("DEFAULT");
        when(newApplication.getDescription()).thenReturn("Application description");
        when(newApplication.getDomain()).thenReturn("Application domain");
        when(newApplication.getStatus()).thenReturn(ApplicationStatus.ACTIVE);
        when(newApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(newApplication.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(newApplication.getUpdatedAt()).thenReturn(parse("12/02/2016"));
        when(newApplication.isDisableMembershipNotifications()).thenReturn(true);
        Map<String, String> createMetadata = new HashMap<>();
        createMetadata.put("type", "app-type");
        when(newApplication.getMetadata()).thenReturn(createMetadata);

        when(applicationRepository.findById("created-app")).thenReturn(of(newApplication));

        final Application updatedApplication = mock(Application.class);
        when(updatedApplication.getId()).thenReturn("updated-app");
        when(updatedApplication.getEnvironmentId()).thenReturn("new_DEFAULT");
        when(updatedApplication.getName()).thenReturn("updated-app");
        when(updatedApplication.getDescription()).thenReturn("Updated description");
        when(updatedApplication.getDomain()).thenReturn("Updated domain");
        when(updatedApplication.getPicture()).thenReturn("New picture");
        when(updatedApplication.getBackground()).thenReturn("New background");
        when(updatedApplication.getStatus()).thenReturn(ApplicationStatus.ARCHIVED);
        when(updatedApplication.getType()).thenReturn(ApplicationType.SIMPLE);
        when(updatedApplication.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(updatedApplication.getUpdatedAt()).thenReturn(parse("22/02/2016"));
        when(updatedApplication.isDisableMembershipNotifications()).thenReturn(true);
        Map<String, String> updateMetadata = new HashMap<>();
        updateMetadata.put("type", "update-type");
        when(updatedApplication.getMetadata()).thenReturn(updateMetadata);

        when(applicationRepository.findById("updated-app")).thenReturn(of(updatedApplication));

        final Application groupedApplication1 = mock(Application.class);
        when(groupedApplication1.getId()).thenReturn("grouped-app1");
        when(groupedApplication1.getGroups()).thenReturn(singleton("application-group"));
        when(applicationRepository.findById("grouped-app1")).thenReturn(of(groupedApplication1));

        final Application groupedApplication2 = mock(Application.class);
        when(groupedApplication2.getId()).thenReturn("grouped-app2");
        when(groupedApplication2.getGroups()).thenReturn(singleton("application-group"));
        when(applicationRepository.findById("grouped-app2")).thenReturn(of(groupedApplication2));

        final Set<Application> allArchivedApplications = newSet(groupedApplication2);
        when(applicationRepository.findAll(ApplicationStatus.ARCHIVED)).thenReturn(allArchivedApplications);

        final Application searchedApp1 = mock(Application.class);
        final Application searchedApp2 = mock(Application.class);
        when(searchedApp1.getId()).thenReturn("searched-app1");
        when(searchedApp1.getName()).thenReturn("searched-app1");
        when(searchedApp1.getApiKeyMode()).thenReturn(ApiKeyMode.SHARED);
        when(searchedApp2.getId()).thenReturn("searched-app2");
        when(searchedApp2.getName()).thenReturn("searched-app2");
        when(applicationRepository.findByNameAndStatuses("searched-app1")).thenReturn(singleton(searchedApp1));
        when(applicationRepository.findByNameAndStatuses("arched")).thenReturn(newSet(searchedApp1, searchedApp2));
        when(applicationRepository.findByNameAndStatuses("aRcHEd")).thenReturn(newSet(searchedApp1, searchedApp2));
        when(applicationRepository.findByNameAndStatuses("aRcHEd", ApplicationStatus.ACTIVE))
            .thenReturn(newSet(searchedApp1, searchedApp2));
        when(applicationRepository.findByNameAndStatuses("aRcHEd", ApplicationStatus.ARCHIVED)).thenReturn(emptySet());

        when(applicationRepository.findByIds(asList("searched-app1", "searched-app2"))).thenReturn(newSet(searchedApp1, searchedApp2));
        when(
            applicationRepository.findByIds(
                asList("searched-app1", "searched-app2"),
                new SortableBuilder().field("name").setAsc(false).build()
            )
        )
            .thenReturn(newSet(searchedApp2, searchedApp1));

        when(applicationRepository.findByGroups(singletonList("application-group")))
            .thenReturn(newSet(groupedApplication1, groupedApplication2));
        when(applicationRepository.findByGroups(singletonList("application-group"), ApplicationStatus.ARCHIVED))
            .thenReturn(newSet(groupedApplication2));
        when(applicationRepository.findByGroups(emptyList())).thenReturn(emptySet());

        when(applicationRepository.findByIds(asList("application-sample", "updated-app", "unknown")))
            .thenReturn(new HashSet<>(asList(application, updatedApplication)));

        when(applicationRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
        when(
            applicationRepository.search(
                new ApplicationCriteria.Builder()
                    .name("SeArched-app")
                    .ids("searched-app1", "app-with-long-client-id", "app-with-long-name")
                    .status(ApplicationStatus.ACTIVE)
                    .environmentIds(singletonList("DEV"))
                    .build(),
                null
            )
        )
            .thenReturn(new Page<>(singletonList(searchedApp1), 1, 1, 1));
        when(applicationRepository.search(new ApplicationCriteria.Builder().environmentIds(asList("DEV", "TEST", "PROD")).build(), null))
            .thenReturn(
                new Page<>(
                    asList(
                        mock(Application.class),
                        mock(Application.class),
                        mock(Application.class),
                        mock(Application.class),
                        mock(Application.class)
                    ),
                    1,
                    5,
                    5
                )
            );
    }
}
