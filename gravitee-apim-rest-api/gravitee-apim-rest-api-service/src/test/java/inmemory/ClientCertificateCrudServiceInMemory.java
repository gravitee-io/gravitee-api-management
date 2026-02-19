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
            .filter(cert -> clientCertificateId.equals(cert.id()))
            .findFirst()
            .map(cert -> cert.toBuilder().build())
            .orElseThrow(() -> new ClientCertificateNotFoundException(clientCertificateId));
    }

    @Override
    public ClientCertificate create(String applicationId, ClientCertificate clientCertificate) {
        Date now = new Date();
        ClientCertificateStatus status = ClientCertificateStatus.computeStatus(
            clientCertificate.startsAt(),
            clientCertificate.endsAt()
        );

        ClientCertificate certificate = new ClientCertificate(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            applicationId,
            clientCertificate.name(),
            clientCertificate.startsAt(),
            clientCertificate.endsAt(),
            now,
            now,
            clientCertificate.certificate(),
            null,
            null,
            null,
            null,
            null,
            status
        );

        storage.add(certificate);
        return certificate.toBuilder().build();
    }

    @Override
    public ClientCertificate update(String clientCertificateId, ClientCertificate clientCertificateToUpdate) {
        OptionalInt index = findIndex(storage, cert -> cert.id().equals(clientCertificateId));
        if (index.isEmpty()) {
            throw new ClientCertificateNotFoundException(clientCertificateId);
        }

        ClientCertificate existing = storage.get(index.getAsInt());
        ClientCertificateStatus status = ClientCertificateStatus.computeStatus(
            clientCertificateToUpdate.startsAt(),
            clientCertificateToUpdate.endsAt()
        );

        ClientCertificate updated = new ClientCertificate(
            existing.id(),
            existing.crossId(),
            existing.applicationId(),
            clientCertificateToUpdate.name(),
            clientCertificateToUpdate.startsAt(),
            clientCertificateToUpdate.endsAt(),
            existing.createdAt(),
            new Date(),
            existing.certificate(),
            existing.certificateExpiration(),
            existing.subject(),
            existing.issuer(),
            existing.fingerprint(),
            existing.environmentId(),
            status
        );

        storage.set(index.getAsInt(), updated);
        return updated.toBuilder().build();
    }

    @Override
    public void delete(String clientCertificateId) {
        boolean found = storage.stream().anyMatch(cert -> clientCertificateId.equals(cert.id()));
        if (!found) {
            throw new ClientCertificateNotFoundException(clientCertificateId);
        }
        storage.removeIf(cert -> clientCertificateId.equals(cert.id()));
    }

    @Override
    public Page<ClientCertificate> findByApplicationId(String applicationId, Pageable pageable) {
        List<ClientCertificate> filtered = storage
            .stream()
            .filter(cert -> applicationId.equals(cert.applicationId()))
            .toList();

        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        // PageableImpl uses 1-based page numbering, handle both 0-based and 1-based
        int fromIndex = Math.max(0, (pageNumber - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        List<ClientCertificate> pageContent = fromIndex < filtered.size()
            ? filtered
                .subList(fromIndex, toIndex)
                .stream()
                .map(cert -> cert.toBuilder().build())
                .toList()
            : Collections.emptyList();

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
            .filter(cert -> applicationId.equals(cert.applicationId()))
            .filter(cert -> statusSet.contains(cert.status()))
            .map(cert -> cert.toBuilder().build())
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
            .filter(cert -> applicationIds.contains(cert.applicationId()))
            .filter(cert -> statusSet.contains(cert.status()))
            .map(cert -> cert.toBuilder().build())
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
            .map(cert -> cert.toBuilder().build())
            .collect(Collectors.toSet());
    }

    @Override
    public void deleteByApplicationId(String applicationId) {
        storage.removeIf(cert -> applicationId.equals(cert.applicationId()));
    }

    @Override
    public Optional<ClientCertificate> findMostRecentActiveByApplicationId(String applicationId) {
        return storage
            .stream()
            .filter(cert -> applicationId.equals(cert.applicationId()))
            .filter(cert -> cert.status() == ClientCertificateStatus.ACTIVE || cert.status() == ClientCertificateStatus.ACTIVE_WITH_END)
            .max(Comparator.comparing(ClientCertificate::createdAt))
            .map(cert -> cert.toBuilder().build());
    }

    @Override
    public void initWith(List<ClientCertificate> items) {
        storage.clear();
        items
            .stream()
            .map(cert -> cert.toBuilder().build())
            .forEach(storage::add);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ClientCertificate> storage() {
        return Collections.unmodifiableList(storage);
    }
}
