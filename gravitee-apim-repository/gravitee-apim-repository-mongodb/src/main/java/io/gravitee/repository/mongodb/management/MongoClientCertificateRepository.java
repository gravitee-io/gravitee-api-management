/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientCertificateRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ClientCertificate;
import io.gravitee.repository.management.model.ClientCertificateStatus;
import io.gravitee.repository.mongodb.management.internal.clientcertificate.ClientCertificateMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
import io.gravitee.repository.mongodb.management.internal.model.ClientCertificateMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * MongoDB implementation of the ClientCertificateRepository.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoClientCertificateRepository implements ClientCertificateRepository {

    @Autowired
    private ClientCertificateMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Optional<ClientCertificate> findById(String id) throws TechnicalException {
        log.debug("Find client certificate by ID [{}]", id);
        ClientCertificateMongo clientCertificateMongo = internalRepository.findById(id).orElse(null);
        return Optional.ofNullable(mapper.map(clientCertificateMongo));
    }

    @Override
    public ClientCertificate create(ClientCertificate clientCertificate) throws TechnicalException {
        log.debug("Create client certificate [{}]", clientCertificate.getId());
        ClientCertificateMongo clientCertificateMongo = mapper.map(clientCertificate);
        ClientCertificateMongo createdClientCertificateMongo = internalRepository.insert(clientCertificateMongo);
        return mapper.map(createdClientCertificateMongo);
    }

    @Override
    public ClientCertificate update(ClientCertificate clientCertificate) throws TechnicalException {
        if (clientCertificate == null || clientCertificate.getId() == null) {
            throw new IllegalStateException("Client certificate to update must have an id");
        }

        log.debug("Update client certificate [{}]", clientCertificate.getId());

        ClientCertificateMongo clientCertificateMongo = internalRepository.findById(clientCertificate.getId()).orElse(null);
        if (clientCertificateMongo == null) {
            throw new IllegalStateException(String.format("No client certificate found with id [%s]", clientCertificate.getId()));
        }

        clientCertificateMongo.setName(clientCertificate.getName());
        clientCertificateMongo.setStartsAt(clientCertificate.getStartsAt());
        clientCertificateMongo.setEndsAt(clientCertificate.getEndsAt());
        clientCertificateMongo.setUpdatedAt(clientCertificate.getUpdatedAt());
        clientCertificateMongo.setStatus(clientCertificate.getStatus() != null ? clientCertificate.getStatus().name() : null);

        ClientCertificateMongo updatedClientCertificateMongo = internalRepository.save(clientCertificateMongo);
        return mapper.map(updatedClientCertificateMongo);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete client certificate [{}]", id);
        internalRepository.deleteById(id);
    }

    @Override
    public Set<ClientCertificate> findAll() throws TechnicalException {
        log.debug("Find all client certificates");
        List<ClientCertificateMongo> clientCertificates = internalRepository.findAll();
        return clientCertificates.stream().map(mapper::map).collect(Collectors.toSet());
    }

    @Override
    public Page<ClientCertificate> findByApplicationId(String applicationId, Pageable pageable) throws TechnicalException {
        log.debug("Find client certificates by application ID [{}]", applicationId);

        org.springframework.data.domain.Page<ClientCertificateMongo> mongoPage = internalRepository.findByApplicationId(
            applicationId,
            PageRequest.of(pageable.pageNumber(), pageable.pageSize())
        );

        List<ClientCertificate> content = mongoPage.getContent().stream().map(mapper::map).toList();

        return new Page<>(content, pageable.pageNumber(), pageable.pageSize(), mongoPage.getTotalElements());
    }

    @Override
    public Set<ClientCertificate> findByApplicationIdAndStatuses(String applicationId, ClientCertificateStatus... statuses)
        throws TechnicalException {
        log.debug("Find client certificates by application ID [{}] and statuses [{}]", applicationId, statuses);

        if (statuses == null || statuses.length == 0) {
            return new HashSet<>();
        }

        List<String> statusStrings = Arrays.stream(statuses).map(ClientCertificateStatus::name).toList();

        List<ClientCertificateMongo> clientCertificates = internalRepository.findByApplicationIdAndStatuses(applicationId, statusStrings);
        return clientCertificates.stream().map(mapper::map).collect(Collectors.toSet());
    }

    @Override
    public Set<ClientCertificate> findByApplicationIdsAndStatuses(Collection<String> applicationIds, ClientCertificateStatus... statuses)
        throws TechnicalException {
        log.debug("Find client certificates by application IDs [{}] and statuses [{}]", applicationIds, statuses);

        if (applicationIds == null || applicationIds.isEmpty() || statuses == null || statuses.length == 0) {
            return new HashSet<>();
        }

        List<String> statusStrings = Arrays.stream(statuses).map(ClientCertificateStatus::name).toList();
        List<String> applicationIdList = new ArrayList<>(applicationIds);

        Set<ClientCertificate> result = new HashSet<>();

        // Process application IDs in batches to avoid query size limitations
        int batchSize = 500;
        for (int i = 0; i < applicationIdList.size(); i += batchSize) {
            List<String> batch = applicationIdList.subList(i, Math.min(i + batchSize, applicationIdList.size()));

            List<ClientCertificateMongo> clientCertificates = internalRepository.findByApplicationIdsAndStatuses(batch, statusStrings);
            clientCertificates.stream().map(mapper::map).forEach(result::add);
        }

        return result;
    }

    @Override
    public boolean existsByFingerprintAndActiveApplication(String fingerprint, String environmentId) throws TechnicalException {
        log.debug(
            "Check if client certificate with fingerprint [{}] exists for an active application in environment [{}]",
            fingerprint,
            environmentId
        );

        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("fingerprint").is(fingerprint));
            query.addCriteria(Criteria.where("environmentId").is(environmentId));
            query.addCriteria(Criteria.where("status").ne(ClientCertificateStatus.REVOKED.name()));

            List<ClientCertificateMongo> certificates = mongoTemplate.find(query, ClientCertificateMongo.class);

            for (ClientCertificateMongo cert : certificates) {
                Query appQuery = new Query();
                appQuery.addCriteria(Criteria.where("_id").is(cert.getApplicationId()));
                appQuery.addCriteria(Criteria.where("status").is(ApplicationStatus.ACTIVE.name()));

                boolean activeAppExists = mongoTemplate.exists(appQuery, ApplicationMongo.class);
                if (activeAppExists) {
                    return true;
                }
            }

            return false;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to check if client certificate exists for active application", ex);
        }
    }

    @Override
    public void deleteByApplicationId(String applicationId) {
        log.debug("Delete client certificates by application ID [{}]", applicationId);
        internalRepository.deleteByApplicationId(applicationId);
    }
}
