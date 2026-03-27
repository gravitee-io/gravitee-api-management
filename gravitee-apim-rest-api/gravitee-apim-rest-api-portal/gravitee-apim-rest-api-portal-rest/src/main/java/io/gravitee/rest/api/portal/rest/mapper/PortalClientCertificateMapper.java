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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.rest.api.portal.rest.model.CreatePortalClientCertificateInput;
import io.gravitee.rest.api.portal.rest.model.PortalClientCertificate;
import io.gravitee.rest.api.portal.rest.model.PortalClientCertificateStatus;
import io.gravitee.rest.api.portal.rest.model.UpdatePortalClientCertificateInput;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for converting between domain ClientCertificate and portal REST DTOs.
 *
 * @author GraviteeSource Team
 */
@Mapper(uses = DateMapper.class)
public interface PortalClientCertificateMapper {
    PortalClientCertificateMapper INSTANCE = Mappers.getMapper(PortalClientCertificateMapper.class);
    PortalClientCertificate toDto(ClientCertificate domain);

    List<PortalClientCertificate> toDto(List<ClientCertificate> domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "crossId", ignore = true)
    @Mapping(target = "applicationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "certificateExpiration", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "issuer", ignore = true)
    @Mapping(target = "fingerprint", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    ClientCertificate toDomain(CreatePortalClientCertificateInput input);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "crossId", ignore = true)
    @Mapping(target = "applicationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "certificate", ignore = true)
    @Mapping(target = "certificateExpiration", ignore = true)
    @Mapping(target = "subject", ignore = true)
    @Mapping(target = "issuer", ignore = true)
    @Mapping(target = "fingerprint", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    ClientCertificate toDomain(UpdatePortalClientCertificateInput input);

    default PortalClientCertificateStatus toPortalStatus(ClientCertificateStatus status) {
        if (status == null) return null;
        return PortalClientCertificateStatus.fromValue(status.name());
    }
}
