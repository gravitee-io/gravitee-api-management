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
import io.gravitee.repository.management.model.MembershipType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static io.gravitee.repository.utils.DateUtils.parse;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class ApplicationRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/application-tests/";
    }

    @Test
    public void findAllTest() throws Exception {
        Set<Application> applications = applicationRepository.findAll();

        Assert.assertNotNull(applications);
        Assert.assertEquals("Fail to resolve application in findAll", 3, applications.size());
    }

    @Test
    public void createTest() throws Exception {
        String name = "created-app";

        Application application = new Application();

        application.setId(name);
        application.setName(name);
        application.setDescription("Application description");
        application.setType("type");
        application.setCreatedAt(parse("11/02/2016"));
        application.setUpdatedAt(parse("12/02/2016"));

        applicationRepository.create(application);

        Optional<Application> optional = applicationRepository.findById(name);

        Assert.assertNotNull(optional);
        Assert.assertTrue("Application saved not found", optional.isPresent());

        Application appSaved = optional.get();

        Assert.assertEquals("Invalid application name.", application.getName(), appSaved.getName());
        Assert.assertEquals("Invalid application description.", application.getDescription(), appSaved.getDescription());
        Assert.assertEquals("Invalid application type.", application.getType(), appSaved.getType());
        Assert.assertEquals("Invalid application createdAt.", application.getCreatedAt(), appSaved.getCreatedAt());
        Assert.assertEquals("Invalid application updateAt.", application.getUpdatedAt(), appSaved.getUpdatedAt());
    }

    @Test
    public void updateTest() throws Exception {
        String applicationName = "updated-app";

        Application application = new Application();
        application.setId(applicationName);
        application.setName(applicationName);
        application.setDescription("Updated description");
        application.setType("update-type");
        application.setCreatedAt(parse("11/02/2016"));
        application.setUpdatedAt(parse("22/02/2016"));

        applicationRepository.update(application);

        Optional<Application> optional = applicationRepository.findById(applicationName);
        Assert.assertTrue("Application updated not found", optional.isPresent());

        Application appUpdated = optional.get();

        Assert.assertEquals("Invalid updated application name.", application.getName(), appUpdated.getName());
        Assert.assertEquals("Invalid updated application description.", application.getDescription(), appUpdated.getDescription());
        Assert.assertEquals("Invalid updated application type.", application.getType(), appUpdated.getType());
        Assert.assertEquals("Invalid updated application updateAt.", application.getUpdatedAt(), appUpdated.getUpdatedAt());
        //Check invariant field
        Assert.assertNotEquals("Invalid updated application createdAt.", application.getCreatedAt(), appUpdated.getCreatedAt());
    }

    @Test
    public void deleteTest() throws Exception {
        String applicationName = "deleted-app";

        int nbApplicationBefore = applicationRepository.findAll().size();
        applicationRepository.delete(applicationName);

        Optional<Application> optional = applicationRepository.findById(applicationName);
        int nbApplicationAfter = applicationRepository.findAll().size();

        Assert.assertFalse("Deleted application always present", optional.isPresent());
        Assert.assertEquals("Invalid number of applications after deletion", nbApplicationBefore - 1, nbApplicationAfter);
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Application> optional = applicationRepository.findById("application-sample");
        Assert.assertTrue("Find application by name return no result ", optional.isPresent());
    }

    @Test
    public void findByUserTest() throws Exception {
        Set<Application> applications = applicationRepository.findByUser("user", null);
        Assert.assertNotNull(applications);
        Assert.assertEquals("Invalid application result for user", 3, applications.size());
    }

    @Test
    public void findPrimaryOwnerMemberByUserTest() throws Exception {
        Set<Application> applications = applicationRepository.findByUser("po", MembershipType.PRIMARY_OWNER);
        Assert.assertNotNull(applications);
        Assert.assertEquals("Invalid application result for primary owner", 3, applications.size());
    }

    @Test
    public void findUserMemberByUserTest() throws Exception {
        Set<Application> applications = applicationRepository.findByUser("po", MembershipType.USER);
        Assert.assertNotNull(applications);
        Assert.assertEquals("Invalid application result for user", 0, applications.size());
    }
}
