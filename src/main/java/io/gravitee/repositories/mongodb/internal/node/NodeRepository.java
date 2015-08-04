package io.gravitee.repositories.mongodb.internal.node;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Node;

@Repository
public interface NodeRepository extends MongoRepository<Node, String>, NodeRepositoryCustom {

}


