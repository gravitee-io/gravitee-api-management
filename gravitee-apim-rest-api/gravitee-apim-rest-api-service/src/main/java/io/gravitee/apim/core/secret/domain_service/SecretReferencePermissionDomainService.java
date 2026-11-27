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
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.util.RawValue;
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
 * admin. A reference whose path the gateway works out per request can name any secret, so it needs the read permission too,
 * unless the text the path starts with already names another provider.
 * The references a definition already had are kept, whoever saves it.
 */
@DomainService
@RequiredArgsConstructor
public class SecretReferencePermissionDomainService {

    // Mirrors the gateway's RefParser, which finds a reference anywhere in a string, plugin configurations included.
    private static final Pattern SECRETS_GET = Pattern.compile("\\{#secrets\\.get\\(.*?\\)}", Pattern.DOTALL);
    private static final Pattern GATEWAY_TRIMMED = Pattern.compile("(^[ ']+)|([ ']+$)");
    private static final String SECRET_SCHEME = "secret://";

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
            var path = pathUnlessDynamic(expression);
            if (path.isEmpty()) {
                // A provider named in full by the leading text stays the provider whatever the request adds after it.
                var prefix = expressionParser.pathPrefix(expression);
                needsSecretRead |= prefix.isEmpty() || asStoreReference(prefix.get()).isPresent();
                continue;
            }
            var storeReference = asStoreReference(path.get());
            if (storeReference.isEmpty()) {
                continue;
            }
            needsSecretRead = true;
            validateStoreReference(organizationId, environmentId, userId, storeReference.get());
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

    /**
     * The gateway strips the spaces and quotes around a path, drops its first character and trims the provider name, so
     * a path only escapes these checks when the provider it names that way is clearly another one.
     *
     * @return the path as a reference to Gravitee's store, empty for a path of another provider
     */
    private static Optional<String> asStoreReference(String path) {
        var trimmed = GATEWAY_TRIMMED.matcher(path).replaceAll("").trim();
        var schemeLess = trimmed.startsWith(SECRET_SCHEME)
            ? trimmed.substring(SECRET_SCHEME.length())
            : trimmed.substring(Math.min(1, trimmed.length()));
        var providerEnd = schemeLess.indexOf('/');
        var provider = providerEnd < 0 ? "" : schemeLess.substring(0, providerEnd).trim();
        if (!provider.isEmpty() && !provider.equalsIgnoreCase(SecretReference.GRAVITEE_PROVIDER)) {
            return Optional.empty();
        }
        return Optional.of("/" + SecretReference.GRAVITEE_PROVIDER + "/" + schemeLess.substring(providerEnd + 1).trim());
    }

    private Optional<String> pathUnlessDynamic(String expression) {
        try {
            return expressionParser.literalPath(expression, expression);
        } catch (InvalidCredentialException e) {
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
            collect(node.textValue(), references);
        } else if (node instanceof POJONode pojo && pojo.getPojo() instanceof RawValue raw) {
            // A plugin configuration is a @JsonRawValue string, which valueToTree keeps unparsed.
            collect(String.valueOf(raw.rawValue()), references);
        } else {
            node.forEach(child -> collect(child, references));
        }
    }

    private static void collect(String text, Set<String> references) {
        // A plugin configuration is JSON held in a string, so its quotes reach this text escaped.
        var matcher = SECRETS_GET.matcher(text.replace("\\\"", "\""));
        while (matcher.find()) {
            references.add(matcher.group());
        }
    }
}
