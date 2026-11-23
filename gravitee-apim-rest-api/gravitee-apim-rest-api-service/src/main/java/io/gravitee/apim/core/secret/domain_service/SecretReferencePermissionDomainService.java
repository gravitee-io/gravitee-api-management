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
package io.gravitee.apim.core.secret.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.credential.crud_service.CredentialCrudService;
import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.credential.service_provider.SecretExpressionParser;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.secret.exception.InvalidSecretReferenceException;
import io.gravitee.apim.core.secret.exception.SecretReferenceNotAllowedException;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * The gateway resolves the {@code {#secrets.get(...)}} references of a definition with the rights of whoever deployed it,
 * so a save may only add references to Gravitee's secret store that its user could read: with {@code ENVIRONMENT_SECRET[READ]},
 * in the environment of the definition, and to the secret of a credential only for the owner of that credential or an
 * admin. A reference whose path the gateway works out per request can name any secret, so it needs the read permission too.
 * The references a definition already had are kept, whoever saves it.
 */
@DomainService
@RequiredArgsConstructor
public class SecretReferencePermissionDomainService {

    // Mirrors the gateway's RefParser, which finds a reference anywhere in a string, plugin configurations included.
    private static final Pattern SECRETS_GET = Pattern.compile("\\{#secrets\\.get\\(.*?\\)}", Pattern.DOTALL);

    private final SecretExpressionParser expressionParser;
    private final PermissionDomainService permissionDomainService;
    private final CredentialCrudService credentialCrudService;
    private final ObjectMapper objectMapper;

    /**
     * @param previous the definition the save replaces, {@code null} on create
     * @param userId the user who saves, {@code null} for a system call, which skips the permission checks
     */
    public void validate(
        String organizationId,
        String environmentId,
        @Nullable String userId,
        @Nullable Object previous,
        Object definition
    ) {
        var added = references(definition);
        added.removeAll(references(previous));
        var needsSecretRead = false;
        for (String expression : added) {
            var path = literalPath(expression);
            if (path.isPresent() && !SecretReference.namesGraviteeStore(path.get())) {
                continue;
            }
            needsSecretRead = true;
            if (path.isPresent()) {
                validateStoreReference(organizationId, environmentId, userId, path.get());
            }
        }
        if (needsSecretRead && userId != null && !canReadSecrets(organizationId, environmentId, userId)) {
            throw new SecretReferenceNotAllowedException(
                "This definition references secrets that Gravitee stores, so saving it needs the secret read permission."
            );
        }
    }

    private void validateStoreReference(String organizationId, String environmentId, @Nullable String userId, String path) {
        SecretReference reference;
        try {
            reference = SecretReference.parse(path, path);
        } catch (InvalidCredentialException e) {
            throw new InvalidSecretReferenceException(e.getMessage());
        }
        if (!environmentId.equals(reference.graviteeEnvironmentId())) {
            throw new InvalidSecretReferenceException(
                "The secret reference " + path + " must name the environment of this definition, as ?envId=" + environmentId + "."
            );
        }
        reference.credentialId().ifPresent(credentialId -> requireCredentialOwner(organizationId, environmentId, userId, credentialId));
    }

    private void requireCredentialOwner(String organizationId, String environmentId, @Nullable String userId, String credentialId) {
        var credential = credentialCrudService
            .findById(organizationId, environmentId, credentialId)
            .orElseThrow(() ->
                new InvalidSecretReferenceException("The secret reference names credential [" + credentialId + "], which does not exist.")
            );
        if (
            userId != null &&
            !userId.equals(credential.ownerId()) &&
            !permissionDomainService.isAdmin(organizationId, environmentId, userId)
        ) {
            throw new SecretReferenceNotAllowedException(
                "Only the owner of credential [" + credentialId + "] or an admin can reference its secrets in a definition."
            );
        }
    }

    private boolean canReadSecrets(String organizationId, String environmentId, String userId) {
        return permissionDomainService.hasPermission(
            organizationId,
            userId,
            RolePermission.ENVIRONMENT_SECRET,
            environmentId,
            RolePermissionAction.READ
        );
    }

    private Optional<String> literalPath(String expression) {
        try {
            return expressionParser.literalPath(expression, expression);
        } catch (InvalidCredentialException e) {
            // Any other expression is evaluated per request, like a path read from context data.
            return Optional.empty();
        }
    }

    private Set<String> references(@Nullable Object definition) {
        var references = new TreeSet<String>();
        if (definition != null) {
            collect(objectMapper.valueToTree(definition), references);
        }
        return references;
    }

    private static void collect(JsonNode node, Set<String> references) {
        if (node.isTextual()) {
            // A plugin configuration is JSON held in a string, so its quotes reach this text escaped.
            var matcher = SECRETS_GET.matcher(node.textValue().replace("\\\"", "\""));
            while (matcher.find()) {
                references.add(matcher.group());
            }
        } else {
            node.forEach(child -> collect(child, references));
        }
    }
}
