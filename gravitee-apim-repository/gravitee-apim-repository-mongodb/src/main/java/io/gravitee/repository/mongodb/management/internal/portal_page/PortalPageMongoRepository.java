package io.gravitee.repository.mongodb.management.internal.portal_page;

import io.gravitee.repository.mongodb.management.internal.model.PortalPageMongo;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PortalPageMongoRepository extends MongoRepository<PortalPageMongo, String> {
    @Query("{ 'contexts': ?0 }")
    List<PortalPageMongo> findByContext(String context);
}

