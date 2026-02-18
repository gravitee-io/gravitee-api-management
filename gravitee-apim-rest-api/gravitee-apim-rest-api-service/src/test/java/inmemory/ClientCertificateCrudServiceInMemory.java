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
package inmemory;

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.exceptions.ClientCertificateNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClientCertificateCrudServiceInMemory implements ClientCertificateCrudService, InMemoryAlternative<ClientCertificate> {

    final ArrayList<ClientCertificate> storage = new ArrayList<>();

    @Override
    public ClientCertificate findById(String clientCertificateId) {
        return storage
            .stream()
            .filter(cert -> clientCertificateId.equals(cert.getId()))
            .findFirst()
            .orElseThrow(() -> new ClientCertificateNotFoundException(clientCertificateId));
    }

    @Override
    public ClientCertificate create(String applicationId, ClientCertificate clientCertificate) {
        Date now = new Date();
        ClientCertificateStatus status = computeStatus(clientCertificate.getStartsAt(), clientCertificate.getEndsAt());

        ClientCertificate certificate = ClientCertificate.builder()
            .id(UUID.randomUUID().toString())
            .crossId(UUID.randomUUID().toString())
            .applicationId(applicationId)
            .name(clientCertificate.getName())
            .startsAt(clientCertificate.getStartsAt())
            .endsAt(clientCertificate.getEndsAt())
            .createdAt(now)
            .updatedAt(now)
            .certificate(clientCertificate.getCertificate())
            .certificateExpiration(null) // would be parsed from PEM in real impl
            .subject(null) // would be parsed from PEM in real impl
            .issuer(null) // would be parsed from PEM in real impl
            .fingerprint(null) // would be computed from PEM in real impl
            .environmentId(null) // would come from GraviteeContext in real impl
            .status(status)
            .build();

        storage.add(certificate);
        return certificate;
    }

    @Override
    public ClientCertificate update(String clientCertificateId, ClientCertificate clientCertificateToUpdate) {
        OptionalInt index = findIndex(storage, cert -> cert.getId().equals(clientCertificateId));
        if (index.isEmpty()) {
            throw new ClientCertificateNotFoundException(clientCertificateId);
        }

        ClientCertificate existing = storage.get(index.getAsInt());
        ClientCertificateStatus status = computeStatus(clientCertificateToUpdate.getStartsAt(), clientCertificateToUpdate.getEndsAt());

        ClientCertificate updated = ClientCertificate.builder()
            .id(existing.getId())
            .crossId(existing.getCrossId())
            .applicationId(existing.getApplicationId())
            .name(clientCertificateToUpdate.getName())
            .startsAt(clientCertificateToUpdate.getStartsAt())
            .endsAt(clientCertificateToUpdate.getEndsAt())
            .createdAt(existing.getCreatedAt())
            .updatedAt(new Date())
            .certificate(existing.getCertificate())
            .certificateExpiration(existing.getCertificateExpiration())
            .subject(existing.getSubject())
            .issuer(existing.getIssuer())
            .fingerprint(existing.getFingerprint())
            .environmentId(existing.getEnvironmentId())
            .status(status)
            .build();

        storage.set(index.getAsInt(), updated);
        return updated;
    }

    @Override
    public void delete(String clientCertificateId) {
        boolean found = storage.stream().anyMatch(cert -> clientCertificateId.equals(cert.getId()));
        if (!found) {
            throw new ClientCertificateNotFoundException(clientCertificateId);
        }
        storage.removeIf(cert -> clientCertificateId.equals(cert.getId()));
    }

    @Override
    public Page<ClientCertificate> findByApplicationId(String applicationId, Pageable pageable) {
        List<ClientCertificate> filtered = storage
            .stream()
            .filter(cert -> applicationId.equals(cert.getApplicationId()))
            .toList();

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        // PageableImpl uses 1-based page numbering, handle both 0-based and 1-based
        int fromIndex = Math.max(0, (pageNumber - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        List<ClientCertificate> pageContent = fromIndex < filtered.size() ? filtered.subList(fromIndex, toIndex) : Collections.emptyList();

        return new Page<>(pageContent, pageNumber, pageSize, filtered.size());
    }

    @Override
    public Set<ClientCertificate> findByApplicationIdAndStatuses(String applicationId, ClientCertificateStatus... statuses) {
        if (statuses == null || statuses.length == 0) {
            return Set.of();
        }
        Set<ClientCertificateStatus> statusSet = Set.of(statuses);
        return storage
            .stream()
            .filter(cert -> applicationId.equals(cert.getApplicationId()))
            .filter(cert -> statusSet.contains(cert.getStatus()))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<ClientCertificate> findByApplicationIdsAndStatuses(Collection<String> applicationIds, ClientCertificateStatus... statuses) {
        if (statuses == null || statuses.length == 0) {
            return Set.of();
        }
        Set<ClientCertificateStatus> statusSet = Set.of(statuses);
        return storage
            .stream()
            .filter(cert -> applicationIds.contains(cert.getApplicationId()))
            .filter(cert -> statusSet.contains(cert.getStatus()))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<ClientCertificate> findByStatuses(ClientCertificateStatus... statuses) {
        if (statuses == null || statuses.length == 0) {
            return Set.of();
        }
        var statusSet = Set.of(statuses);
        return storage
            .stream()
            .filter(cert -> statusSet.contains(cert.getStatus()))
            .collect(Collectors.toSet());
    }

    @Override
    public void deleteByApplicationId(String applicationId) {
        storage.removeIf(cert -> applicationId.equals(cert.getApplicationId()));
    }

    @Override
    public Optional<ClientCertificate> findMostRecentActiveByApplicationId(String applicationId) {
        return storage
            .stream()
            .filter(cert -> applicationId.equals(cert.getApplicationId()))
            .filter(
                cert -> cert.getStatus() == ClientCertificateStatus.ACTIVE || cert.getStatus() == ClientCertificateStatus.ACTIVE_WITH_END
            )
            .max(Comparator.comparing(ClientCertificate::getCreatedAt));
    }

    @Override
    public void initWith(List<ClientCertificate> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ClientCertificate> storage() {
        return Collections.unmodifiableList(storage);
    }

    private ClientCertificateStatus computeStatus(Date startsAt, Date endsAt) {
        Instant now = Instant.now();

        if (endsAt != null && endsAt.toInstant().isBefore(now)) {
            return ClientCertificateStatus.REVOKED;
        }
        if (startsAt != null && startsAt.toInstant().isAfter(now)) {
            return ClientCertificateStatus.SCHEDULED;
        }
        if (endsAt != null) {
            return ClientCertificateStatus.ACTIVE_WITH_END;
        }
        return ClientCertificateStatus.ACTIVE;
    }
}
