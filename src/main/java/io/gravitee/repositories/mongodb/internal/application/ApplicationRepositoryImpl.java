package io.gravitee.repositories.mongodb.internal.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class ApplicationRepositoryImpl implements ApplicationRepositoryCustom{

	@Autowired
	private MongoTemplate mongoTemplate;
	
}
