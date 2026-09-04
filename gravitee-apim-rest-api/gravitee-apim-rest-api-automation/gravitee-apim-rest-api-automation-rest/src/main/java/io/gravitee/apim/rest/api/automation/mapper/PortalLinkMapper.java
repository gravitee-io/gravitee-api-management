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
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.Errors;
import io.gravitee.apim.rest.api.automation.model.PortalLinkSpec;
import io.gravitee.apim.rest.api.automation.model.PortalLinkState;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface PortalLinkMapper {
    PortalLinkMapper INSTANCE = Mappers.getMapper(PortalLinkMapper.class);

    default PortalLinkState toPortalLinkState(
        PortalLinkSpec spec,
        String id,
        List<Validator.Error> errors,
        AuditInfo audit,
        String portalHrid
    ) {
        var state = new PortalLinkState(id, audit.environmentId(), audit.organizationId(), toErrors(errors), portalHrid, null);
        state.setHrid(spec.getHrid());
        state.setName(spec.getName());
        state.setHref(spec.getHref());
        state.setLocation(spec.getLocation());
        state.setOrder(spec.getOrder());
        state.setVisibility(spec.getVisibility());
        return state;
    }

    default PortalLinkState toApiLinkState(PortalLinkSpec spec, String id, List<Validator.Error> errors, AuditInfo audit, String apiHrid) {
        var state = new PortalLinkState(id, audit.environmentId(), audit.organizationId(), toErrors(errors), null, apiHrid);
        state.setHrid(spec.getHrid());
        state.setName(spec.getName());
        state.setHref(spec.getHref());
        state.setLocation(spec.getLocation());
        state.setOrder(spec.getOrder());
        state.setVisibility(spec.getVisibility());
        return state;
    }

    default PortalLinkState toPortalLinkState(PortalNavigationLink link, String hrid, String portalHrid) {
        var state = new PortalLinkState(
            link.getId() != null ? link.getId().toString() : null,
            link.getEnvironmentId(),
            link.getOrganizationId(),
            null,
            portalHrid,
            null
        );
        state.setHrid(hrid);
        state.setName(link.getTitle());
        state.setHref(link.getUrl());
        state.setOrder(link.getOrder());
        state.setLocation(link.getAutomationMetadata() != null ? link.getAutomationMetadata().location().orElse(null) : null);
        state.setVisibility(toWireVisibility(link.getVisibility()));
        return state;
    }

    default PortalLinkState toApiLinkState(PortalNavigationLink link, String hrid, String apiHrid) {
        var state = new PortalLinkState(
            link.getId() != null ? link.getId().toString() : null,
            link.getEnvironmentId(),
            link.getOrganizationId(),
            null,
            null,
            apiHrid
        );
        state.setHrid(hrid);
        state.setName(link.getTitle());
        state.setHref(link.getUrl());
        state.setOrder(link.getOrder());
        state.setLocation(link.getAutomationMetadata() != null ? link.getAutomationMetadata().location().orElse(null) : null);
        state.setVisibility(toWireVisibility(link.getVisibility()));
        return state;
    }

    default io.gravitee.apim.core.portal.model.PortalVisibility toDomainVisibility(
        io.gravitee.apim.rest.api.automation.model.PortalVisibility wire
    ) {
        return wire == null ? null : io.gravitee.apim.core.portal.model.PortalVisibility.valueOf(wire.getValue());
    }

    default io.gravitee.apim.rest.api.automation.model.PortalVisibility toWireVisibility(
        io.gravitee.apim.core.portal.model.PortalVisibility domain
    ) {
        return domain == null ? null : io.gravitee.apim.rest.api.automation.model.PortalVisibility.fromValue(domain.name());
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
}
