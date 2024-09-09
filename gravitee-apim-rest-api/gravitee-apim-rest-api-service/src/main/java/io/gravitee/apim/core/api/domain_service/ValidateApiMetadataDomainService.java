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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.DuplicateApiMetadataKeyException;
import io.gravitee.apim.core.api.exception.DuplicateApiMetadataNameException;
import io.gravitee.apim.core.api.exception.InvalidApiMetadataValueException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.metadata.model.MetadataId;
import jakarta.mail.internet.InternetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@DomainService
public class ValidateApiMetadataDomainService {

    private final ApiMetadataQueryService metadataQueryService;
    private final MetadataCrudService metadataCrudService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApiMetadataDecoderDomainService apiMetadataDecoderDomainService;

    private final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public void validateUniqueKey(String apiId, String key) {
        this.metadataCrudService.findById(
                MetadataId.builder().key(key).referenceId(apiId).referenceType(Metadata.ReferenceType.API).build()
            )
            .ifPresent(m -> {
                throw new DuplicateApiMetadataKeyException(apiId, key);
            });
    }

    public void validateUniqueName(String environmentId, String apiId, String name) {
        this.metadataQueryService.findApiMetadata(environmentId, apiId)
            .values()
            .forEach(val -> {
                if (val.getName().equalsIgnoreCase(name)) {
                    throw new DuplicateApiMetadataNameException(apiId, name);
                }
            });
    }

    public void validateValueByFormat(Api api, String organizationId, String value, Metadata.MetadataFormat format) {
        var valueToCheck = Objects.nonNull(value) && value.startsWith("${") ? this.getDecodedValue(api, organizationId, value) : value;

        try {
            switch (format) {
                case URL:
                    new URL(valueToCheck);
                    break;
                case MAIL:
                    final InternetAddress email = new InternetAddress(valueToCheck);
                    email.validate();
                    break;
                case DATE:
                    SIMPLE_DATE_FORMAT.setLenient(false);
                    SIMPLE_DATE_FORMAT.parse(valueToCheck);
                    break;
                case NUMERIC:
                    Double.valueOf(valueToCheck);
                    break;
            }
        } catch (Exception e) {
            throw new InvalidApiMetadataValueException(value, format.name());
        }
    }

    private String getDecodedValue(Api api, String organizationId, String value) {
        var apiTemplate = new ApiMetadataDecoderDomainService.ApiMetadataDecodeContext(
            api,
            Map.of(), // Metadata cannot reference another metadata entry
            apiPrimaryOwnerDomainService.getApiPrimaryOwner(organizationId, api.getId())
        );

        return this.apiMetadataDecoderDomainService.decodeMetadataValue(value, apiTemplate);
    }
}
