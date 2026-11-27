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

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;

/**
 * The gateway resolves the {@code {#secrets.get(...)}} references of a definition with the rights of whoever deployed it,
 * so a save may only add references to Gravitee's secret store that its user could read: with {@code ENVIRONMENT_SECRET[READ]},
 * in the environment of the definition, and to the secret of a credential only for the owner of that credential or an
 * admin. A reference whose path the gateway works out per request can name any secret, so it needs the read permission too,
 * unless the text the path starts with already names another provider.
 * A definition may not use the {@code #secrets} variable in any other way, even where the previous one did: the gateway
 * rewrites each reference to its internal {@code fromGrant} and {@code fromEL} functions, which resolve any secret of the
 * environment that a deployed context was granted. A reference the definition already had at the same place, with the same
 * {@code enabled} state, is kept whoever saves it, so a save that moves or copies a reference, or enables the step or flow
 * around it, is checked again. An element of a list is placed by its id or name where that is unique among its siblings,
 * so a reorder keeps its references.
 */
@DomainService
@RequiredArgsConstructor
public class SecretReferencePermissionDomainService {

    // Mirrors the gateway's RefParser, which finds a reference anywhere in a string, plugin configurations included.
    private static final Pattern SECRETS_GET = Pattern.compile("\\{#secrets\\.get\\(.*?\\)}", Pattern.DOTALL);
    // SpEL reaches the gateway's internal functions through ?., [] and spaces as well as a plain dot.
    private static final Pattern INTERNAL_SECRETS_CALL = Pattern.compile("#secrets(?![\\w$])(?!\\.get\\().*?(\\)}|$)", Pattern.DOTALL);
    // A plugin configuration is JSON, so the gateway decodes an escape such as \\u0023 into # before it evaluates the expression.
    private static final Pattern JSON_UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
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
        if (!references(INTERNAL_SECRETS_CALL, definition).isEmpty()) {
            throw new InvalidSecretReferenceException(
                "This definition calls the gateway's internal secret functions. Reference a secret with {#secrets.get(...)} only."
            );
        }
        var stored = references(SECRETS_GET, previous);
        var added = references(SECRETS_GET, definition)
            .stream()
            .filter(reference -> !stored.contains(reference) && !stored.contains(reference.enabledAgain()))
            .map(Reference::expression)
            .collect(Collectors.toCollection(TreeSet::new));
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

    private Set<Reference> references(Pattern pattern, @Nullable Object definition) {
        var references = new HashSet<Reference>();
        if (definition != null) {
            collect(objectMapper.valueToTree(definition), pattern, "", true, references);
        }
        return references;
    }

    private void collect(JsonNode node, Pattern pattern, String pointer, boolean enabled, Set<Reference> references) {
        if (node.isTextual()) {
            collectEmbedded(node.textValue(), pattern, pointer, enabled, references);
        } else if (node instanceof POJONode pojo && pojo.getPojo() instanceof RawValue raw) {
            // A plugin configuration is a @JsonRawValue string, which valueToTree keeps unparsed.
            collectEmbedded(String.valueOf(raw.rawValue()), pattern, pointer, enabled, references);
        } else if (node.isArray()) {
            var places = placesOf(node);
            for (var i = 0; i < node.size(); i++) {
                collect(node.get(i), pattern, pointer + "/" + places.get(i), enabled, references);
            }
        } else {
            var nodeEnabled = enabled && node.path("enabled").asBoolean(true);
            node
                .properties()
                .forEach(field -> collect(field.getValue(), pattern, pointer + "/" + escaped(field.getKey()), nodeEnabled, references));
        }
    }

    // A plugin configuration is JSON, so each of its values gets its own place and a reference moved inside it counts as added.
    private void collectEmbedded(String text, Pattern pattern, String pointer, boolean enabled, Set<Reference> references) {
        embeddedJson(text).ifPresentOrElse(
            embedded -> collect(embedded, pattern, pointer, enabled, references),
            () -> collect(text, pattern, pointer, enabled, references)
        );
    }

    private Optional<JsonNode> embeddedJson(String text) {
        var trimmed = text.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readTree(trimmed)).filter(JsonNode::isContainerNode);
        } catch (JsonProcessingException e) {
            // An expression such as {#secrets.get(...)} starts with { too, and is scanned as text.
            return Optional.empty();
        }
    }

    private static List<String> placesOf(JsonNode array) {
        return IntStream.range(0, array.size())
            .mapToObj(index ->
                uniqueValue(array, index, "id")
                    .map(id -> "id:" + id)
                    .or(() -> uniqueValue(array, index, "name").map(name -> "name:" + name))
                    .orElse(String.valueOf(index))
            )
            .toList();
    }

    private static Optional<String> uniqueValue(JsonNode array, int index, String field) {
        var value = array.get(index).path(field);
        if (!value.isTextual() || value.textValue().isEmpty()) {
            return Optional.empty();
        }
        var count = 0;
        for (var element : array) {
            if (value.equals(element.path(field))) {
                count++;
            }
        }
        return count == 1 ? Optional.of(escaped(value.textValue())) : Optional.empty();
    }

    // RFC 6901 escaping, so a key or a name with a / cannot spell the place of another value.
    private static String escaped(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }

    private static void collect(String text, Pattern pattern, String pointer, boolean enabled, Set<Reference> references) {
        // A plugin configuration is JSON held in a string, so its quotes reach this text escaped.
        var unescaped = JSON_UNICODE_ESCAPE.matcher(text.replace("\\\"", "\"")).replaceAll(escape ->
            Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(escape.group(1), 16)))
        );
        var matcher = pattern.matcher(unescaped);
        while (matcher.find()) {
            references.add(new Reference(pointer, matcher.group(), enabled));
        }
    }

    private record Reference(String pointer, String expression, boolean enabled) {
        // The gateway does not resolve a reference in a disabled step or flow, so disabling a stored one needs no check.
        Reference enabledAgain() {
            return enabled ? this : new Reference(pointer, expression, true);
        }
    }
}
