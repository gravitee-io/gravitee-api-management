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
package io.gravitee.rest.api.service.impl;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientCertificateRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificateStatus;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.UpdateClientCertificate;
import io.gravitee.rest.api.service.ClientCertificateService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ClientCertificateAlreadyUsedException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.xml.bind.DatatypeConverter;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of the ClientCertificateService.
 *
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ClientCertificateServiceImpl extends TransactionalService implements ClientCertificateService {

    @Lazy
    @Autowired
    private ClientCertificateRepository clientCertificateRepository;

    @Override
    public ClientCertificate findById(String clientCertificateId) {
        try {
            log.debug("Find client certificate by ID: {}", clientCertificateId);
            return clientCertificateRepository
                .findById(clientCertificateId)
                .map(this::toModel)
                .orElseThrow(() -> new ClientCertificateNotFoundException(clientCertificateId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find client certificate by ID " + clientCertificateId,
                e
            );
        }
    }

    @Override
    public ClientCertificate create(String applicationId, CreateClientCertificate createClientCertificate) {
        try {
            log.debug("Create client certificate for application: {}", applicationId);

            X509Certificate x509Certificate = parseCertificate(createClientCertificate.certificate());
            String fingerprint = computeFingerprint(x509Certificate);
            String environmentId = GraviteeContext.getCurrentEnvironment();

            if (clientCertificateRepository.existsByFingerprintAndActiveApplication(fingerprint, environmentId)) {
                throw new ClientCertificateAlreadyUsedException(fingerprint);
            }

            io.gravitee.repository.management.model.ClientCertificate clientCertificate =
                new io.gravitee.repository.management.model.ClientCertificate();

            clientCertificate.setId(UuidString.generateRandom());
            clientCertificate.setCrossId(UuidString.generateRandom());
            clientCertificate.setApplicationId(applicationId);
            clientCertificate.setName(createClientCertificate.name());
            clientCertificate.setStartsAt(createClientCertificate.startsAt());
            clientCertificate.setEndsAt(createClientCertificate.endsAt());
            clientCertificate.setCertificate(createClientCertificate.certificate());
            clientCertificate.setCertificateExpiration(x509Certificate.getNotAfter());
            clientCertificate.setSubject(x509Certificate.getSubjectX500Principal().getName());
            clientCertificate.setIssuer(x509Certificate.getIssuerX500Principal().getName());
            clientCertificate.setFingerprint(fingerprint);
            clientCertificate.setOrganizationId(GraviteeContext.getCurrentOrganization());
            clientCertificate.setEnvironmentId(environmentId);
            clientCertificate.setCreatedAt(new Date());
            clientCertificate.setStatus(computeStatus(createClientCertificate.startsAt(), createClientCertificate.endsAt()));

            io.gravitee.repository.management.model.ClientCertificate created = clientCertificateRepository.create(clientCertificate);
            return toModel(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to create client certificate for application " + applicationId,
                e
            );
        }
    }

    @Override
    public ClientCertificate update(String clientCertificateId, UpdateClientCertificate updateClientCertificate) {
        try {
            log.debug("Update client certificate: {}", clientCertificateId);

            io.gravitee.repository.management.model.ClientCertificate existingCertificate = clientCertificateRepository
                .findById(clientCertificateId)
                .orElseThrow(() -> new ClientCertificateNotFoundException(clientCertificateId));

            existingCertificate.setName(updateClientCertificate.name());
            existingCertificate.setStartsAt(updateClientCertificate.startsAt());
            existingCertificate.setEndsAt(updateClientCertificate.endsAt());
            existingCertificate.setUpdatedAt(new Date());
            existingCertificate.setStatus(computeStatus(updateClientCertificate.startsAt(), updateClientCertificate.endsAt()));

            io.gravitee.repository.management.model.ClientCertificate updated = clientCertificateRepository.update(existingCertificate);
            return toModel(updated);
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
    public Page<ClientCertificate> findByApplicationId(String applicationId, io.gravitee.rest.api.model.common.Pageable pageable) {
        try {
            log.debug("Find client certificates by application ID: {}", applicationId);

            var repoPageable = new PageableBuilder().pageNumber(pageable.getPageNumber()).pageSize(pageable.getPageSize()).build();

            Page<io.gravitee.repository.management.model.ClientCertificate> page = clientCertificateRepository.findByApplicationId(
                applicationId,
                repoPageable
            );

            return new Page<>(
                page.getContent().stream().map(this::toModel).toList(),
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
    public Set<ClientCertificate> findByApplicationIdAndStatuses(String applicationId, Collection<ClientCertificateStatus> statuses) {
        try {
            log.debug("Find client certificates by application ID {} and statuses {}", applicationId, statuses);

            Collection<io.gravitee.repository.management.model.ClientCertificateStatus> repoStatuses = statuses
                .stream()
                .map(status -> io.gravitee.repository.management.model.ClientCertificateStatus.valueOf(status.name()))
                .toList();

            return clientCertificateRepository
                .findByApplicationIdAndStatuses(applicationId, repoStatuses)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find client certificates by application ID " + applicationId + " and statuses",
                e
            );
        }
    }

    private X509Certificate parseCertificate(String pemCertificate) {
        try {
            Certificate[] certificates = KeyStoreUtils.loadPemCertificates(pemCertificate);
            if (certificates.length == 0) {
                throw new TechnicalManagementException("Invalid PEM certificate: no certificate found");
            }
            return (X509Certificate) certificates[0];
        } catch (Exception e) {
            throw new TechnicalManagementException("Failed to parse PEM certificate", e);
        }
    }

    private String computeFingerprint(X509Certificate certificate) {
        try {
            return DatatypeConverter.printHexBinary(new SHA256.Digest().digest(certificate.getEncoded())).toLowerCase();
        } catch (CertificateEncodingException e) {
            throw new TechnicalManagementException("Failed to compute certificate fingerprint", e);
        }
    }

    private io.gravitee.repository.management.model.ClientCertificateStatus computeStatus(Date startsAt, Date endsAt) {
        Date now = new Date();

        if (endsAt != null && now.after(endsAt)) {
            return io.gravitee.repository.management.model.ClientCertificateStatus.REVOKED;
        }

        if (startsAt != null && now.before(startsAt)) {
            return io.gravitee.repository.management.model.ClientCertificateStatus.SCHEDULED;
        }

        if (endsAt != null) {
            return io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE_WITH_END;
        }

        return io.gravitee.repository.management.model.ClientCertificateStatus.ACTIVE;
    }

    private ClientCertificate toModel(io.gravitee.repository.management.model.ClientCertificate entity) {
        return new ClientCertificate(
            entity.getId(),
            entity.getCrossId(),
            entity.getApplicationId(),
            entity.getName(),
            entity.getStartsAt(),
            entity.getEndsAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCertificate(),
            entity.getCertificateExpiration(),
            entity.getSubject(),
            entity.getIssuer(),
            entity.getFingerprint(),
            entity.getEnvironmentId(),
            entity.getOrganizationId(),
            entity.getStatus() != null ? ClientCertificateStatus.valueOf(entity.getStatus().name()) : null
        );
    }
}
