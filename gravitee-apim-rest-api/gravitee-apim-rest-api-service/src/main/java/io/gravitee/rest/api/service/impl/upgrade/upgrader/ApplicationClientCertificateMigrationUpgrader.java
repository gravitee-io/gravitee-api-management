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

import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.AbstractClientCertificateException;
import io.gravitee.rest.api.service.exceptions.ClientCertificateInvalidException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
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

    static final String METADATA_CLIENT_CERTIFICATE = "client_certificate";

    @Lazy
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ClientCertificateCrudService clientCertificateCrudService;

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            log.info("Starting migration of application client certificates to ClientCertificate entity");

            ApplicationCriteria criteria = ApplicationCriteria.builder().build();
            int pageNumber = 0;
            Page<Application> page;
            do {
                Pageable pageable = new PageableBuilder().pageNumber(pageNumber + 1).pageSize(100).build();
                page = applicationRepository.search(criteria, pageable);
                for (Application application : page
                    .getContent()
                    .stream()
                    .filter(
                        app ->
                            app.getMetadata() != null &&
                            app.getMetadata().get(METADATA_CLIENT_CERTIFICATE) != null &&
                            !app.getMetadata().get(METADATA_CLIENT_CERTIFICATE).isBlank()
                    )
                    .toList()) {
                    // candidate to be migrated
                    migrateApplicationCertificate(application);
                }
                pageNumber++;
            } while (page.getTotalElements() > pageNumber * page.getPageElements());

            log.info("Completed migration of application client certificates");
            return true;
        });
    }

    private void migrateApplicationCertificate(Application application) throws TechnicalException {
        String applicationId = application.getId();
        String applicationName = application.getName();
        String base64Certificate = application.getMetadata().get(METADATA_CLIENT_CERTIFICATE);

        try {
            // Set the application environment id in the context
            GraviteeContext.setCurrentEnvironment(application.getEnvironmentId());

            // Create a new ClientCertificate named after the Application

            clientCertificateCrudService.create(
                applicationId,
                ClientCertificate.builder().name(applicationName).certificate(decodeCert(base64Certificate)).build()
            );

            log.debug("Created ClientCertificate for application {} ({})", applicationName, applicationId);
        } catch (AbstractClientCertificateException e) {
            log.warn(
                "Failed to decode or parse certificate for application {} ({}), skipping certificate creation but removing metadata",
                application.getName(),
                application.getId(),
                e
            );
        } catch (Exception e) {
            throw new TechnicalException(
                "Failed to migrate certificate for application %s (%s)".formatted(applicationName, applicationId),
                e
            );
        } finally {
            GraviteeContext.cleanContext();
        }

        // Remove the client_certificate metadata entry from the application and save it
        Map<String, String> updatedMetadata = new HashMap<>(application.getMetadata());
        updatedMetadata.remove(METADATA_CLIENT_CERTIFICATE);
        application.setMetadata(updatedMetadata);
        applicationRepository.update(application);
        log.debug("Removed client_certificate metadata from application {} ({})", application.getName(), application.getId());
    }

    private static String decodeCert(String base64Certificate) {
        try {
            return new String(Base64.getDecoder().decode(base64Certificate));
        } catch (IllegalArgumentException e) {
            throw new ClientCertificateInvalidException();
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.APPLICATION_CLIENT_CERTIFICATE_MIGRATION_UPGRADER;
    }
}
