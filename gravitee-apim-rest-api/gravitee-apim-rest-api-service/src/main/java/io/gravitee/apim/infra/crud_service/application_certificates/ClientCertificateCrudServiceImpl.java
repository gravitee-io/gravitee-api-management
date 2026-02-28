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
package io.gravitee.apim.infra.crud_service.application_certificates;

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.apim.infra.adapter.ClientCertificateAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.security.CertificateUtils;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientCertificateRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAlreadyUsedException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAuthorityException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateDateBoundsInvalidException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateEmptyException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of the ClientCertificateCrudService.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class ClientCertificateCrudServiceImpl extends TransactionalService implements ClientCertificateCrudService {

    @Lazy
    @Autowired
    private ClientCertificateRepository clientCertificateRepository;

    @Override
    public ClientCertificate findById(String clientCertificateId) {
        try {
            log.debug("Find client certificate by ID: {}", clientCertificateId);
            return clientCertificateRepository
                .findById(clientCertificateId)
                .map(ClientCertificateAdapter.INSTANCE::toDomain)
                .orElseThrow(() -> new ClientCertificateNotFoundException(clientCertificateId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find client certificate by ID " + clientCertificateId,
                e
            );
        }
    }

    @Override
    public ClientCertificate create(String applicationId, ClientCertificate clientCertificateToCreate) {
        try {
            log.debug("Create client certificate for application: {}", applicationId);

            X509Certificate x509Certificate = parseCertificate(clientCertificateToCreate.certificate());
            String fingerprint = CertificateUtils.generateThumbprint(x509Certificate, "SHA-256");
            if (fingerprint == null) {
                throw new ClientCertificateInvalidException();
            }
            String environmentId = GraviteeContext.getCurrentEnvironment();

            if (clientCertificateRepository.existsByFingerprintAndActiveApplication(fingerprint, environmentId)) {
                throw new ClientCertificateAlreadyUsedException(fingerprint);
            }
            validateDateBounds(clientCertificateToCreate);

            var clientCertificate = ClientCertificateAdapter.INSTANCE.toRepo(clientCertificateToCreate);

            clientCertificate.setId(UuidString.generateRandom());
            clientCertificate.setCrossId(UuidString.generateRandom());
            clientCertificate.setApplicationId(applicationId);
            clientCertificate.setCertificateExpiration(x509Certificate.getNotAfter());
            clientCertificate.setSubject(x509Certificate.getSubjectX500Principal().getName());
            clientCertificate.setIssuer(x509Certificate.getIssuerX500Principal().getName());
            clientCertificate.setFingerprint(fingerprint);
            clientCertificate.setEnvironmentId(environmentId);
            clientCertificate.setCreatedAt(new Date());
            clientCertificate.setStatus(computeStatus(clientCertificateToCreate.startsAt(), clientCertificateToCreate.endsAt()));

            return ClientCertificateAdapter.INSTANCE.toDomain(clientCertificateRepository.create(clientCertificate));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to create client certificate for application " + applicationId,
                e
            );
        }
    }

    private void validateDateBounds(ClientCertificate clientCertificate) {
        if (
            !Duration.between(
                Optional.ofNullable(clientCertificate.startsAt()).map(Date::toInstant).orElse(Instant.ofEpochMilli(Long.MIN_VALUE)),
                Optional.ofNullable(clientCertificate.endsAt()).map(Date::toInstant).orElse(Instant.ofEpochMilli(Long.MAX_VALUE))
            ).isPositive()
        ) {
            throw new ClientCertificateDateBoundsInvalidException();
        }
    }

    @Override
    public ClientCertificate update(String clientCertificateId, ClientCertificate update) {
        try {
            log.debug("Update client certificate: {}", clientCertificateId);

            var existingCertificate = clientCertificateRepository
                .findById(clientCertificateId)
                .orElseThrow(() -> new ClientCertificateNotFoundException(clientCertificateId));

            validateDateBounds(update);

            existingCertificate.setName(update.name());
            existingCertificate.setStartsAt(update.startsAt());
            existingCertificate.setEndsAt(update.endsAt());
            existingCertificate.setUpdatedAt(new Date());
            existingCertificate.setStatus(computeStatus(update.startsAt(), update.endsAt()));

            return ClientCertificateAdapter.INSTANCE.toDomain(clientCertificateRepository.update(existingCertificate));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to update client certificate " + clientCertificateId, e);
        }
    }

    @Override
    public void delete(String clientCertificateId) {
        try {
            log.debug("Delete client certificate: {}", clientCertificateId);

            clientCertificateRepository
                .findById(clientCertificateId)
                .orElseThrow(() -> new ClientCertificateNotFoundException(clientCertificateId));

            clientCertificateRepository.delete(clientCertificateId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to delete client certificate " + clientCertificateId, e);
        }
    }

    @Override
    public Page<ClientCertificate> findByApplicationId(String applicationId, Pageable pageable) {
        try {
            log.debug("Find client certificates by application ID: {}", applicationId);

            var repoPageable = new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build();

            var page = clientCertificateRepository.findByApplicationId(applicationId, repoPageable);

            return new Page<>(
                page.getContent().stream().map(ClientCertificateAdapter.INSTANCE::toDomain).toList(),
                page.getPageNumber(),
                (int) page.getPageElements(),
                page.getTotalElements()
            );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find client certificates by application ID " + applicationId,
                e
            );
        }
    }

    @Override
    public Set<ClientCertificate> findByApplicationIdAndStatuses(String applicationId, ClientCertificateStatus... statuses) {
        try {
            log.debug("Find client certificates by application ID {} and statuses {}", applicationId, Arrays.toString(statuses));
            return clientCertificateRepository
                .findByApplicationIdAndStatuses(applicationId, ClientCertificateAdapter.INSTANCE.toRepoStatuses(statuses))
                .stream()
                .map(ClientCertificateAdapter.INSTANCE::toDomain)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find client certificates by application ID " + applicationId + " and statuses",
                e
            );
        }
    }

    @Override
    public Set<ClientCertificate> findByApplicationIdsAndStatuses(Collection<String> applicationIds, ClientCertificateStatus... statuses) {
        try {
            log.debug("Find client certificates by application IDs {} and statuses {}", applicationIds, Arrays.toString(statuses));
            return clientCertificateRepository
                .findByApplicationIdsAndStatuses(applicationIds, ClientCertificateAdapter.INSTANCE.toRepoStatuses(statuses))
                .stream()
                .map(ClientCertificateAdapter.INSTANCE::toDomain)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find client certificates by application IDs and statuses",
                e
            );
        }
    }

    @Override
    public Set<ClientCertificate> findByStatuses(ClientCertificateStatus... statuses) {
        try {
            log.debug("Find client certificates by statuses {}", Arrays.toString(statuses));
            return clientCertificateRepository
                .findByStatuses(ClientCertificateAdapter.INSTANCE.toRepoStatuses(statuses))
                .stream()
                .map(ClientCertificateAdapter.INSTANCE::toDomain)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find client certificates by statuses", e);
        }
    }

    private X509Certificate parseCertificate(String pemCertificate) {
        Certificate[] certificates = null;
        try {
            certificates = KeyStoreUtils.loadPemCertificates(pemCertificate);
        } catch (Exception e) {
            throw new ClientCertificateInvalidException();
        }

        if (certificates.length == 0) {
            throw new ClientCertificateEmptyException();
        }
        if (certificates.length > 1) {
            log.debug("Certificate chain contains multiple certificates, using the first one");
        }
        X509Certificate certificate = (X509Certificate) certificates[0];
        // Accept only client certificates.
        final boolean isCertificateAuthority = certificate.getBasicConstraints() != -1;
        if (isCertificateAuthority) {
            throw new ClientCertificateAuthorityException();
        }
        return certificate;
    }

    private io.gravitee.repository.management.model.ClientCertificateStatus computeStatus(Date startsAt, Date endsAt) {
        var status = ClientCertificateStatus.computeStatus(startsAt, endsAt);
        return ClientCertificateAdapter.INSTANCE.toRepoStatus(status);
    }

    @Override
    public void deleteByApplicationId(String applicationId) {
        try {
            log.debug("Delete all client certificates for application: {}", applicationId);
            clientCertificateRepository.deleteByApplicationId(applicationId);
        } catch (Exception e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to delete client certificates for application " + applicationId,
                e
            );
        }
    }

    @Override
    public Optional<ClientCertificate> findMostRecentActiveByApplicationId(String applicationId) {
        try {
            log.debug("Find most recent active client certificate for application: {}", applicationId);

            Set<io.gravitee.repository.management.model.ClientCertificate> certificates =
                clientCertificateRepository.findByApplicationIdAndStatuses(
                    applicationId,
                    io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE,
                    io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE_WITH_END
                );

            return certificates
                .stream()
                .max(Comparator.comparing(io.gravitee.repository.management.model.ClientCertificate::getCreatedAt))
                .map(ClientCertificateAdapter.INSTANCE::toDomain);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find most recent active client certificate for application " + applicationId,
                e
            );
        }
    }
}
