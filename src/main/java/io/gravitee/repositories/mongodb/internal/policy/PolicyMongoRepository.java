package io.gravitee.repositories.mongodb.internal.policy;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.PolicyMongo;

@Repository
public interface PolicyMongoRepository extends MongoRepository<PolicyMongo, String>, PolicyMongoRepositoryCustom {


}


