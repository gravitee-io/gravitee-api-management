package io.gravitee.repositories.mongodb.internal.team;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class TeamMongoRepositoryImpl implements TeamMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

}
