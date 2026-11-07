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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.DEFAULT_PORTAL_UPGRADER;

import io.gravitee.apim.core.portal.domain_service.DefaultPortalDomainService;
import io.gravitee.apim.core.portal.exception.SeveralPortalsInEnvironmentException;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Gives every environment its default portal and replaces the placeholder URL on every portal. It
 * refuses to change anything while an environment has several portals, which stops the start-up.
 */
@Component
@CustomLog
public class DefaultPortalUpgrader implements Upgrader {

    /**
     * The URL stored on portals created before they had a real one (the column is mandatory), replaced here by the
     * environment's portal URL.
     */
    static final URL URL_PLACEHOLDER = placeholderUrl();

    private final EnvironmentRepository environmentRepository;
    private final DefaultPortalDomainService defaultPortalDomainService;

    public DefaultPortalUpgrader(@Lazy EnvironmentRepository environmentRepository, DefaultPortalDomainService defaultPortalDomainService) {
        this.environmentRepository = environmentRepository;
        this.defaultPortalDomainService = defaultPortalDomainService;
    }

    @Override
    public int getOrder() {
        return DEFAULT_PORTAL_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        var environments = List.copyOf(environmentRepository.findAll());
        checkSinglePortalInEveryEnvironment(environments);
        for (var environment : environments) {
            // ParameterService memoizes parameters per thread by key only: clear the context around each
            // environment so none gets a portal URL read for another one, here or in an earlier upgrader.
            GraviteeContext.cleanContext();
            try {
                defaultPortalDomainService.ensureDefault(environment.getOrganizationId(), environment.getId());
                var replacedUrls = defaultPortalDomainService.replaceUrl(environment.getId(), URL_PLACEHOLDER);
                if (replacedUrls > 0) {
                    log.info("Placeholder URL replaced on {} portal(s) of environment [{}]", replacedUrls, environment.getId());
                }
            } finally {
                GraviteeContext.cleanContext();
            }
        }
        return true;
    }

    private void checkSinglePortalInEveryEnvironment(List<Environment> environments) {
        List<String> failures = new ArrayList<>();
        for (var environment : environments) {
            try {
                defaultPortalDomainService.checkSinglePortal(environment.getId());
            } catch (SeveralPortalsInEnvironmentException e) {
                failures.add(e.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("No default portal was created and no portal was changed. " + String.join("; ", failures));
        }
    }

    private static URL placeholderUrl() {
        try {
            return URI.create("https://example.com/to-be-implemented").toURL();
        } catch (MalformedURLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
