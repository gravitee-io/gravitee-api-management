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

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.api.UserRepository;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.LifecycleState;
import io.gravitee.repository.model.OwnerType;
import io.gravitee.repository.model.PolicyConfiguration;
import io.gravitee.repository.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryConfiguration.class })
public class ApiRepositoryTest extends AbstractMongoDBTest {

	private static final String TESTCASES_PATH = "/data/api-tests/";

	private static final int NB_APIS_FOR_CREATOR = 4; 
	
	private Logger logger = LoggerFactory.getLogger(ApiRepositoryTest.class);	

	
	@Autowired
	private ApiRepository apiRepository;
	
	@Autowired
	private UserRepository userRepository;

    @Override
    protected String getTestCasesPath() {
        return TESTCASES_PATH;
    }
	
	private User createUser(String userName) throws Exception{
			
		User user = new User();
		user.setUsername(userName);
		user.setEmail(userName+"@itest.test");
		return userRepository.create(user);
	}
	
	@Test
	public void createApiTest() {

		try {

			User owner = createUser("user-"+UUID.randomUUID());

			String apiName = "sample-"+new Date().getTime();
			
			Api api = new Api();
			api.setName(apiName);
			api.setVersion("1");
			api.setLifecycleState(LifecycleState.STOPPED);
			api.setPrivate(true);
			api.setPublicURI(URI.create("/public/sample/"));
			api.setTargetURI(URI.create("/target/sample/"));
			api.setCreatedAt(new Date());
			api.setUpdatedAt(new Date());
			api.setCreator("creator");
			api.setOwner(owner.getUsername());
			api.setOwnerType(OwnerType.USER);
			
			apiRepository.create(api);
					
			Optional<Api> optional = apiRepository.findByName(apiName);
			Assert.assertTrue("Api saved not found", optional.isPresent());
			
			Api apiSaved = optional.get();
			Assert.assertEquals("Invalid saved api version.", 	api.getVersion(), apiSaved.getVersion());
			Assert.assertEquals("Invalid api lifecycle.", 		api.getLifecycleState(), apiSaved.getLifecycleState());
			Assert.assertEquals("Invalid api private status.", 	api.isPrivate(), apiSaved.isPrivate());
			Assert.assertEquals("Invalid api public uri.", 		api.getPublicURI(), apiSaved.getPublicURI());
			Assert.assertEquals("Invalid api target uri.", 		api.getTargetURI(), apiSaved.getTargetURI());
			Assert.assertEquals("Invalid api createdAt.", 		api.getCreatedAt(), apiSaved.getCreatedAt());
			Assert.assertEquals("Invalid api updateAt.", 		api.getUpdatedAt(), apiSaved.getUpdatedAt());
			Assert.assertEquals("Invalid api Owner.", 			api.getOwner(), apiSaved.getOwner());
			Assert.assertEquals("Invalid api OwnerType.", 		api.getOwnerType(), apiSaved.getOwnerType());
			Assert.assertEquals("Invalid api creator.", 		api.getCreator(), apiSaved.getCreator());
			
		} catch (Exception e) {
			logger.error("Error while testing createApi", e);
			Assert.fail("Error while testing createApi");
		}
	}

	@Test
	public void findByNameTest() {
		try{
			Optional<Api> optional = apiRepository.findByName("findByNameOk");
			Assert.assertTrue("Find api by name return no result ", optional.isPresent());
		}catch(Exception e){
			logger.error("Error while calling findByName", e);
			Assert.fail("Error while calling findByName");		
		}
	}
	
	@Test
	public void findByNameMissingTest() {
		try{
		
			Optional<Api> optional = apiRepository.findByName("findByNameMissing");
			Assert.assertFalse("Find api by name on missing api return a result", optional.isPresent());
	
		}catch(Exception e){
			logger.error("Error while calling findByName on missing api", e);
			Assert.fail("Error while calling findByName on missing api");		
		}
	}

	@Test
	public void findByCreatorNameTest() {
		try{
			Set<Api> apis = apiRepository.findByCreator("creator");
			
			Assert.assertNotNull(apis);
			Assert.assertEquals("Invalid number of creator api found",  NB_APIS_FOR_CREATOR, apis.size());
	
		}catch(Exception e){
			logger.error("Error while finding apis by creator",e);
			Assert.fail("Error while finding apis by creator");
		}
	}

	@Test
	public void findByTeamTest() {
		try{
	
			Set<Api> apis = apiRepository.findByTeam("findByTeamTest", false);
			Assert.assertNotNull(apis);
			Assert.assertEquals("Invalid api result in findByTeam",apis.size(), 2);
	
		}catch(Exception e){
			logger.error("Error while finding api by name",e);
			Assert.fail("Error while finding api by name");
		}
	}
	
	@Test
	public void findByUserTest() {
		try{
			
			Set<Api> apis = apiRepository.findByUser("findByUserTest", false);
			Assert.assertNotNull(apis);
			Assert.assertEquals("Invalid api result in findByUser",apis.size(), 2);
			
		}catch(Exception e){
			logger.error("Error while finding api by user",e);
			Assert.fail("Error while finding api by user");
		}
	}	
	
	@Test
	public void findPublicByUserTest() {
		try{
			
			Set<Api> apis = apiRepository.findByUser("findByUserTest", true);
			Assert.assertNotNull(apis);
			Assert.assertEquals("Invalid api result in findByUser",apis.size(), 1);
			
		}catch(Exception e){
			logger.error("Error while finding api by user",e);
			Assert.fail("Error while finding api by user");
		}
	}	
	
	
	@Test
	public void findAllTest() {
		try{
			Set<Api> apis = apiRepository.findAll();
			
			Assert.assertNotNull(apis);
			Assert.assertFalse("Fail to resolve api in findAll", apis.isEmpty());
			
		}catch(Exception e){
			logger.error("Error while finding all apis",e);
			Assert.fail("Error while finding all apise");
		}
	}	

	@Test
	public void countApisByTeamTest(){
		try{
			
			int nbApis = apiRepository.countByTeam("findByTeamTest", false);
			Assert.assertEquals("Invalid api result in countByTeam", nbApis, 2);
			
		}catch(Exception e){
			logger.error("Error while counting api by team",e);
			Assert.fail("Error while counting api by team");
		}
	}
	
	@Test
	public void countApisByUserTest(){
		try{
			
			int nbApis = apiRepository.countByUser("findByUserTest", false);
			Assert.assertEquals("Invalid api result in countByUser", nbApis, 2);
			
		}catch(Exception e){
			logger.error("Error while counting api by user",e);
			Assert.fail("Error while counting api by user");
		}
	}
	
	
	@Test
	public void countPublicApisByUserTest(){
		try{
			
			int nbApis = apiRepository.countByUser("findByUserTest", true);
			Assert.assertEquals("Invalid api result in countByUser", nbApis, 1);
			
		}catch(Exception e){
			logger.error("Error while counting api by user",e);
			Assert.fail("Error while counting api by user");
		}
	}

	@Test
	public void deleteApiTest() {
		try{
			int nbApiBefore = apiRepository.findAll().size();
			apiRepository.delete("findByNameOk");
			int nbApiAfter = apiRepository.findAll().size();
	
			Assert.assertEquals(nbApiBefore -1, nbApiAfter);
			
		}catch(Exception e){
			logger.error("Error while deleting api",e);
			Assert.fail("Error while deleting api");
		}

	}
	
	//@Test
	//TODO test correction
	public void findByApplicationTest(){
		try{
			Set<Api> apis = apiRepository.findByApplication("application-sample");
			Assert.assertNotNull(apis);
			Assert.assertEquals(2, apis.size());
			
		}catch(Exception e){
			logger.error("Error while finding api by application",e);
			Assert.fail("Error while finding api by application");
		}
	}

	@Test
	public void updatePoliciesConfigurationTest() throws Exception {
		
		PolicyConfiguration policyConfiguration = new PolicyConfiguration();
		policyConfiguration.setPolicy("policy1");
		policyConfiguration.setConfiguration("{ 'update:'configuration1' }");
		
		PolicyConfiguration policyConfiguration2 = new PolicyConfiguration();
		policyConfiguration2.setPolicy("policy2");
		policyConfiguration2.setConfiguration("{ 'update:'configuration2' }");
		
		List<PolicyConfiguration> configs = Arrays.asList(policyConfiguration, policyConfiguration2);
			
		apiRepository.updatePoliciesConfiguration("api1", configs);

	}
	
	
	@Test
	public void updatePolicyConfigurationTest() throws Exception {
		try{
			String apiName = "api2";
			PolicyConfiguration policyConfiguration = new PolicyConfiguration();
			policyConfiguration.setPolicy("policy1");
			policyConfiguration.setConfiguration("{ 'update:'configuration' }");
			
			apiRepository.updatePolicyConfiguration(apiName, policyConfiguration);
			
			List<PolicyConfiguration> configurations = apiRepository.findPoliciesByApi(apiName);
			
			Optional<PolicyConfiguration> optional = configurations.stream().filter(new Predicate<PolicyConfiguration>() {

				@Override
				public boolean test(PolicyConfiguration t) {
					return t.getPolicy().equalsIgnoreCase(policyConfiguration.getPolicy());
				}
			}).findFirst();
			
			Assert.assertTrue(optional.isPresent());
			Assert.assertEquals(optional.get().getConfiguration(), policyConfiguration.getConfiguration());
			
		}catch(Exception e){
			logger.error("Error while finding api by application",e);
			Assert.fail("Error while finding api by application");
		}
	}
}
