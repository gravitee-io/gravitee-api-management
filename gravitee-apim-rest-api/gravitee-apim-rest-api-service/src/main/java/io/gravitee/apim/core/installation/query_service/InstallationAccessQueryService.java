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
package io.gravitee.apim.core.installation.query_service;

import io.gravitee.apim.core.installation.model.RestrictedDomain;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface InstallationAccessQueryService {
    List<String> getConsoleUrls(final String organizationId);

    String getConsoleUrl(final String organizationId);

    String getConsoleAPIUrl(final String organizationId);

    List<String> getPortalUrls(final String environmentId);

    String getPortalUrl(final String environmentId);

    String getPortalAPIUrl(final String environmentId);

    List<RestrictedDomain> getGatewayRestrictedDomains(final String environmentId);
}
