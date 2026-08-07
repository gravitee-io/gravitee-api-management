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

import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.scheduling.support.CronExpression;

public class PortalNavigationItemSourceDomainServiceInMemory implements PortalNavigationItemSourceDomainService {

    public static final String MARKDOWN = "# In memory markdown";
    public static final String SENSITIVE_DATA = "I'm a sensitive data";
    public static final String SENSITIVE_DATA_REPLACEMENT = "********";

    private final Set<PortalNavigationItemSource> notDueSources = Collections.newSetFromMap(new IdentityHashMap<>());

    private RuntimeException fetchFailure;
    private String lastValidatedConfiguration;

    public void failNextFetchWith(RuntimeException failure) {
        this.fetchFailure = failure;
    }

    /** Configuration the last validation was given, to assert what the plugin actually sees. */
    public String lastValidatedConfiguration() {
        return lastValidatedConfiguration;
    }

    @Override
    public String fetchContent(PortalNavigationItemSource source) {
        if (fetchFailure != null) {
            var failure = fetchFailure;
            fetchFailure = null;
            throw failure;
        }
        return MARKDOWN;
    }

    @Override
    public void removeSensitiveData(PortalNavigationItemSource source) {
        if (source.getSourceConfiguration() != null) {
            source.setSourceConfiguration(source.getSourceConfiguration().replace(SENSITIVE_DATA, SENSITIVE_DATA_REPLACEMENT));
        }
    }

    @Override
    public void mergeSensitiveData(PortalNavigationItemSource oldSource, PortalNavigationItemSource newSource) {
        if (oldSource == null || newSource == null) {
            return;
        }
        if (
            newSource.getSourceConfiguration().contains(SENSITIVE_DATA_REPLACEMENT) &&
            oldSource.getSourceConfiguration().contains(SENSITIVE_DATA)
        ) {
            newSource.setSourceConfiguration(newSource.getSourceConfiguration().replace(SENSITIVE_DATA_REPLACEMENT, SENSITIVE_DATA));
        }
    }

    @Override
    public void validateSourceConfiguration(PortalNavigationItemSource source) {
        this.lastValidatedConfiguration = source.getSourceConfiguration();
        if (source.getSourceConfiguration() != null && source.getSourceConfiguration().contains(SENSITIVE_DATA_REPLACEMENT)) {
            throw InvalidPortalNavigationItemSourceException.unresolvedSensitivePlaceholder("sensitiveData");
        }
        if (source.isUseAutoFetch() && source.getFetchCron() == null) {
            throw InvalidPortalNavigationItemSourceException.cronRequiredForAutoFetch();
        }
        if (source.getFetchCron() != null && !CronExpression.isValidExpression(source.getFetchCron())) {
            throw InvalidPortalNavigationItemSourceException.invalidCronExpression(source.getFetchCron());
        }
    }

    /**
     * Every auto-fetch source is due, unless the test marked it otherwise. Cron evaluation itself belongs
     * to the real implementation and is covered by its own test.
     */
    @Override
    public boolean isAutoFetchDue(PortalNavigationItemSource source) {
        return source.canUseAutoFetch() && !notDueSources.contains(source);
    }

    /** Marks a source as not yet due, i.e. its cron has not elapsed since the last fetch. */
    public void markAutoFetchNotDue(PortalNavigationItemSource source) {
        notDueSources.add(source);
    }
}
