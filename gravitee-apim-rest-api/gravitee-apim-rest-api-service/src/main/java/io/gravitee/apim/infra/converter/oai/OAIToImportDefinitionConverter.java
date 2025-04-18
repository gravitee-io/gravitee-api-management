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
package io.gravitee.apim.infra.converter.oai;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.rest.api.service.swagger.converter.extension.XGraviteeIODefinition;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;

public class OAIToImportDefinitionConverter {

    public static OAIToImportDefinitionConverter INSTANCE = new OAIToImportDefinitionConverter();
    private static final String X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION = "x-graviteeio-definition";
    private static final String PICTURE_REGEX = "^data:image/[\\w]+;base64,.*$";

    public ImportDefinition toImportDefinition(OpenAPI specification, Collection<? extends OAIOperationVisitor> visitors) {
        var xGraviteeIODefinition = getXGraviteeIODefinition(specification);
        var serverUrls = OAIServersConverter.INSTANCE.convert(specification.getServers());
        var importDefinitionBuilder = ImportDefinition.builder();
        var apiBuilder = ApiExport
            .builder()
            .name(specification.getInfo().getTitle())
            .description(
                StringUtils.isEmpty(specification.getInfo().getDescription())
                    ? "Description of " + specification.getInfo().getTitle()
                    : specification.getInfo().getDescription()
            )
            .definitionVersion(DefinitionVersion.V4)
            .apiVersion(specification.getInfo().getVersion())
            .type(ApiType.PROXY)
            .flows(OAIToFlowsConverter.INSTANCE.convert(specification, visitors))
            .listeners(
                OAIToListenersConverter.INSTANCE.convert(
                    xGraviteeIODefinition,
                    specification.getInfo().getTitle(),
                    serverUrls.isEmpty() ? null : serverUrls.get(0)
                )
            )
            .endpointGroups(OAIToEndpointGroupsConverter.INSTANCE.convert(specification.getServers(), serverUrls));

        if (xGraviteeIODefinition != null) {
            apiBuilder
                .categories(
                    CollectionUtils.isEmpty(xGraviteeIODefinition.getCategories())
                        ? Collections.emptySet()
                        : new HashSet<>(xGraviteeIODefinition.getCategories())
                )
                .labels(
                    CollectionUtils.isEmpty(xGraviteeIODefinition.getLabels()) ? Collections.emptyList() : xGraviteeIODefinition.getLabels()
                )
                .groups(
                    CollectionUtils.isEmpty(xGraviteeIODefinition.getGroups())
                        ? Collections.emptySet()
                        : new HashSet<>(xGraviteeIODefinition.getGroups())
                )
                .picture(
                    xGraviteeIODefinition.getPicture() != null &&
                        !StringUtils.isEmpty(xGraviteeIODefinition.getPicture()) &&
                        xGraviteeIODefinition.getPicture().matches(PICTURE_REGEX)
                        ? xGraviteeIODefinition.getPicture()
                        : null
                )
                .visibility(
                    xGraviteeIODefinition.getVisibility() != null ? Visibility.valueOf(xGraviteeIODefinition.getVisibility().name()) : null
                )
                .properties(OAIToPropertiesConverter.INSTANCE.convert(xGraviteeIODefinition.getProperties()))
                .tags(
                    CollectionUtils.isEmpty(xGraviteeIODefinition.getTags())
                        ? Collections.emptySet()
                        : new HashSet<>(xGraviteeIODefinition.getTags())
                );

            importDefinitionBuilder
                .metadata(OAIToMetadataConverter.INSTANCE.convert(xGraviteeIODefinition.getMetadata()))
                .plans(OAIToPlanConverter.INSTANCE.convert(xGraviteeIODefinition.getPlans()));
        }

        return importDefinitionBuilder.apiExport(apiBuilder.build()).build();
    }

    private XGraviteeIODefinition getXGraviteeIODefinition(OpenAPI specification) {
        if (specification.getExtensions() == null || specification.getExtensions().get(X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION) == null) {
            return null;
        }
        return new ObjectMapper()
            .convertValue(specification.getExtensions().get(X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION), XGraviteeIODefinition.class);
    }
}
