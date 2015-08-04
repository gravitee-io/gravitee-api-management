package io.gravitee.repositories.mongodb.internal.application;

import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import io.gravitee.repositories.mongodb.internal.model.Application;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String>, ApplicationRepositoryCustom{

    List<Application> findAll();

   // Set<Application> findByUser(String username);
}


