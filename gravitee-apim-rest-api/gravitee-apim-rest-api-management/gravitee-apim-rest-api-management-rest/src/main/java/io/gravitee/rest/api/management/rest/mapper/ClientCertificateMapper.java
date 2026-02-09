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
package io.gravitee.rest.api.management.rest.mapper;

import io.gravitee.apim.core.application_certificate.model.ClientCertificate;
import io.gravitee.apim.core.application_certificate.model.ClientCertificateStatus;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.UpdateClientCertificate;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for converting between domain model and DTOs for client certificates.
 *
 * @author GraviteeSource Team
 */
@Mapper
public interface ClientCertificateMapper {
    ClientCertificateMapper INSTANCE = Mappers.getMapper(ClientCertificateMapper.class);

    io.gravitee.rest.api.model.clientcertificate.ClientCertificate toDto(ClientCertificate domain);

    ClientCertificate toDomain(CreateClientCertificate dto);

    ClientCertificate toDomain(UpdateClientCertificate dto);

    Page<io.gravitee.rest.api.model.clientcertificate.ClientCertificate> map(Page<ClientCertificate> clientCertificatePage);
}
