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

import static org.junit.Assert.*;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repository.api.ApiKeyRepository;
import io.gravitee.repository.model.ApiKey;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestRepositoryConfiguration.class })
public class ApiKeyRepositoryTest extends AbstractMongoDBTest {

	private static final String TESTCASES_PATH = "/data/apikey-tests/";
	
	private Logger logger = LoggerFactory.getLogger(ApiKeyRepositoryTest.class);	
	
	@Autowired
	private ApiKeyRepository apiKeyRepository;
	
    @Override
    protected String getTestCasesPath() {
        return TESTCASES_PATH;
    }

    @Test
    public void createKeyTest(){
    	
    	try{
	    	String apiName = "api1";
	    	String applicationName = "application1";
	    	String key = UUID.randomUUID().toString();
	    	
	    	ApiKey apiKey = new ApiKey();
	    	apiKey.setKey(key);
	    	apiKey.setExpiration(new Date());

	    	apiKeyRepository.create(applicationName, apiName, apiKey);
	
	    	Optional<ApiKey> optional = apiKeyRepository.retrieve(key);	
	    	Assert.assertTrue("ApiKey not found", optional.isPresent());
	    	
	    	ApiKey keyFound = optional.get();
	    	
	    	Assert.assertNotNull("ApiKey not found", keyFound);
	    	
	    	Assert.assertEquals("Key value saved doesn't match", apiKey.getKey(), keyFound.getKey());
	    	Assert.assertEquals("Key expiration doesn't match",  apiKey.getExpiration(), keyFound.getExpiration());
   
    	}catch(Exception e){
    		logger.error("Error while creating key",e);
    		Assert.fail("Error while creating key");
    	}
    }
    
    @Test
    public void retrieveKeyTest() {
	    try{
	    	
	    	String key = "d449098d-8c31-4275-ad59-8dd707865a33";
	    	
	    	Optional<ApiKey> optional = apiKeyRepository.retrieve(key);
	    	
	    	Assert.assertTrue("ApiKey not found", optional.isPresent());
	    	
	    	ApiKey keyFound = optional.get();
	    	Assert.assertNotNull("ApiKey not found", keyFound);
	    	
	    }catch(Exception e){
			logger.error("Error while getting key",e);
			Assert.fail("Error while getting key");
		}
	} 
    
    @Test
    public void retrieveMissingKeyTest() {
	    try{
	    	
	    	String key = "d449098d-8c31-4275-ad59-000000000";
	    	
	    	Optional<ApiKey> optional = apiKeyRepository.retrieve(key);
	    	
	    	Assert.assertFalse("Invalid ApiKey found", optional.isPresent());
	    	
	    }catch(Exception e){
			logger.error("Error while retrieving missing key",e);
			Assert.fail("Error while retrieving missing key");
		}
	}
    
    @Test
	public void findByApplicationTest() throws Exception {
    	try{
    		
    		Set<ApiKey> apiKeys = apiKeyRepository.findByApplication("application1");
    		
    		Assert.assertNotNull("ApiKey not found", apiKeys);
    		Assert.assertEquals("Invalid number of ApiKey found", 2, apiKeys.size());
    		
    	}catch(Exception e){
 			logger.error("Error while testing findByApplication",e);
 			Assert.fail("Error while testing findByApplication");
 		}
	}
    
    @Test
	public void findByApplicationNoResult() throws Exception {
    	try{
    		
    		Set<ApiKey> apiKeys = apiKeyRepository.findByApplication("application-no-api-key");
    		Assert.assertNotNull("ApiKey Set is null", apiKeys);
    		
    		Assert.assertTrue("Api found on application with no api", apiKeys.isEmpty());
    	}catch(Exception e){
 			logger.error("Error while testing findByApplication",e);
 			Assert.fail("Error while testing findByApplication");
 		}
	}

    @Test
	public void findByApplicationAndApi() throws Exception {
    	Set<ApiKey> apiKeys = apiKeyRepository.findByApplicationAndApi("application1", "api1");
    	
    	Assert.assertNotNull("ApiKey Set is null", apiKeys);
    	Assert.assertEquals("Invalid number of ApiKey found", 1, apiKeys.size());
    }
}
