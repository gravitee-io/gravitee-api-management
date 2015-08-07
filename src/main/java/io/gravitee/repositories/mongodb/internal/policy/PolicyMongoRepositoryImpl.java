package io.gravitee.repositories.mongodb.internal.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class PolicyMongoRepositoryImpl implements PolicyMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

}
