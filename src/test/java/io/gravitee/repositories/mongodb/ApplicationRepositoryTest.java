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
package io.gravitee.repositories.mongodb;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repository.api.ApplicationRepository;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.OwnerType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestRepositoryConfiguration.class })
public class ApplicationRepositoryTest extends AbstractMongoDBTest {

	private static final String TESTCASES_PATH = "/data/application-tests/";
	
	private static final int NB_SAMPLE_APPLICATION_TESTCASES = 8; 
	
	private Logger logger = LoggerFactory.getLogger(ApplicationRepositoryTest.class);	
	
	@Autowired
	private ApplicationRepository applicationRepository;

    @Override
    protected String getTestCasesPath() {
        return TESTCASES_PATH;
    }
    
	@Test
	public void findAllTest() {
		try{
			Set<Application> applications = applicationRepository.findAll();
			
			Assert.assertNotNull(applications);
			Assert.assertEquals("Fail to resolve application in findAll", NB_SAMPLE_APPLICATION_TESTCASES, applications.size());
			
		}catch(Exception e){
			logger.error("Error while finding all application",e);
			Assert.fail("Error while finding all application");
		}
	}


	@Test
	public void createTest() {
		try{
			
			String name="created-app";
					
			Application application = new Application();
			
			application.setName(name);
			application.setDescription("Application description");
			application.setCreator("creator");
			application.setOwnerType(OwnerType.USER);
			application.setOwner("creator");
			application.setType("type");
			application.setCreatedAt(new Date());
			application.setUpdatedAt(new Date());
			
			applicationRepository.create(application);
			
			Optional<Application> optionnal = applicationRepository.findByName(name);
			
			Assert.assertNotNull(optionnal);
			Assert.assertTrue("Application saved not found", optionnal.isPresent());
			
			Application appSaved = optionnal.get();
			
			Assert.assertEquals("Invalid application name.", 		application.getName(), 			appSaved.getName());
			Assert.assertEquals("Invalid application description.",	application.getDescription(), 	appSaved.getDescription());
			Assert.assertEquals("Invalid application type.", 		application.getType(), 			appSaved.getType());
			Assert.assertEquals("Invalid application creator.", 	application.getCreator(), 		appSaved.getCreator());
			Assert.assertEquals("Invalid application createdAt.", 	application.getCreatedAt(), 	appSaved.getCreatedAt());
			Assert.assertEquals("Invalid application updateAt.", 	application.getUpdatedAt(), 	appSaved.getUpdatedAt());
			Assert.assertEquals("Invalid application Owner.", 		application.getOwner(), 		appSaved.getOwner());
			Assert.assertEquals("Invalid application OwnerType.", 	application.getOwnerType(), 	appSaved.getOwnerType());

			
		}catch(Exception e){
			logger.error("Error while calling findByName", e);
			Assert.fail("Error while calling findByName");		
		}
	}

	@Test
	public void updateTest() {
		try{
			String applicationName="updated-app";
			
			Application application = new Application();
			application.setName(applicationName);
			application.setCreator("updater");
			application.setDescription("Updated description");
			//application.setName(name);
			application.setOwner("updater");
			application.setOwnerType(OwnerType.USER);
			application.setType("update-type");
			application.setUpdatedAt(new Date());
			
			applicationRepository.update(application);
			
			
			Optional<Application> optionnal = applicationRepository.findByName(applicationName);
			Assert.assertTrue("Application updated not found", optionnal.isPresent());
		
			Application appUpdated = optionnal.get();
			
			Assert.assertEquals("Invalid updated application name.", 		application.getName(), 			appUpdated.getName());
			Assert.assertEquals("Invalid updated application description.",	application.getDescription(), 	appUpdated.getDescription());
			Assert.assertEquals("Invalid updated application type.", 		application.getType(), 			appUpdated.getType());
			Assert.assertEquals("Invalid updated application updateAt.", 	application.getUpdatedAt(), 	appUpdated.getUpdatedAt());
			Assert.assertEquals("Invalid updated application Owner.", 		application.getOwner(), 		appUpdated.getOwner());
			Assert.assertEquals("Invalid updated application OwnerType.", 	application.getOwnerType(), 	appUpdated.getOwnerType());
			//Check invariant field
			Assert.assertNotEquals("Invalid updated application creator.", 	application.getCreator(), 		appUpdated.getCreator());
			Assert.assertNotEquals("Invalid updated application createdAt.",application.getCreatedAt(), 	appUpdated.getCreatedAt());

			
		}catch(Exception e){
			logger.error("Error while calling updating application", e);
			Assert.fail("Error while calling updating application");		
		}
	}
	
	@Test
	public void deleteTest() {
		try{
			String applicationName = "deleted-app";
			
			int nbApplicationBefore = applicationRepository.findAll().size();
			applicationRepository.delete(applicationName);
			
			Optional<Application> optional = applicationRepository.findByName(applicationName);
			int nbApplicationAfter = applicationRepository.findAll().size();
			
			Assert.assertFalse("Deleted application always present", optional.isPresent());
			Assert.assertEquals("Invalid number of applications after deletion", nbApplicationBefore -1, nbApplicationAfter);

			
		}catch(Exception e){
			logger.error("Error while calling delete application", e);
			Assert.fail("Error while calling delete application");		
		}
	}


	@Test
	public void findByNameTest() {
		try{
			Optional<Application> optional = applicationRepository.findByName("findByNameOk");
			Assert.assertTrue("Find application by name return no result ", optional.isPresent());
		}catch(Exception e){
			logger.error("Error while calling findByName", e);
			Assert.fail("Error while calling findByName");		
		}
	}

	@Test
	public void findByTeamTest() {
		try{
			Set<Application> applications = applicationRepository.findByTeam("findByTeamTest");
			Assert.assertNotNull(applications);
			Assert.assertEquals("Invalid application result in findByTeam",applications.size(), 2);
			
		}catch(Exception e){
			logger.error("Error while finding application by team",e);
			Assert.fail("Error while finding application by team");
		}
	}
	
	@Test
	public void findByUserTest() {
		try{
			
			Set<Application> applications = applicationRepository.findByUser("findByUserTest");
			Assert.assertNotNull(applications);
			Assert.assertEquals("Invalid application result in findByUser",applications.size(), 1);

		}catch(Exception e){
			logger.error("Error while finding application by user",e);
			Assert.fail("Error while finding application by user");
		}
	}	


	@Test
	public void countByTeamTest(){
		try{
			
			int nbApplications = applicationRepository.countByTeam("findByTeamTest");
			Assert.assertEquals("Invalid application result in countByTeam", nbApplications, 2);
	
		}catch(Exception e){
			logger.error("Error while counting application by team",e);
			Assert.fail("Error while counting application by team");
		}
	}
	
	@Test
	public void countByUserTest(){
		try{
			
			int nbApplications = applicationRepository.countByUser("findByUserTest");
			Assert.assertEquals("Invalid application result in countByUser", nbApplications, 1);
		
		}catch(Exception e){
			logger.error("Error while counting application by user",e);
			Assert.fail("Error while counting application by user");
		}
	}

}
