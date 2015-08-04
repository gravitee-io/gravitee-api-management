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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repositories.mongodb.internal.api.ApiRepository;
import io.gravitee.repositories.mongodb.internal.model.Api;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={ RepositoryConfiguration.class})
public class ApiRepositoryTest {

	@Autowired
    private ApiRepository  repository;
    
	@Test 
	public void createApiTest(){
		
    	Api api = new Api();
    	api.setName("sample");
    	api.setVersion("1");
    	repository.save(api);
	}
	
    @Test 
    public void findByNameTest(){
    	Api api = repository.findByName("sample");
    	Assert.assertNotNull(api);
    }
    
    @Test
    public void start(){
    	repository.start("sample");
    }
     
    @Test
    public void stop(){
    	repository.stop("sample");
    }
    
    @Test
    public void findAll(){
    	List<Api> apis = repository.findAll();
    	Assert.assertNotNull(apis);
    }
    
	@Test
	public void deleteApiTest(){
		repository.delete("sample");
	}
    

}
