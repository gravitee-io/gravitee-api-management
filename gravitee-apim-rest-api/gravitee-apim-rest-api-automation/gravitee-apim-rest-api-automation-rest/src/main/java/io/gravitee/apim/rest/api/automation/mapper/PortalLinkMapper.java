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
        return fromSpec(spec, id, errors, audit, portalHrid, null);
    }

    default PortalLinkState toApiLinkState(PortalLinkSpec spec, String id, List<Validator.Error> errors, AuditInfo audit, String apiHrid) {
        return fromSpec(spec, id, errors, audit, null, apiHrid);
    }

    default PortalLinkState toPortalLinkState(PortalNavigationLink link, String hrid, List<Validator.Error> errors, String portalHrid) {
        return fromLink(link, hrid, errors, portalHrid, null);
    }

    default PortalLinkState toApiLinkState(PortalNavigationLink link, String hrid, List<Validator.Error> errors, String apiHrid) {
        return fromLink(link, hrid, errors, null, apiHrid);
    }

    /**
     * A link is attached to either a portal or an API, never both, so exactly one of
     * {@code portalHrid} / {@code apiHrid} is ever non-null. Nothing else about the two attachments
     * differs — mapping them through one body is what keeps a field added to {@link PortalLinkSpec}
     * from reaching one attachment and not the other.
     */
    private PortalLinkState fromSpec(
        PortalLinkSpec spec,
        String id,
        List<Validator.Error> errors,
        AuditInfo audit,
        String portalHrid,
        String apiHrid
    ) {
        var state = new PortalLinkState(id, audit.environmentId(), audit.organizationId(), toErrors(errors), portalHrid, apiHrid);
        state.setHrid(spec.getHrid());
        state.setName(spec.getName());
        state.setHref(spec.getHref());
        state.setLocation(spec.getLocation());
        state.setOrder(spec.getOrder());
        state.setVisibility(spec.getVisibility());
        return state;
    }

    /**
     * The persisted counterpart of {@link #fromSpec}. The hrid is passed in rather than read off the
     * link: it is the caller-supplied key the id and segment were derived from, and the nav item does
     * not carry it.
     */
    private PortalLinkState fromLink(
        PortalNavigationLink link,
        String hrid,
        List<Validator.Error> errors,
        String portalHrid,
        String apiHrid
    ) {
        var state = new PortalLinkState(
            link.getId() != null ? link.getId().toString() : null,
            link.getEnvironmentId(),
            link.getOrganizationId(),
            toErrors(errors),
            portalHrid,
            apiHrid
        );
        state.setHrid(hrid);
        state.setName(link.getTitle());
        state.setHref(link.getUrl());
        state.setLocation(link.getAutomationMetadata() != null ? link.getAutomationMetadata().location().orElse(null) : null);
        state.setOrder(link.getOrder());
        state.setVisibility(toWireVisibility(link.getVisibility()));
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
}
