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

import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.Set;

public class UserRepositoryTest extends AbstractMongoDBTest {

	private final static Logger logger = LoggerFactory.getLogger(UserRepositoryTest.class);
	
	@Autowired
	private UserRepository userRepository;

	@Override
	protected String getTestCasesPath() {
		return "/data/user-tests/";
	}

	@Test
	public void createUserTest() {
		try {
			String username = "createuser1";
			
			User user = new User();
			user.setUsername(username);
			user.setEmail(String.format("%s@gravitee.io", username));
			User userCreated =  userRepository.create(user);
			
			Assert.assertNotNull("User created is null", userCreated);
			
			Optional<User> optional = userRepository.findByUsername(username);
			
			Assert.assertTrue("Unable to find saved user", optional.isPresent());
			User userFound = optional.get();
			
			Assert.assertEquals("Invalid saved user name.", user.getUsername(), userFound.getUsername());
			Assert.assertEquals("Invalid saved user mail.",	user.getEmail(), userFound.getEmail());
		} catch (Exception e) {
			logger.error("Error while calling createUser", e);
			Assert.fail("Error while calling createUser");	
		}
	}

	@Test
	public void findAllTest() {
		try {
			Set<User> users= userRepository.findAll();
				
			Assert.assertNotNull(users);
			Assert.assertEquals("Invalid user numbers in find all", 6, users.size());
		} catch (Exception e) {
			logger.error("Error while finding all users",e);
			Assert.fail("Error while finding all users");
		}
	}	
	
	@Test
	public void findByEmailTest() {
		try{
			Optional<User> user= userRepository.findByEmail("user2@gravitee.io");
				
			Assert.assertNotNull("Optional is null", user);
			Assert.assertTrue("Impossible to find user by email", user.isPresent());
			
		}catch(Exception e){
			logger.error("Error while finding user by email",e);
			Assert.fail("Error while finding user by email");
		}
	}
}
