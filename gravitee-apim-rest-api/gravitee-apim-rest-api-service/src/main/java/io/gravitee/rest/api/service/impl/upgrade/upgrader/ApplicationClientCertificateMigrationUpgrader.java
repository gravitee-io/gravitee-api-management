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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_CERTIFICATE;

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Upgrader that migrates client certificates from application metadata to the ClientCertificate entity.
 *
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ApplicationClientCertificateMigrationUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ClientCertificateCrudService clientCertificateCrudService;

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            log.info("Starting migration of application client certificates to ClientCertificate entity");

            List<Application> applicationsWithCertificate = applicationRepository
                .findAll()
                .stream()
                .filter(
                    app ->
                        app.getMetadata() != null &&
                        app.getMetadata().containsKey(METADATA_CLIENT_CERTIFICATE) &&
                        app.getMetadata().get(METADATA_CLIENT_CERTIFICATE) != null &&
                        !app.getMetadata().get(METADATA_CLIENT_CERTIFICATE).isBlank()
                )
                .toList();

            log.info("Found {} applications with client certificates to migrate", applicationsWithCertificate.size());

            for (Application application : applicationsWithCertificate) {
                migrateApplicationCertificate(application);
            }

            log.info("Completed migration of application client certificates");
            return true;
        });
    }

    private void migrateApplicationCertificate(Application application) throws TechnicalException {
        String applicationId = application.getId();
        String applicationName = application.getName();
        String base64Certificate = application.getMetadata().get(METADATA_CLIENT_CERTIFICATE);

        try {
            // Decode the certificate from base64
            Optional<String> decodedCertificate = decodeCert(application, base64Certificate);

            if (decodedCertificate.isPresent()) {
                // Create a new ClientCertificate named after the Application
                clientCertificateCrudService.create(
                    applicationId,
                    new CreateClientCertificate(applicationName, null, null, decodedCertificate.get())
                );

                log.debug("Created ClientCertificate for application {} ({})", applicationName, applicationId);
            }

            // Remove the client_certificate metadata entry from the application and save it
            Map<String, String> updatedMetadata = new HashMap<>(application.getMetadata());
            updatedMetadata.remove(METADATA_CLIENT_CERTIFICATE);
            application.setMetadata(updatedMetadata);
            applicationRepository.update(application);
            log.debug("Removed client_certificate metadata from application {} ({})", application.getName(), application.getId());
        } catch (Exception e) {
            throw new TechnicalException(
                "Failed to migrate certificate for application %s (%s)".formatted(applicationName, applicationId),
                e
            );
        }
    }

    private static Optional<String> decodeCert(Application application, String base64Certificate) {
        try {
            return Optional.of(new String(Base64.getDecoder().decode(base64Certificate)));
        } catch (IllegalArgumentException e) {
            // Base64 decoding failed, skip certificate creation but still remove metadata
            log.warn(
                "Failed to decode base64 certificate for application {} ({}), skipping certificate creation but removing metadata",
                application.getName(),
                application.getId(),
                e
            );
        }
        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.APPLICATION_CLIENT_CERTIFICATE_MIGRATION_UPGRADER;
    }
}
