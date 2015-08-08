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

import io.gravitee.repository.api.UserRepository;
import io.gravitee.repository.model.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryConfiguration.class })
public class UserRepositoryTest extends  AbstractMongoDBTest{
	
	private static final String TESTCASES_PATH = "/data/user-tests/";
	
	private static final int NB_USERS_TESTCASES = 6; 
	
	@Autowired
	private UserRepository userRepository;
	
	
	private Logger Logger = LoggerFactory.getLogger(UserRepositoryTest.class);

	@Override
	protected String getJsonDataSetResourceName() {
		return TESTCASES_PATH;
	}

	@Test
	public void createUserTest() {

		try {
	
			String username = "createuser1";
			
			User user = new User();
			user.setUsername(username);
			user.setMail(String.format("%s@gravitee.io", username));
			User userCreated =  userRepository.create(user);
			
			Assert.assertNotNull("User created is null", userCreated);
			
			Optional<User> optional = userRepository.findByUsername(username);
			
			Assert.assertTrue("Unable to find saved user", optional.isPresent());
			User userFound = optional.get();
			
			Assert.assertEquals("Invalid saved user name.", user.getUsername(), userFound.getUsername());
			Assert.assertEquals("Invalid saved user mail.",	user.getMail(), userFound.getMail());
		
			
		} catch (Exception e) {
			Logger.error("Error while calling createUser", e);
			Assert.fail("Error while calling createUser");	
		}
	}


	@Test
	public void findAllTest() {
		Set<User> users= userRepository.findAll();
			
		Assert.assertNotNull(users);
		Assert.assertEquals("Invalid user numbers in find all", NB_USERS_TESTCASES, users.size());
	}	
	

}
