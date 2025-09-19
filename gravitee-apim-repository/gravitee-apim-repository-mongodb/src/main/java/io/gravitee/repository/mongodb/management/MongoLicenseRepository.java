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
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.search.LicenseCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.License;
import io.gravitee.repository.mongodb.management.internal.license.LicenseMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.LicenseMongo;
import io.gravitee.repository.mongodb.management.internal.model.LicensePkMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MongoLicenseRepository implements LicenseRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoLicenseRepository.class);

    @Autowired
    private LicenseMongoRepository internalLicenseRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<License> findById(String referenceId, License.ReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("Find license by ID [{} {}]", referenceId, referenceType);

        final LicenseMongo license = internalLicenseRepo.findById(new LicensePkMongo(referenceId, referenceType.name())).orElse(null);

        LOGGER.debug("Find license by ID [{}] - Done", license);
        return Optional.ofNullable(mapper.map(license));
    }

    @Override
    public License create(License license) throws TechnicalException {
        LOGGER.debug("Create license with id [{} {}]", license.getReferenceId(), license.getReferenceType());

        LicenseMongo licenseMongo = mapper.map(license);
        LicenseMongo createdOrganizationMongo = internalLicenseRepo.insert(licenseMongo);

        License res = mapper.map(createdOrganizationMongo);

        LOGGER.debug("Create license [{} {}] - Done", res.getReferenceId(), res.getReferenceType());

        return res;
    }

    @Override
    public License update(License license) throws TechnicalException {
        final LicenseMongo organizationMongo = internalLicenseRepo
            .findById(new LicensePkMongo(license.getReferenceId(), license.getReferenceType().name()))
            .orElse(null);

        if (organizationMongo == null) {
            throw new IllegalStateException(
                String.format("No licenses found with id [%s %s]", license.getReferenceId(), license.getReferenceType())
            );
        }

        try {
            //Update
            organizationMongo.setLicense(license.getLicense());
            organizationMongo.setUpdatedAt(license.getUpdatedAt());

            LicenseMongo licenseMongoUpdated = internalLicenseRepo.save(organizationMongo);
            return mapper.map(licenseMongoUpdated);
        } catch (Exception e) {
            LOGGER.error("An error occurred when updating license", e);
            throw new TechnicalException("An error occurred when updating license");
        }
    }

    @Override
    public void delete(String referenceId, License.ReferenceType referenceType) throws TechnicalException {
        try {
            internalLicenseRepo.deleteById(new LicensePkMongo(referenceId, referenceType.name()));
        } catch (Exception e) {
            LOGGER.error("An error occurred when deleting license [{} {}]", referenceId, referenceType, e);
            throw new TechnicalException("An error occurred when deleting license");
        }
    }

    @Override
    public Page<License> findByCriteria(LicenseCriteria criteria, Pageable pageable) throws TechnicalException {
        final Page<LicenseMongo> licenseMongoPage = internalLicenseRepo.search(criteria, pageable);
        final List<License> content = mapper.mapLicenses(licenseMongoPage.getContent());

        return new Page(
            content,
            licenseMongoPage.getPageNumber(),
            (int) licenseMongoPage.getPageElements(),
            licenseMongoPage.getTotalElements()
        );
    }

    @Override
    public Set<License> findAll() throws TechnicalException {
        try {
            return internalLicenseRepo
                .findAll()
                .stream()
                .map(organization -> mapper.map(organization))
                .collect(Collectors.toSet());
        } catch (Exception e) {
            LOGGER.error("An error occurred when finding all licenses", e);
            throw new TechnicalException("An error occurred when finding all licenses");
        }
    }
}
