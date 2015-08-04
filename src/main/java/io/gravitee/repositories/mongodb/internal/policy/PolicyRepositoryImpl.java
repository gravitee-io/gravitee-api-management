package io.gravitee.repositories.mongodb.internal.policy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class PolicyRepositoryImpl implements PolicyRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

}
