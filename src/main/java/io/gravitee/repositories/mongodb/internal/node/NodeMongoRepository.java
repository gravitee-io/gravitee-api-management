package io.gravitee.repositories.mongodb.internal.node;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.NodeMongo;

@Repository
public interface NodeMongoRepository extends MongoRepository<NodeMongo, String>, NodeMongoRepositoryCustom {

}


