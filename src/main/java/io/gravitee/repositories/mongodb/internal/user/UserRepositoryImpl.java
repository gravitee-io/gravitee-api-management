package io.gravitee.repositories.mongodb.internal.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class UserRepositoryImpl implements UserRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

}
