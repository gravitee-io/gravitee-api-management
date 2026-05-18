package io.gravitee.gamma.module.authz.entityimport.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ScimConnectorRepository {

    private final MongoOperations mongo;

    @Autowired
    public ScimConnectorRepository(@Qualifier("managementMongoTemplate") MongoOperations mongo) {
        this.mongo = mongo;
    }

    public ScimConnectorDocument save(ScimConnectorDocument doc) {
        return mongo.save(doc);
    }

    public Optional<ScimConnectorDocument> findById(String id, String environmentId) {
        Query q = Query.query(Criteria.where("_id").is(id).and("environmentId").is(environmentId));
        return Optional.ofNullable(mongo.findOne(q, ScimConnectorDocument.class));
    }

    public Optional<ScimConnectorDocument> findByEnvAndName(String environmentId, String name) {
        Query q = Query.query(Criteria.where("environmentId").is(environmentId).and("name").is(name));
        return Optional.ofNullable(mongo.findOne(q, ScimConnectorDocument.class));
    }

    public List<ScimConnectorDocument> findByEnvironment(String environmentId) {
        Query q = Query.query(Criteria.where("environmentId").is(environmentId)).with(Sort.by(Sort.Direction.ASC, "name"));
        return mongo.find(q, ScimConnectorDocument.class);
    }

    public List<ScimConnectorDocument> findAll() {
        return mongo.findAll(ScimConnectorDocument.class);
    }

    public boolean deleteById(String id, String environmentId) {
        Query q = Query.query(Criteria.where("_id").is(id).and("environmentId").is(environmentId));
        return mongo.remove(q, ScimConnectorDocument.class).getDeletedCount() > 0;
    }
}
