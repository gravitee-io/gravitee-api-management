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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

public class ApplicationRepositoryTest extends AbstractMongoDBTest {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationRepositoryTest.class);
	
	@Autowired
	private ApplicationRepository applicationRepository;

    @Override
    protected String getTestCasesPath() {
        return "/data/application-tests/";
    }
    
	@Test
	public void findAllTest() {
		try {
			Set<Application> applications = applicationRepository.findAll();
			
			Assert.assertNotNull(applications);
			Assert.assertEquals("Fail to resolve application in findAll", 8, applications.size());
			
		} catch (Exception e) {
			logger.error("Error while finding all application",e);
			Assert.fail("Error while finding all application");
		}
	}

	@Test
	public void createTest() {
		try {
			String name="created-app";
					
			Application application = new Application();
			
			application.setName(name);
			application.setDescription("Application description");
			application.setType("type");
			application.setCreatedAt(new Date());
			application.setUpdatedAt(new Date());
			
			applicationRepository.create(application);
			
			Optional<Application> optional = applicationRepository.findById(name);
			
			Assert.assertNotNull(optional);
			Assert.assertTrue("Application saved not found", optional.isPresent());
			
			Application appSaved = optional.get();
			
			Assert.assertEquals("Invalid application name.", 		application.getName(), 			appSaved.getName());
			Assert.assertEquals("Invalid application description.",	application.getDescription(), 	appSaved.getDescription());
			Assert.assertEquals("Invalid application type.", 		application.getType(), 			appSaved.getType());
			Assert.assertEquals("Invalid application createdAt.", 	application.getCreatedAt(), 	appSaved.getCreatedAt());
			Assert.assertEquals("Invalid application updateAt.", 	application.getUpdatedAt(), 	appSaved.getUpdatedAt());

			
		} catch (Exception e) {
			logger.error("Error while calling findById", e);
			Assert.fail("Error while calling findById");
		}
	}

	@Test
	public void updateTest() {
		try {
			String applicationName="updated-app";
			
			Application application = new Application();
			application.setName(applicationName);
			application.setDescription("Updated description");
			application.setType("update-type");
			application.setUpdatedAt(new Date());
			
			applicationRepository.update(application);
			
			
			Optional<Application> optionnal = applicationRepository.findById(applicationName);
			Assert.assertTrue("Application updated not found", optionnal.isPresent());
		
			Application appUpdated = optionnal.get();
			
			Assert.assertEquals("Invalid updated application name.", 		application.getName(), 			appUpdated.getName());
			Assert.assertEquals("Invalid updated application description.",	application.getDescription(), 	appUpdated.getDescription());
			Assert.assertEquals("Invalid updated application type.", 		application.getType(), 			appUpdated.getType());
			Assert.assertEquals("Invalid updated application updateAt.", 	application.getUpdatedAt(), 	appUpdated.getUpdatedAt());
			//Check invariant field
			Assert.assertNotEquals("Invalid updated application createdAt.",application.getCreatedAt(), 	appUpdated.getCreatedAt());
		} catch (Exception e) {
			logger.error("Error while calling updating application", e);
			Assert.fail("Error while calling updating application");		
		}
	}
	
	@Test
	public void deleteTest() {
		try {
			String applicationName = "deleted-app";
			
			int nbApplicationBefore = applicationRepository.findAll().size();
			applicationRepository.delete(applicationName);
			
			Optional<Application> optional = applicationRepository.findById(applicationName);
			int nbApplicationAfter = applicationRepository.findAll().size();
			
			Assert.assertFalse("Deleted application always present", optional.isPresent());
			Assert.assertEquals("Invalid number of applications after deletion", nbApplicationBefore -1, nbApplicationAfter);
		} catch(Exception e) {
			logger.error("Error while calling delete application", e);
			Assert.fail("Error while calling delete application");		
		}
	}

	@Test
	public void findByNameTest() {
		try {
			Optional<Application> optional = applicationRepository.findById("findByNameOk");
			Assert.assertTrue("Find application by name return no result ", optional.isPresent());
		} catch (Exception e) {
			logger.error("Error while calling findById", e);
			Assert.fail("Error while calling findById");
		}
	}
	
	@Test
	public void findByUserTest() {
		try {
			Set<Application> applications = applicationRepository.findByUser("findByUserTest");
			Assert.assertNotNull(applications);
			Assert.assertEquals("Invalid application result in findByUser",applications.size(), 1);
		} catch (Exception e) {
			logger.error("Error while finding application by user",e);
			Assert.fail("Error while finding application by user");
		}
	}
	
	@Test
	public void countByUserTest(){
		try {
			int nbApplications = applicationRepository.countByUser("findByUserTest");
			Assert.assertEquals("Invalid application result in countByUser", nbApplications, 1);
		} catch (Exception e) {
			logger.error("Error while counting application by user", e);
			Assert.fail("Error while counting application by user");
		}
	}

}
