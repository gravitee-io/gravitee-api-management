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

import io.gravitee.repository.api.PolicyRepository;
import io.gravitee.repository.model.Policy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryConfiguration.class })
public class PolicyRepositoryTest extends AbstractMongoDBTest {

	private static final String TESTCASES_PATH = "/data/policy-tests/";
	
	private static final int NB_POLICIES_TESTCASES = 3;

	private Logger Logger = LoggerFactory.getLogger(PolicyRepositoryTest.class);	
	
	@Autowired
	private PolicyRepository policyRepository;


    @Override
    protected String getJsonDataSetResourceName() {
        return TESTCASES_PATH;
    }
	

	
	@Test
	public void createPolicyTest() {

		try {
			Policy policy = new Policy();
			policy.setName("Ratelimit");
			policy.setVersion("1");
			policy.setDescription("Ratelimit description");
			policy.setConfiguration("{ 'name': 'sample'}");
			
			Policy createdPolicy = policyRepository.create(policy);
					
			Optional<Policy> optional = policyRepository.findById(createdPolicy.getId());
			Assert.assertTrue("Policy saved not found", optional.isPresent());
			
			Policy policySaved = optional.get();
			Assert.assertEquals("Invalid saved api name.", 			policy.getName(),			policySaved.getName());
			Assert.assertEquals("Invalid saved api version.", 		policy.getVersion(), 		policySaved.getVersion());
			Assert.assertEquals("Invalid saved api configuration.", policy.getConfiguration(), 	policySaved.getConfiguration());
			Assert.assertEquals("Invalid saved api description.", 	policy.getDescription(), 	policySaved.getDescription());

			
		} catch (Exception e) {
			Logger.error("Error while creating policy", e);
			Assert.fail("Error while creating policy");
		}
	}

	@Test
	public void findById() {
		try{
			Optional<Policy> optional = policyRepository.findById("55c8f340bc51bd82a7cb16ed");
			Assert.assertTrue("Find policy by id return no result ", optional.isPresent());
		}catch(Exception e){
			Logger.error("Error while calling findById", e);
			Assert.fail("Error while calling findById");		
		}
	}
/*	
	@Test
	public void findByNameMissingTest() {
		try{
			Optional<Api> optional = apiRepository.findByName("findByNameMissing");
			Assert.assertFalse("Find api by name on missing api return a result", optional.isPresent());
		}catch(Exception e){
			Logger.error("Error while calling findByName on missing api", e);
			Assert.fail("Error while calling findByName on missing api");		
		}
	}

	*/

	@Test
	public void findAllTest() {
		
		try{
			Set<Policy> policies = policyRepository.findAll();
			
			Assert.assertNotNull(policies);
			Assert.assertEquals("Invalid user numbers in find all", NB_POLICIES_TESTCASES, policies.size());
	
		}catch(Exception e){
			Logger.error("Error while testing policies with findAll", e);
			Assert.fail("Error while testing policies with findAll");			
		}
	}	


	@Test
	public void deletePolicyTest() {
		try{
			int nbApiBefore = policyRepository.findAll().size();
			policyRepository.delete("55c8f340bc51bd82a7cb16ef");
			int nbApiAfter = policyRepository.findAll().size();
	
			Assert.assertEquals(nbApiBefore -1, nbApiAfter);
		}catch(Exception e){
			Logger.error("Error while testing policies with findAll", e);
			Assert.fail("Error while testing policies with findAll");			
		}
	}

	@Test
	public void updatePolicyTest() {
		try{
			Policy policy = new Policy();
			policy.setId("55c8f340bc51bd82a7cb16ee");
			policy.setName("Update name");
			policy.setDescription("Update description");
			policy.setVersion("v2");
			policy.setConfiguration("{ 'name': 'configuration update'}");
			policyRepository.update(policy);
			
			Optional<Policy> optional = policyRepository.findById("55c8f340bc51bd82a7cb16ee");
			Assert.assertTrue("Policy saved not found", optional.isPresent());
			
			Policy policyUpdated = optional.get();
			Assert.assertEquals("Invalid saved api name.", 			policy.getName(),			policyUpdated.getName());
			Assert.assertEquals("Invalid saved api version.", 		policy.getVersion(), 		policyUpdated.getVersion());
			Assert.assertEquals("Invalid saved api configuration.", policy.getConfiguration(), 	policyUpdated.getConfiguration());
			Assert.assertEquals("Invalid saved api description.", 	policy.getDescription(), 	policyUpdated.getDescription());

			
		}catch(Exception e){
			Logger.error("Error while testing policies with findAll", e);
			Assert.fail("Error while testing policies with findAll");			
		}
	}
}
