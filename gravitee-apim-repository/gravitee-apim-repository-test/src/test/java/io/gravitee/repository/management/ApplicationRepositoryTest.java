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
package io.gravitee.repository.management;

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static io.gravitee.repository.utils.DateUtils.compareDate;
import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationRepositoryTest extends AbstractManagementRepositoryTest {

    public static final String APP_WITH_LONG_NAME =
        "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012";

    @Override
    protected String getTestCasesPath() {
        return "/data/application-tests/";
    }

    @Test
    public void findAllTest() throws Exception {
        Set<Application> applications = applicationRepository.findAll();

        assertNotNull(applications);
        assertEquals(15, applications.size(), "Fail to resolve application in findAll");
    }

    @Test
    public void findAllByEnvironmentTest() throws Exception {
        Set<Application> applications = applicationRepository.findAllByEnvironment("DEFAULT");

        assertNotNull(applications);
        assertEquals(7, applications.size(), "Fail to resolve application in findAllByEnvironment");
    }

    @Test
    public void findAllArchivedTest() throws Exception {
        Set<Application> applications = applicationRepository.findAll(ApplicationStatus.ARCHIVED);

        assertNotNull(applications);
        assertEquals(3, applications.size(), "Fail to resolve application in findAll with application status");
        assertThat(applications)
            .extracting(Application::getId)
            .containsExactlyInAnyOrder("grouped-app2", "app-with-client-id-archived", "app-with-certificate-archived");
    }

    @Test
    public void createTest() throws Exception {
        String name = "created-app";

        Application application = new Application();

        application.setId(name);
        application.setEnvironmentId("DEFAULT");
        application.setName(name);
        application.setDescription("Application description");
        application.setDomain("Application domain");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "app-type");
        application.setMetadata(metadata);
        application.setType(ApplicationType.SIMPLE);
        application.setStatus(ApplicationStatus.ACTIVE);
        application.setCreatedAt(parse("11/02/2016"));
        application.setUpdatedAt(parse("12/02/2016"));
        application.setDisableMembershipNotifications(true);

        applicationRepository.create(application);

        Optional<Application> optional = applicationRepository.findById(name);

        assertNotNull(optional);
        assertTrue(optional.isPresent(), "Application saved not found");

        Application appSaved = optional.get();

        assertEquals(application.getEnvironmentId(), appSaved.getEnvironmentId(), "Invalid environment id.");
        assertEquals(application.getName(), appSaved.getName(), "Invalid application name.");
        assertEquals(application.getDescription(), appSaved.getDescription(), "Invalid application description.");
        assertEquals(application.getDomain(), appSaved.getDomain(), "Invalid application domain.");
        assertEquals(application.getStatus(), appSaved.getStatus(), "Invalid application status.");
        assertTrue(compareDate(application.getCreatedAt(), appSaved.getCreatedAt()), "Invalid application createdAt.");
        assertTrue(compareDate(application.getUpdatedAt(), appSaved.getUpdatedAt()), "Invalid application updateAt.");
        assertEquals(application.getMetadata().get("type"), appSaved.getMetadata().get("type"), "Invalid application metadata.");
        assertTrue(appSaved.isDisableMembershipNotifications(), "Invalid application disable membership notifications");
    }

    @Test
    public void updateTest() throws Exception {
        String applicationName = "updated-app";

        Application application = new Application();
        application.setId(applicationName);
        application.setEnvironmentId("new_DEFAULT");
        application.setName(applicationName);
        application.setDescription("Updated description");
        application.setDomain("Updated domain");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("type", "update-type");
        metadata.put("client_certificate", null);
        application.setMetadata(metadata);
        application.setPicture("New picture");
        application.setBackground("New background");
        application.setStatus(ApplicationStatus.ARCHIVED);
        application.setType(ApplicationType.SIMPLE);
        application.setCreatedAt(parse("11/02/2016"));
        application.setUpdatedAt(parse("22/02/2016"));
        application.setDisableMembershipNotifications(true);

        applicationRepository.update(application);

        Optional<Application> optional = applicationRepository.findById(applicationName);
        assertTrue(optional.isPresent(), "Application updated not found");

        Application appUpdated = optional.get();

        assertEquals(application.getEnvironmentId(), appUpdated.getEnvironmentId(), "Invalid updated environment id.");
        assertEquals(application.getName(), appUpdated.getName(), "Invalid updated application name.");
        assertEquals(application.getDescription(), appUpdated.getDescription(), "Invalid updated application description.");
        assertEquals(application.getDomain(), appUpdated.getDomain(), "Invalid updated application domain.");
        assertEquals(application.getStatus(), appUpdated.getStatus(), "Invalid updated application status.");
        assertTrue(compareDate(application.getCreatedAt(), appUpdated.getCreatedAt()), "Invalid updated application createdAt.");
        assertTrue(compareDate(application.getUpdatedAt(), appUpdated.getUpdatedAt()), "Invalid updated application updateAt.");
        assertEquals(application.getMetadata().get("type"), appUpdated.getMetadata().get("type"), "Invalid application metadata.");
        assertTrue(appUpdated.isDisableMembershipNotifications(), "Invalid application disable membership notifications");
        assertEquals(application.getPicture(), appUpdated.getPicture(), "Invalid updated application picture.");
        assertEquals(application.getBackground(), appUpdated.getBackground(), "Invalid updated application background.");
    }

    @Test
    public void deleteTest() throws Exception {
        String applicationName = "deleted-app";

        int nbApplicationBefore = applicationRepository.findAll().size();
        applicationRepository.delete(applicationName);

        Optional<Application> optional = applicationRepository.findById(applicationName);
        int nbApplicationAfter = applicationRepository.findAll().size();

        assertFalse(optional.isPresent(), "Deleted application always present");
        assertEquals(nbApplicationBefore - 1, nbApplicationAfter, "Invalid number of applications after deletion");
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Application> optional = applicationRepository.findById("application-sample");
        assertTrue(optional.isPresent(), "Find application by name return no result ");
        assertEquals(1, optional.get().getMetadata().size());
    }

    @Test
    public void findByIdsTest() throws Exception {
        Set<Application> apps = applicationRepository.findByIds(Arrays.asList("searched-app1", "searched-app2"));
        assertNotNull(apps);
        assertEquals(2, apps.size());
        assertEquals(Arrays.asList("searched-app1", "searched-app2"), apps.stream().map(Application::getId).toList());
    }

    @Test
    public void findByIdsTestOrderByNameDesc() throws Exception {
        Set<Application> apps = applicationRepository.findByIds(
            Arrays.asList("searched-app1", "searched-app2"),
            new SortableBuilder().field("name").setAsc(false).build()
        );
        assertNotNull(apps);
        assertEquals(2, apps.size());
        assertEquals(Arrays.asList("searched-app2", "searched-app1"), apps.stream().map(Application::getId).toList());
    }

    @Test
    public void shouldFindApplicationWithGroup() throws Exception {
        Optional<Application> application = applicationRepository.findById("grouped-app1");
        assertTrue(application.isPresent());
        assertNotNull(application.get().getGroups());
        assertEquals(Collections.singleton("application-group"), application.get().getGroups());
    }

    @Test
    public void shouldFindApplicationByExactName() throws Exception {
        Set<Application> apps = applicationRepository.findByNameAndStatuses("searched-app1");
        assertNotNull(apps);
        assertEquals(1, apps.size());
        assertEquals("searched-app1", apps.iterator().next().getId());
    }

    @Test
    public void shouldNotFindApplicationByName() throws Exception {
        Set<Application> apps = applicationRepository.findByNameAndStatuses("unknowd-app");
        assertNotNull(apps);
        assertEquals(0, apps.size());
    }

    @Test
    public void shouldFindApplicationByPartialName() throws Exception {
        Set<Application> apps = applicationRepository.findByNameAndStatuses("arched");
        assertNotNull(apps);
        assertEquals(2, apps.size());
    }

    @Test
    public void shouldFindApplicationByPartialNameIgnoreCase() throws Exception {
        Set<Application> apps = applicationRepository.findByNameAndStatuses("aRcHEd");
        assertNotNull(apps);
        assertEquals(2, apps.size());
    }

    @Test
    public void shouldFindApplicationByPartialNameAndActiveStatus() throws Exception {
        Set<Application> apps = applicationRepository.findByNameAndStatuses("aRcHEd", ApplicationStatus.ARCHIVED);
        assertNotNull(apps);
        assertEquals(0, apps.size());
    }

    @Test
    public void shouldNotFindApplicationByPartialNameAndArchivedStatus() throws Exception {
        Set<Application> apps = applicationRepository.findByNameAndStatuses("aRcHEd", ApplicationStatus.ACTIVE);
        assertNotNull(apps);
        assertEquals(2, apps.size());
    }

    @Test
    public void findByName_shouldReadApiKeyMode() throws Exception {
        Set<Application> apps = applicationRepository.findByNameAndStatuses("searched-app1");
        assertEquals(1, apps.size());
        assertEquals(ApiKeyMode.SHARED, apps.iterator().next().getApiKeyMode());
    }

    @Test
    public void shouldFindByGroups() throws Exception {
        Set<Application> apps = applicationRepository.findByGroups(Collections.singletonList("application-group"));

        assertNotNull(apps);
        assertEquals(2, apps.size());
    }

    @Test
    public void shouldFindByGroupsAndStatus() throws Exception {
        Set<Application> apps = applicationRepository.findByGroups(
            Collections.singletonList("application-group"),
            ApplicationStatus.ARCHIVED
        );

        assertNotNull(apps);
        assertEquals(1, apps.size());
        assertEquals("grouped-app2", apps.iterator().next().getId());
    }

    @Test
    public void shouldFindByEmptyGroups() throws Exception {
        Set<Application> apps = applicationRepository.findByGroups(emptyList());

        assertNotNull(apps);
        assertTrue(apps.isEmpty());
    }

    @Test
    public void shouldFindByIds() throws Exception {
        Set<Application> apps = applicationRepository.findByIds(Arrays.asList("application-sample", "updated-app", "unknown"));

        assertNotNull(apps);
        assertFalse(apps.isEmpty());
        assertEquals(2, apps.size());
        assertTrue(apps.stream().map(Application::getId).toList().containsAll(Arrays.asList("application-sample", "updated-app")));
    }

    @Test
    public void shouldNotUpdateUnknownApplication() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Application unknownApplication = new Application();
            unknownApplication.setId("unknown");
            applicationRepository.update(unknownApplication);
            fail("An unknown application should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            applicationRepository.update(null);
            fail("A null application should not be updated");
        });
    }

    @Test
    public void shouldSearchByName() throws Exception {
        final Page<Application> appsPage = applicationRepository.search(ApplicationCriteria.builder().name("dElETed-a").build(), null);
        final List<Application> apps = appsPage.getContent();

        assertEquals(1, apps.size());
        assertTrue(apps.stream().map(Application::getId).toList().contains("deleted-app"));
    }

    @Test
    public void shouldSearchByQuery_matchesName() throws Exception {
        final Page<Application> appsPage = applicationRepository.search(
            ApplicationCriteria.builder().query("Application test query").build(),
            null
        );
        final List<Application> apps = appsPage.getContent();

        assertEquals(2, apps.size());
        assertTrue(
            apps
                .stream()
                .map(Application::getId)
                .toList()
                .containsAll(List.of("dbc12b15-e975-4fa1-812b-15e975bfa13c", "74e34cc3-000f-492e-a34c-c3000f192e32"))
        );
    }

    @Test
    public void shouldSearchByQueryAndNameAndRestrictedIds() throws Exception {
        final Page<Application> appsPage = applicationRepository.search(
            ApplicationCriteria.builder()
                .query("app")
                .name("Application test")
                .restrictedToIds(Set.of("app-with-long-name", "dbc12b15-e975-4fa1-812b-15e975bfa13c"))
                .build(),
            null
        );
        final List<Application> apps = appsPage.getContent();

        assertEquals(1, apps.size());
        assertTrue(apps.stream().map(Application::getId).toList().contains("dbc12b15-e975-4fa1-812b-15e975bfa13c"));
    }

    @Test
    public void shouldSearchByEnvironmentIds() throws Exception {
        final Page<Application> appsPage = applicationRepository.search(
            ApplicationCriteria.builder().environmentIds(Set.of("DEV", "TEST", "PROD")).build(),
            null
        );

        final List<Application> apps = appsPage.getContent();

        assertThat(apps)
            .map(Application::getName)
            .containsExactlyInAnyOrder(
                APP_WITH_LONG_NAME,
                "Application test query 1",
                "Application test query 2",
                "app-with-client-id",
                "app-with-client-id-archived",
                "app-with-long-client-id",
                "searched-app1",
                "searched-app2"
            );
    }

    @Test
    public void shouldSearchByEnvironmentIdsWithSort() throws Exception {
        Sortable sortable = new SortableBuilder().field("updated_at").order(Order.ASC).build();
        final Page<Application> appsPage = applicationRepository.search(
            ApplicationCriteria.builder().environmentIds(Set.of("DEV", "TEST", "PROD")).build(),
            null,
            sortable
        );

        final List<Application> apps = appsPage.getContent();

        assertNotNull(apps);
        assertFalse(apps.isEmpty());
        assertEquals(8, apps.size());
        List<String> names = apps.stream().map(Application::getName).toList();

        assertEquals(
            List.of(
                "searched-app1",
                "searched-app2",
                "app-with-client-id",
                "app-with-client-id-archived",
                "app-with-long-client-id",
                APP_WITH_LONG_NAME,
                "Application test query 1",
                "Application test query 2"
            ),
            names
        );
    }

    @Test
    public void shouldSearchWithPagination() throws Exception {
        Pageable pageable = new PageableBuilder().pageSize(1).pageNumber(2).build();
        final Page<Application> appsPage = applicationRepository.search(
            ApplicationCriteria.builder().environmentIds(Set.of("DEV", "TEST", "PROD")).build(),
            pageable
        );

        final List<Application> apps = appsPage.getContent();

        assertNotNull(apps);
        assertFalse(apps.isEmpty());
        assertEquals(1, apps.size());
        assertEquals(2, appsPage.getPageNumber());
        assertEquals(1, appsPage.getPageElements());
        assertEquals(8, appsPage.getTotalElements());
    }

    @Test
    public void shouldSearchByGroups() throws Exception {
        final Page<Application> appsPage = applicationRepository.search(
            ApplicationCriteria.builder().groups(Set.of("application-group")).build(),
            null
        );

        final List<Application> apps = appsPage.getContent();

        assertNotNull(apps);
        assertFalse(apps.isEmpty());
        assertEquals(2, apps.size());
        assertEquals("grouped-app1", apps.get(0).getName());
        assertEquals("grouped-app2", apps.get(1).getName());
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        final var beforeDeletion = applicationRepository
            .findAll()
            .stream()
            .filter(app -> "DEFAULT".equals(app.getEnvironmentId()))
            .map(Application::getId)
            .toList();

        final var deleted = applicationRepository.deleteByEnvironmentId("DEFAULT");
        final var nbAfterDeletion = applicationRepository
            .findAll()
            .stream()
            .filter(app -> "DEFAULT".equals(app.getEnvironmentId()))
            .count();

        assertEquals(beforeDeletion.size(), deleted.size());
        assertTrue(beforeDeletion.containsAll(deleted));
        assertEquals(0, nbAfterDeletion);
    }

    @Test
    public void should_return_true_on_existing_client_id_in_env() {
        assertTrue(applicationRepository.existsMetadataEntryForEnv(METADATA_CLIENT_ID, "my-client-id", "PROD"));
    }

    @Test
    public void should_return_false_on_existing_client_id_with_archived_app_in_env() {
        assertFalse(applicationRepository.existsMetadataEntryForEnv(METADATA_CLIENT_ID, "my-client-id-old", "PROD"));
    }

    @Test
    public void should_return_false_on_existing_client_id_in_different_env() {
        assertFalse(applicationRepository.existsMetadataEntryForEnv(METADATA_CLIENT_ID, "my-client-id", "DEFAULT"));
    }
}
