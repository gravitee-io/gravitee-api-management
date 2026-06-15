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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.DocumentationSpec;
import io.gravitee.apim.rest.api.automation.model.DocumentationState;
import io.gravitee.apim.rest.api.automation.model.Errors;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface ApiDocumentationMapper {
    ApiDocumentationMapper INSTANCE = Mappers.getMapper(ApiDocumentationMapper.class);

    default DocumentationState toDocumentationState(
        DocumentationSpec spec,
        String id,
        List<Validator.Error> errors,
        AuditInfo audit,
        String apiHrid
    ) {
        var state = new DocumentationState(id, audit.environmentId(), audit.organizationId(), toErrors(errors), null, apiHrid);
        state.setHrid(spec.getHrid());
        state.setName(spec.getName());
        state.setType(spec.getType());
        state.setContent(spec.getContent());
        state.setLocation(spec.getLocation());
        state.setOrder(spec.getOrder());
        return state;
    }

    default DocumentationState toDocumentationState(PortalPageContent<?> pageContent, String hrid, String apiHrid) {
        var meta = pageContent.getAutomationMetadata();
        String rawContent = switch (pageContent) {
            case GraviteeMarkdownPageContent gmd -> gmd.getContent().value();
            case OpenApiPageContent oapi -> oapi.getContent().value();
            case AsyncApiPageContent aapi -> aapi.getContent().value();
        };
        var state = new DocumentationState(
            pageContent.getId() != null ? pageContent.getId().toString() : null,
            pageContent.getEnvironmentId(),
            pageContent.getOrganizationId(),
            null,
            null,
            apiHrid
        );
        state.setHrid(hrid);
        state.setName(meta.name());
        state.setType(toWireType(pageContent.getType()));
        state.setContent(rawContent);
        state.setLocation(meta.location().orElse(null));
        state.setOrder(meta.order().orElse(null));
        return state;
    }

    default Errors toErrors(List<Validator.Error> validationErrors) {
        if (validationErrors == null || validationErrors.isEmpty()) {
            return null;
        }
        var wire = new Errors();
        wire.setSevere(validationErrors.stream().filter(Validator.Error::isSevere).map(Validator.Error::getMessage).toList());
        wire.setWarning(validationErrors.stream().filter(Validator.Error::isWarning).map(Validator.Error::getMessage).toList());
        return wire;
    }

    default PortalPageContentType toDomainType(io.gravitee.apim.rest.api.automation.model.DocumentationType wire) {
        return wire == null ? null : PortalPageContentType.valueOf(wire.getValue());
    }

    default io.gravitee.apim.rest.api.automation.model.DocumentationType toWireType(PortalPageContentType domain) {
        return domain == null ? null : io.gravitee.apim.rest.api.automation.model.DocumentationType.fromValue(domain.name());
    }
}
