package io.gravitee.repositories.mongodb.internal.application;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.ApplicationMongo;

@Repository
public interface ApplicationMongoRepository extends MongoRepository<ApplicationMongo, String>, ApplicationMongoRepositoryCustom{




}


