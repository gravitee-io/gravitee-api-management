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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.mongodb.management.internal.model.TeamMemberMongo;
import io.gravitee.repository.mongodb.management.internal.model.UserMongo;
import io.gravitee.repository.mongodb.management.internal.user.UserMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class UserRepositoryImpl implements UserRepository {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private UserMongoRepository internalUserRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public Optional<User> findByUsername(String username) throws TechnicalException {

		logger.debug("Find user by name user [{}]", username);

		UserMongo user = internalUserRepo.findOne(username);
		User res = mapper.map(user, User.class);

		logger.debug("Find user by name user [{}] - Done", username);
		return Optional.ofNullable(res);

	}

	@Override
	public Set<User> findAll() throws TechnicalException {

		logger.debug("Find all users");

		List<UserMongo> users = internalUserRepo.findAll();
		Set<User> res = mapper.collection2set(users, UserMongo.class, User.class);

		logger.debug("Find all users - Done");
		return res;
	}

	@Override
	public Set<User> findByTeam(String teamName) throws TechnicalException {

		logger.debug("Find users by team [{}]", teamName);

		List<TeamMemberMongo> members = internalUserRepo.findByTeam(teamName);

		Set<User> res = new HashSet<>();
		for (TeamMemberMongo member : members) {
			res.add(mapper.map(member.getMember(), User.class));
		}

		logger.debug("Find users by team [{}] - Done", teamName);

		return res;
	}

	@Override
	public User create(User user) throws TechnicalException {

		logger.debug("Create user [{}]", user.getUsername());
		
		UserMongo userMongo = mapper.map(user, UserMongo.class);
		UserMongo createdUserMongo = internalUserRepo.insert(userMongo);
		
		User res = mapper.map(createdUserMongo, User.class);
		
		logger.debug("Create user [{}] - Done", user.getUsername());
		
		return res;
	}

	@Override
	public Optional<User> findByEmail(String email) throws TechnicalException {
	
		logger.debug("Find users by email [{}]", email);

		UserMongo userMongo = internalUserRepo.findByEmail(email);
		User res = mapper.map(userMongo, User.class);

		logger.debug("Find users by email [{}] - Done", email);
		return Optional.ofNullable(res);
	}

}
