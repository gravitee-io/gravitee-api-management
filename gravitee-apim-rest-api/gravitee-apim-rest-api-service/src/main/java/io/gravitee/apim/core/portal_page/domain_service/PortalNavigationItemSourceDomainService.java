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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import java.util.List;

public interface PortalNavigationItemSourceDomainService {
    String fetchContent(PortalNavigationItemSource source);

    boolean supportsFileListing(PortalNavigationItemSource source);

    List<String> listFiles(PortalNavigationItemSource source);

    String fetchFileContent(PortalNavigationItemSource source, String filepath);

    void removeSensitiveData(PortalNavigationItemSource source);

    void mergeSensitiveData(PortalNavigationItemSource oldSource, PortalNavigationItemSource newSource);

    void validateSourceConfiguration(PortalNavigationItemSource source);

    /**
     * Whether the auto-fetch cron has elapsed since the last fetch attempt, successful or not.
     *
     * @return {@code false} when auto-fetch is off, when no cron is configured, or when the cron cannot be parsed
     */
    boolean isAutoFetchDue(PortalNavigationItemSource source);
}
