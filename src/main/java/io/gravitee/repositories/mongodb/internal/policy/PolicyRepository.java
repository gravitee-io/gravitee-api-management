package io.gravitee.repositories.mongodb.internal.policy;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Policy;

@Repository
public interface PolicyRepository extends MongoRepository<Policy, String>, PolicyRepositoryCustom {

}


