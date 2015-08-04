package io.gravitee.repositories.mongodb.internal.node;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class NodeRepositoryImpl implements NodeRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

}
