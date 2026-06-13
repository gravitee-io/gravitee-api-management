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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.async_api.AsyncApi;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_documentation.domain_service.ValidatePortalDocumentationDomainService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateOrUpdatePortalDocumentationUseCase {

    private final ValidatePortalDocumentationDomainService validator;
    private final PortalPageContentCrudService portalPageContentCrudService;
    private final PortalPageContentQueryService portalPageContentQueryService;
    private final PortalDocumentationSyncDomainService syncDomainService;

    public record Input(
        AuditInfo auditInfo,
        PortalPageContentId portalPageContentId,
        PortalId portalId,
        String name,
        PortalPageContentType type,
        String content,
        String location,
        Integer order
    ) {}

    public record Output(PortalPageContentId id, List<Validator.Error> errors) {}

    public Output execute(Input input) {
        var validation = validator.validateAndSanitize(
            new ValidatePortalDocumentationDomainService.Input(
                input.auditInfo(),
                input.portalPageContentId(),
                input.portalId(),
                input.name(),
                input.type(),
                input.content(),
                input.location(),
                input.order()
            )
        );

        validation
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var warnings = validation.warning().orElseGet(List::of);
        var sanitized = validation.value().orElseThrow(() -> new ValidationDomainException("Unable to sanitize portal documentation"));

        var meta = new AutomationMetadata(
            AutomationMetadata.ReferenceType.PORTAL,
            sanitized.portalId().toString(),
            sanitized.name(),
            Optional.ofNullable(sanitized.location()),
            Optional.ofNullable(sanitized.order())
        );

        var existing = portalPageContentQueryService.findById(sanitized.portalPageContentId());
        PortalPageContent<?> saved;
        if (existing.isPresent()) {
            var current = existing.get();
            if (current.getType() != sanitized.type()) {
                portalPageContentCrudService.delete(current.getId());
                saved = portalPageContentCrudService.create(buildNew(sanitized, meta));
            } else {
                current.update(new UpdatePortalPageContent(sanitized.content(), null), meta);
                saved = portalPageContentCrudService.update(current);
            }
        } else {
            saved = portalPageContentCrudService.create(buildNew(sanitized, meta));
        }

        syncDomainService.materialize(input.auditInfo(), saved);

        return new Output(saved.getId(), warnings);
    }

    private PortalPageContent<?> buildNew(ValidatePortalDocumentationDomainService.Input sanitized, AutomationMetadata meta) {
        var id = sanitized.portalPageContentId();
        var orgId = sanitized.auditInfo().organizationId();
        var envId = sanitized.auditInfo().environmentId();
        return switch (sanitized.type()) {
            case GRAVITEE_MARKDOWN -> new GraviteeMarkdownPageContent(id, orgId, envId, GraviteeMarkdown.of(sanitized.content()), meta);
            case OPENAPI -> new OpenApiPageContent(id, orgId, envId, OpenApi.of(sanitized.content()), new RedocConfiguration(), meta);
            case ASYNCAPI -> new AsyncApiPageContent(id, orgId, envId, AsyncApi.of(sanitized.content()), meta);
        };
    }
}
