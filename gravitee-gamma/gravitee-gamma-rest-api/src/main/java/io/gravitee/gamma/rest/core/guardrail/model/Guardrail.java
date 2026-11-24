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
package io.gravitee.gamma.rest.core.guardrail.model;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A Guardrail: a reusable sequence of guardrail policies and the resources they need, defined at environment
 * level, deployed on its own and attached to APIs.
 *
 * <p>What a Guardrail <em>is</em> lives in its definition — the model the gateway reads — and this
 * class reads the rest through it rather than keeping a second copy, so the two cannot drift apart.
 *
 * <p>Four things are its own, because the gateway has no use for them: the description the console
 * shows, the lifecycle state, the deployment stamp, and the two timestamps. The lifecycle state,
 * {@code deployedAt} and {@code deployedRevisionId} are derived from the latest publication of the Guardrail
 * whenever it is read; the values stored with it are never relied upon.
 *
 * <p>The definition field is spelled out rather than imported: a domain class named {@code Guardrail}
 * cannot import another one.
 *
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Guardrail {

    private io.gravitee.definition.model.v4.guardrail.Guardrail definition;

    private String description;
    private GuardrailLifecycleState lifecycleState;
    private Instant deployedAt;
    private String deployedRevisionId;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * How many times the draft was written, starting at 1. A writer states the one it read, and its write
     * is refused when the draft moved on since. The Guardrails stored before the counter existed were
     * given version 1 by the upgrade; {@code null} is left to one written since by a node older than the
     * counter, until its next write.
     *
     * <p>A deployment does not count: it publishes the draft without changing it.
     */
    private Integer version;

    /**
     * The Guardrail a creation payload describes, scoped to the environment it is created in — which
     * comes from the path, never from the payload.
     *
     * <p>The name is stripped here as well as in {@code CreateGuardrailUseCase}, and both are needed: the
     * use case runs it before the payload's own constraints, so {@code @NotBlank} and {@code @Size} judge
     * the stored value, while this one keeps the invariant for any other caller of the model. The
     * operation is idempotent, so running it twice costs a comparison.
     */
    public static Guardrail from(CreateGuardrail createGuardrail, String environmentId) {
        return Guardrail.builder()
            .definition(
                createGuardrail
                    .toDefinition()
                    .toBuilder()
                    .environmentId(environmentId)
                    .name(withoutOuterWhitespace(createGuardrail.getName()))
                    .build()
            )
            .description(createGuardrail.getDescription())
            .build();
    }

    /**
     * Replaces the definition and the description wholesale, the way a {@code PUT} does, and counts the
     * write. The identity
     * comes from the path and the deployment stamp from a deployment, so neither is the caller's to
     * send: the first is carried over explicitly, the second simply survives in the builder.
     */
    public Guardrail update(UpdateGuardrailCommand command) {
        return this.toBuilder()
            .definition(
                command
                    .getDefinition()
                    .toBuilder()
                    .id(getId())
                    .environmentId(getEnvironmentId())
                    .name(withoutOuterWhitespace(command.getDefinition().getName()))
                    .build()
            )
            .description(command.getDescription())
            .updatedAt(TimeProvider.instantNow())
            .version(version == null ? 1 : version + 1)
            .build();
    }

    /**
     * The name as it is kept. Both writes assemble the stored Guardrail here, so this is where the
     * uniqueness check reads it from.
     *
     * <p>Neither {@code trim} nor {@code strip} is enough on their own. {@code trim} stops at
     * {@code U+0020}, and {@code strip} follows {@link Character#isWhitespace}, which deliberately
     * excludes the non-breaking spaces — {@code U+00A0} above all, which is what a name pasted from a
     * page or a document carries. Either way the padded name reads as the same name and compares as a
     * different one, which is the whole defect. Space characters are therefore taken too.
     *
     * <p>Zero-width and other format characters are left alone: they are not spaces, and a name
     * carrying one is a different name rather than a padded one.
     *
     * <p>Blank once stripped is refused upstream by {@code @NotBlank}, on the creation payload and on
     * the definition, so this only ever shortens a name.
     */
    public static String withoutOuterWhitespace(String name) {
        if (name == null) {
            return null;
        }
        int start = 0;
        int end = name.length();
        while (start < end && isOuterSpace(name.codePointAt(start))) {
            start += Character.charCount(name.codePointAt(start));
        }
        while (end > start && isOuterSpace(name.codePointBefore(end))) {
            end -= Character.charCount(name.codePointBefore(end));
        }
        return name.substring(start, end);
    }

    private static boolean isOuterSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    public String getId() {
        return definition == null ? null : definition.getId();
    }

    public String getEnvironmentId() {
        return definition == null ? null : definition.getEnvironmentId();
    }

    public String getName() {
        return definition == null ? null : definition.getName();
    }

    public Phase getPhase() {
        return definition == null ? null : definition.getPhase();
    }

    public Set<ApiType> getApiTypes() {
        return definition == null ? null : definition.getApiTypes();
    }

    public List<Step> getPolicies() {
        return definition == null ? null : definition.getPolicies();
    }

    public List<Resource> getResources() {
        return definition == null ? null : definition.getResources();
    }

    /** Whether this Guardrail runs on the same phase of the same API types as another one. */
    public boolean runsWhere(Guardrail other) {
        return runsWhere(other.getPhase(), other.getApiTypes());
    }

    /** Whether this Guardrail runs where a definition of it — a published revision, say — runs. */
    public boolean runsWhere(io.gravitee.definition.model.v4.guardrail.Guardrail other) {
        return runsWhere(other.getPhase(), other.getApiTypes());
    }

    private boolean runsWhere(Phase phase, Set<ApiType> apiTypes) {
        return getPhase() == phase && orEmpty(getApiTypes()).equals(orEmpty(apiTypes));
    }

    private static Set<ApiType> orEmpty(Set<ApiType> apiTypes) {
        return apiTypes == null ? Set.of() : apiTypes;
    }

    public boolean isDeployed() {
        return lifecycleState == GuardrailLifecycleState.DEPLOYED;
    }

    /**
     * The steps of the sequence the gateways would run: enabled, and present — a sequence may be
     * absent, and a caller building a definition by hand may leave a hole in it.
     */
    public Stream<Step> enabledPolicies() {
        return Objects.requireNonNullElse(getPolicies(), List.<Step>of()).stream().filter(Objects::nonNull).filter(Step::isEnabled);
    }

    /**
     * Whether at least one enabled step of the sequence runs a guardrail policy.
     *
     * <p>Which policies are guardrail policies is the caller's to say: the installed plugins answer that, and
     * this model knows nothing of them.
     *
     * @param isGuardrailPolicy whether the policy a step names is a guardrail policy
     */
    public boolean hasEnabledGuardrailPolicy(Predicate<String> isGuardrailPolicy) {
        return enabledPolicies().anyMatch(step -> isGuardrailPolicy.test(step.getPolicy()));
    }

    /** The enabled resource of this name, the one the gateway would hand a policy naming it. */
    public Optional<Resource> enabledResourceNamed(String name) {
        return resourcesNamed(name).filter(Resource::isEnabled).findFirst();
    }

    /** Whether the Guardrail holds a resource of this name, enabled or not. */
    public boolean holdsResourceNamed(String name) {
        return resourcesNamed(name).findAny().isPresent();
    }

    private Stream<Resource> resourcesNamed(String name) {
        return Objects.requireNonNullElse(getResources(), List.<Resource>of())
            .stream()
            .filter(Objects::nonNull)
            .filter(resource -> name.equals(resource.getName()));
    }
}
