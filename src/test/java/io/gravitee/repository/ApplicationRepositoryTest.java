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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.gravitee.repository.utils.DateUtils.parse;
import static org.junit.Assert.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/application-tests/";
    }

    @Test
    public void findAllTest() throws Exception {
        Set<Application> applications = applicationRepository.findAll();

        assertNotNull(applications);
        assertEquals("Fail to resolve application in findAll", 7, applications.size());
    }

    @Test
    public void findAllArchivedTest() throws Exception {
        Set<Application> applications = applicationRepository.findAll(ApplicationStatus.ARCHIVED);

        assertNotNull(applications);
        assertEquals("Fail to resolve application in findAll with application status", 1, applications.size());
        assertEquals("grouped-app2", applications.iterator().next().getId());
    }

    @Test
    public void createTest() throws Exception {
        String name = "created-app";

        Application application = new Application();

        application.setId(name);
        application.setName(name);
        application.setDescription("Application description");
        application.setType("type");
        application.setStatus(ApplicationStatus.ACTIVE);
        application.setCreatedAt(parse("11/02/2016"));
        application.setUpdatedAt(parse("12/02/2016"));

        applicationRepository.create(application);

        Optional<Application> optional = applicationRepository.findById(name);

        assertNotNull(optional);
        assertTrue("Application saved not found", optional.isPresent());

        Application appSaved = optional.get();

        assertEquals("Invalid application name.", application.getName(), appSaved.getName());
        assertEquals("Invalid application description.", application.getDescription(), appSaved.getDescription());
        assertEquals("Invalid application type.", application.getType(), appSaved.getType());
        assertEquals("Invalid application status.", application.getStatus(), appSaved.getStatus());
        assertEquals("Invalid application createdAt.", application.getCreatedAt(), appSaved.getCreatedAt());
        assertEquals("Invalid application updateAt.", application.getUpdatedAt(), appSaved.getUpdatedAt());
    }

    @Test
    public void updateTest() throws Exception {
        String applicationName = "updated-app";

        Application application = new Application();
        application.setId(applicationName);
        application.setName(applicationName);
        application.setDescription("Updated description");
        application.setType("update-type");
        application.setStatus(ApplicationStatus.ARCHIVED);
        application.setCreatedAt(parse("11/02/2016"));
        application.setUpdatedAt(parse("22/02/2016"));

        applicationRepository.update(application);

        Optional<Application> optional = applicationRepository.findById(applicationName);
        assertTrue("Application updated not found", optional.isPresent());

        Application appUpdated = optional.get();

        assertEquals("Invalid updated application name.", application.getName(), appUpdated.getName());
        assertEquals("Invalid updated application description.", application.getDescription(), appUpdated.getDescription());
        assertEquals("Invalid updated application type.", application.getType(), appUpdated.getType());
        assertEquals("Invalid updated application status.", application.getStatus(), appUpdated.getStatus());
        assertEquals("Invalid updated application createdAt.", application.getCreatedAt(), appUpdated.getCreatedAt());
        assertEquals("Invalid updated application updateAt.", application.getUpdatedAt(), appUpdated.getUpdatedAt());
    }

    @Test
    public void deleteTest() throws Exception {
        String applicationName = "deleted-app";

        int nbApplicationBefore = applicationRepository.findAll().size();
        applicationRepository.delete(applicationName);

        Optional<Application> optional = applicationRepository.findById(applicationName);
        int nbApplicationAfter = applicationRepository.findAll().size();

        assertFalse("Deleted application always present", optional.isPresent());
        assertEquals("Invalid number of applications after deletion", nbApplicationBefore - 1, nbApplicationAfter);
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Application> optional = applicationRepository.findById("application-sample");
        assertTrue("Find application by name return no result ", optional.isPresent());
    }

    @Test
    public void findByIdsTest() throws Exception {
        Set<Application> apps = applicationRepository.findByIds(Arrays.asList("searched-app1", "searched-app2"));
        assertNotNull(apps);
        assertEquals(2, apps.size());
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
        Set<Application> apps = applicationRepository.findByName("searched-app1");
        assertNotNull(apps);
        assertEquals(1, apps.size());
        assertEquals("searched-app1", apps.iterator().next().getId());
    }

    @Test
    public void shouldNotFindApplicationByName() throws Exception {
        Set<Application> apps = applicationRepository.findByName("unknowd-app");
        assertNotNull(apps);
        assertEquals(0, apps.size());
    }

    @Test
    public void shouldFindApplicationByPartialName() throws Exception {
        Set<Application> apps = applicationRepository.findByName("arched");
        assertNotNull(apps);
        assertEquals(2, apps.size());
    }

    @Test
    public void shouldFindApplicationByPartialNameIgnoreCase() throws Exception {
        Set<Application> apps = applicationRepository.findByName("aRcHEd");
        assertNotNull(apps);
        assertEquals(2, apps.size());
    }

    @Test
    public void shouldFindByGroups() throws Exception {
        Set<Application> apps = applicationRepository.findByGroups(Arrays.asList("application-group"));

        assertNotNull(apps);
        assertEquals(2, apps.size());
    }

    @Test
    public void shouldFindByGroupsAndStatus() throws Exception {
        Set<Application> apps = applicationRepository.findByGroups(Arrays.asList("application-group"), ApplicationStatus.ARCHIVED);

        assertNotNull(apps);
        assertEquals(1, apps.size());
        assertEquals("grouped-app2", apps.iterator().next().getId());
    }


    @Test
    public void shouldFindByIds() throws Exception {
        Set<Application> apps = applicationRepository.findByIds(Arrays.asList("application-sample", "updated-app", "unknown"));

        assertNotNull(apps);
        assertFalse(apps.isEmpty());
        assertEquals(2, apps.size());
        assertTrue(apps.
                stream().
                map(Application::getId).
                collect(Collectors.toList()).
                containsAll(Arrays.asList("application-sample", "updated-app")));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownApplication() throws Exception {
        Application unknownApplication = new Application();
        unknownApplication.setId("unknown");
        applicationRepository.update(unknownApplication);
        fail("An unknown application should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        applicationRepository.update(null);
        fail("A null application should not be updated");
    }
}
