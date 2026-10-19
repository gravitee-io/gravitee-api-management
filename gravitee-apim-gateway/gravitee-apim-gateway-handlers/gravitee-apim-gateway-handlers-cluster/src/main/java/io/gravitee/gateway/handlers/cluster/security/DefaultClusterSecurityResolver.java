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
package io.gravitee.gateway.handlers.cluster.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.gravitee.definition.model.cluster.ClusterSecurityResolutionException;
import io.gravitee.definition.model.cluster.ClusterSecurityResolver;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.secrets.api.el.FieldKind;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Evaluates every string of a cluster connection's security that holds an expression, with a template engine that only
 * knows the gateway-wide variables (the secrets among them) and none of an API or a request.
 *
 * <p>Each field is evaluated with the field kind the SSL options of the plugins give the same field with {@code @Secret}
 * ({@code PASSWORD} for passwords, {@code PRIVATE_KEY} and {@code PUBLIC_KEY} for PEM contents, {@code GENERIC} otherwise)
 * and its lower-cased path as name, so that secret specs restricted to a field kind or name apply to clusters too. Unlike a
 * plugin field without {@code @Secret}, every field of the security may reference a secret.</p>
 */
public class DefaultClusterSecurityResolver implements ClusterSecurityResolver {

    /** Expressions start with this marker: strings without it are returned as is, as the template engine would. */
    private static final String EXPRESSION_MARKER = "{#";
    private static final String SECRETS_VARIABLE = "#secrets.";

    /** Field names, lower-cased, by the {@code @Secret} field kind of the plugin SSL options and SASL credentials. */
    private static final Set<String> PASSWORD_FIELDS = Set.of("password", "keypassword");
    private static final Set<String> PRIVATE_KEY_FIELDS = Set.of("keycontent");
    private static final Set<String> PUBLIC_KEY_FIELDS = Set.of("certcontent");

    private final ObjectMapper objectMapper;
    private final Supplier<List<TemplateVariableProvider>> templateVariableProviders;

    public DefaultClusterSecurityResolver(ObjectMapper objectMapper, Supplier<List<TemplateVariableProvider>> templateVariableProviders) {
        this.objectMapper = objectMapper;
        this.templateVariableProviders = templateVariableProviders;
    }

    @Override
    public Single<SecurityConfig> resolve(SecurityConfig security) {
        if (security == null) {
            return Single.error(new IllegalArgumentException("No security to resolve"));
        }
        return Single.defer(() -> {
            JsonNode tree = objectMapper.valueToTree(security);
            List<TemplateVariableProvider> providers = templateVariableProviders.get();
            List<Completable> evaluations = new ArrayList<>();
            collect(tree, null, providers, evaluations);
            if (evaluations.isEmpty()) {
                return Single.just(security);
            }
            // One field at a time: the evaluations write into the same tree.
            return Completable.concat(evaluations).andThen(Single.fromCallable(() -> objectMapper.treeToValue(tree, SecurityConfig.class)));
        });
    }

    private void collect(JsonNode node, String path, List<TemplateVariableProvider> providers, List<Completable> evaluations) {
        if (node instanceof ObjectNode objectNode) {
            collectFields(objectNode, path, providers, evaluations);
        } else if (node instanceof ArrayNode arrayNode) {
            collectItems(arrayNode, path, providers, evaluations);
        }
    }

    private void collectFields(
        ObjectNode objectNode,
        String path,
        List<TemplateVariableProvider> providers,
        List<Completable> evaluations
    ) {
        for (Map.Entry<String, JsonNode> field : objectNode.properties()) {
            String fieldPath = path == null ? field.getKey() : path + "." + field.getKey();
            if (field.getValue() instanceof TextNode text) {
                evaluate(fieldPath, field.getKey(), text.textValue(), providers, resolved ->
                    objectNode.put(field.getKey(), resolved)
                ).ifPresent(evaluations::add);
            } else {
                collect(field.getValue(), fieldPath, providers, evaluations);
            }
        }
    }

    private void collectItems(ArrayNode arrayNode, String path, List<TemplateVariableProvider> providers, List<Completable> evaluations) {
        for (int i = 0; i < arrayNode.size(); i++) {
            int index = i;
            String itemPath = path + "[" + i + "]";
            if (arrayNode.get(i) instanceof TextNode text) {
                evaluate(itemPath, lastSegment(path), text.textValue(), providers, resolved -> arrayNode.set(index, resolved)).ifPresent(
                    evaluations::add
                );
            } else {
                collect(arrayNode.get(i), itemPath, providers, evaluations);
            }
        }
    }

    private Optional<Completable> evaluate(
        String path,
        String name,
        String value,
        List<TemplateVariableProvider> providers,
        Consumer<String> onResolved
    ) {
        if (value == null || !value.contains(EXPRESSION_MARKER)) {
            return Optional.empty();
        }
        SecretFieldAccessControl accessControl = new SecretFieldAccessControl(true, kindOf(name), path.toLowerCase(Locale.ROOT));
        return Optional.of(
            Single.defer(() -> {
                // One engine per field: the access control is a variable of the engine's context.
                TemplateEngine engine = TemplateEngine.templateEngine();
                providers.forEach(provider -> provider.provide(engine.getTemplateContext()));
                engine.getTemplateContext().setVariable(SecretFieldAccessControl.EL_VARIABLE, accessControl);
                return engine.eval(value, String.class).defaultIfEmpty("");
            })
                .doOnSuccess(onResolved::accept)
                .onErrorResumeNext(e -> Single.error(resolutionError(path, value, e)))
                .ignoreElement()
        );
    }

    /**
     * A literal credential may contain the expression marker: the failure then echoes the credential, so it is only
     * kept when the field holds a secret reference.
     */
    private static ClusterSecurityResolutionException resolutionError(String path, String value, Throwable error) {
        return new ClusterSecurityResolutionException(path, value.contains(SECRETS_VARIABLE) ? error : null);
    }

    private static FieldKind kindOf(String name) {
        String lowerCaseName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (PASSWORD_FIELDS.contains(lowerCaseName)) {
            return FieldKind.PASSWORD;
        }
        if (PRIVATE_KEY_FIELDS.contains(lowerCaseName)) {
            return FieldKind.PRIVATE_KEY;
        }
        if (PUBLIC_KEY_FIELDS.contains(lowerCaseName)) {
            return FieldKind.PUBLIC_KEY;
        }
        return FieldKind.GENERIC;
    }

    private static String lastSegment(String path) {
        return path == null ? null : path.substring(path.lastIndexOf('.') + 1);
    }
}
