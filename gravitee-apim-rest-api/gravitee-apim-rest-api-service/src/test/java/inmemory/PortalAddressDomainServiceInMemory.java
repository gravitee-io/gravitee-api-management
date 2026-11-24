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

import io.gravitee.apim.core.portal.domain_service.PortalAddressDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalUrlDomainService;

/** The real service over in-memory portals and access points. */
public class PortalAddressDomainServiceInMemory extends PortalAddressDomainService {

    private final AccessPointQueryServiceInMemory accessPoints;
    private final InstallationTypeDomainServiceInMemory installationType;

    public PortalAddressDomainServiceInMemory(PortalCrudServiceInMemory portals, InstallationAccessQueryServiceInMemory installation) {
        this(portals, installation, new AccessPointQueryServiceInMemory(), new InstallationTypeDomainServiceInMemory());
    }

    private PortalAddressDomainServiceInMemory(
        PortalCrudServiceInMemory portals,
        InstallationAccessQueryServiceInMemory installation,
        AccessPointQueryServiceInMemory accessPoints,
        InstallationTypeDomainServiceInMemory installationType
    ) {
        super(
            portals,
            accessPoints,
            new AccessPointCrudServiceInMemory(accessPoints),
            installation,
            new PortalUrlDomainService(installation),
            installationType
        );
        this.accessPoints = accessPoints;
        this.installationType = installationType;
    }

    public AccessPointQueryServiceInMemory accessPoints() {
        return accessPoints;
    }

    public InstallationTypeDomainServiceInMemory installationType() {
        return installationType;
    }
}
