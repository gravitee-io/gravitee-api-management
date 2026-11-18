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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianWithoutEnabledGuardrailException;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Validates Guardian structure, supported API types and phase, and the policies its steps reference.
 *
 * <p><strong>Structure</strong> — the required fields, the field sizes and the shape of the id —
 * is declared as bean validation constraints on {@link CreateGuardian} and
 * {@link UpdateGuardianCommand}, and run here <em>explicitly</em>. Spring method validation is not
 * wired in this product, so a {@code @Valid} on a use-case parameter would be inert; running the
 * validator by hand is what makes the constraints hold for every caller of the use case rather than
 * only for one arriving through JAX-RS. {@code RemoteApiDefinitionParser} does the same in
 * management v2, for the same reason. The {@link ConstraintViolationException} it throws already has
 * a mapper registered by {@code GammaModuleApplication}, so it answers 400.
 *
 * <p><strong>The product rule</strong> — the gateway only runs a Guardian on an {@code llm-proxy} in
 * 4.14, so accepting any other API type would store a Guardian nothing can execute. A Guardian also
 * needs a phase.
 *
 * <p><strong>The steps</strong> — every enabled step, guardrail or not, must reference an installed
 * policy whose plugin declares the Guardian phase in its {@code guardian} flow, and carry a
 * configuration that policy's schema accepts. A guardrail must also support the Guardian API types, in
 * its plugin metadata and in any explicit scope protocol: the {@code guardian} flow says where it
 * runs, its API type flows only which API types it applies to.
 *
 * <p>These rules apply to creation and to update, a {@code PUT} replacing the whole definition, and
 * again to the stored draft when it is deployed: a draft may predate a rule, or have been written
 * against policies that have changed since, and a deployment turns it into a revision the gateways
 * must be able to start. One rule runs there only, {@link #validateDeployable(Guardian)} — the
 * sequence must hold an enabled guardrail — so a draft may be saved empty and still be refused at
 * deployment. What separates the two passes is the configuration a policy accepts only corrected: a
 * write path stores it, a deployment or a rollback refuses the step.
 *
 * <p><strong>The name is free</strong> — two Guardians of one environment may not share a name. This
 * is the check users meet, and the only one that reads the store; the unique index behind it is a
 * backstop for a race, not the enforcement, and it cannot be relied on for the case-insensitive rule
 * nor on a store that predates it.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateGuardianDomainService implements Validator<ValidateGuardianDomainService.Input> {

    private static final jakarta.validation.Validator BEAN_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ApiType SUPPORTED_API_TYPE = ApiType.LLM_PROXY;

    /** What a step carrying no configuration is judged as, so a schema requiring a property refuses it. */
    private static final String EMPTY_CONFIGURATION = "{}";

    private final GuardianRepository guardianRepository;
    private final PolicyPluginProvider policyPluginProvider;
    private final PolicyValidationDomainService policyValidation;
    private final JsonDeserializer jsonDeserializer;

    /**
     * What a path does with the configuration a policy accepts when it is not the one it was given.
     *
     * <p>The schema validation corrects a document before accepting it — it drops null entries, injects
     * the default of a missing required property, prunes what a closed schema forbids — so what a policy
     * accepts is not always what was submitted.
     */
    private enum Correction {
        /** Store it: on a write path, what is stored has to be what the policy validated. */
        STORE,
        /**
         * Refuse the step: a path that publishes or restores a stored definition may not rewrite it, and
         * publishing a configuration the policy was never validated with is the bug AIAM-1750 reports.
         */
        REFUSE,
    }

    public record Input(Guardian guardian) implements Validator.Input {}

    /** Refuses a creation payload whose shape the store would not accept. */
    public void validateStructure(CreateGuardian command) {
        refuseViolations(command);
    }

    /** Refuses an update payload whose shape the store would not accept. */
    public void validateStructure(UpdateGuardianCommand command) {
        refuseViolations(command);
    }

    /**
     * Refuses an assembled definition the gateway could not read.
     *
     * <p>Checked here rather than by cascading from the commands, because a definition is only
     * complete once the identity has been merged in: the id and the environment come from the path,
     * and the definition declares both {@code @NotBlank}. Cascading from an update command would
     * therefore refuse every legitimate update. Running it on the result instead keeps one invariant
     * true on both paths — what reaches the store is a definition the gateway can read.
     */
    public void validateDefinition(io.gravitee.definition.model.v4.guardian.Guardian definition) {
        refuseViolations(definition);
        refuseAnOverlongName(definition);
    }

    /**
     * The length the guardians table enforces, checked on the assembled definition because both writes
     * pass through it and the creation payload's own {@code @Size} only covers one of them.
     *
     * <p>Here rather than as an annotation on the definition model: that model is shared with the
     * gateway. Raising the limit belongs with the column.
     */
    private static void refuseAnOverlongName(io.gravitee.definition.model.v4.guardian.Guardian definition) {
        var name = definition.getName();
        if (name != null && name.length() > CreateGuardian.NAME_MAX_LENGTH) {
            throw new ValidationDomainException(CreateGuardian.NAME_TOO_LONG);
        }
    }

    /**
     * Refuses a name another Guardian of the environment already carries, ignoring case.
     *
     * @param guardianIdToExclude the Guardian being written, so renaming one to the name it already
     *     has is not read as a clash with itself. On a creation it is the id about to be used, which
     *     no stored Guardian carries — a caller-supplied id that is taken is refused on its own, by
     *     the store.
     */
    public void validateNameIsAvailable(String environmentId, String name, String guardianIdToExclude) {
        // every match, then exclude this one: with a single row, "the only match is me" and "one of the
        // matches is me" are indistinguishable, and the answer would depend on which row came back
        boolean takenByAnother = guardianRepository
            .findByEnvironmentIdAndName(environmentId, name)
            .stream()
            .anyMatch(existing -> !existing.getId().equals(guardianIdToExclude));
        if (takenByAnother) {
            throw new GuardianNameAlreadyExistsException(name);
        }
    }

    private static <C> void refuseViolations(C command) {
        var violations = BEAN_VALIDATOR.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    /**
     * Refuses a Guardian that would enforce nothing: no enabled step of its sequence runs a guardrail
     * the platform has installed.
     *
     * <p>Run when a Guardian is deployed, not when it is saved — a draft is expected to be empty while
     * its sequence is being designed, and only a deployment claims that the APIs using it are
     * protected. It is also the only rule of this class a deployment runs; the rest was checked when
     * the draft was written.
     */
    public void validateDeployable(Guardian guardian) {
        var installed = policyPluginProvider.findAllCapabilities();
        Predicate<String> isGuardrail = policy ->
            installed.get(policy) instanceof PolicyCapabilities capabilities && capabilities.guardrail();
        if (guardian.hasEnabledGuardrail(isGuardrail)) {
            return;
        }
        throw new GuardianWithoutEnabledGuardrailException(
            guardian.getId(),
            guardian
                .enabledPolicies()
                .filter(step -> !isGuardrail.test(step.getPolicy()))
                .toList()
        );
    }

    /**
     * Refuses a Guardian the gateway could not run with the policies installed now, and rewrites the
     * configuration of each enabled step as its policy accepts it.
     *
     * <p>For the write paths — creation, update, rollback — where what is stored has to be what was
     * validated.
     */
    public void validateAgainstInstalledPolicies(Guardian guardian) {
        refuse(validate(guardian, Correction.STORE));
    }

    /**
     * Refuses a Guardian the gateway could not run, leaving the definition exactly as it is stored.
     *
     * <p>For the paths that publish or restore a stored definition — a deployment, a rollback. They may
     * not rewrite what they read: a deployment that sanitised the draft would publish a revision the
     * stored draft no longer matches, and the Guardian would turn {@code PENDING} the moment it was
     * deployed. A configuration its policy only accepts corrected is therefore refused here rather than
     * ignored, since publishing it would publish one the policy was never validated with; editing and
     * saving the Guardian stores what the policy accepts, and that one deploys.
     */
    public void validateStoredAgainstInstalledPolicies(Guardian guardian) {
        refuse(validate(guardian, Correction.REFUSE));
    }

    /**
     * Validates without touching the Guardian it is given, the {@link Validator} contract being no
     * licence to rewrite a caller's object: a configuration a policy would correct is reported as a
     * violation here. The write paths call {@link #validateAgainstInstalledPolicies(Guardian)}, which
     * stores the corrected configuration instead.
     */
    @Override
    public Result<Input> validateAndSanitize(Input input) {
        return Result.ofBoth(input, validate(input.guardian(), Correction.REFUSE));
    }

    private static void refuse(List<Error> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationDomainException(errors.stream().map(Error::getMessage).collect(Collectors.joining(" ")));
        }
    }

    /**
     * @param correction what to do with a step whose configuration its policy only accepts corrected
     */
    private List<Error> validate(Guardian guardian, Correction correction) {
        var apiTypes = guardian.getApiTypes();
        if (apiTypes == null || apiTypes.isEmpty()) {
            return List.of(Error.severe("At least one API type is required."));
        }
        if (guardian.getPhase() == null) {
            return List.of(Error.severe("A Guardian phase is required."));
        }
        var unsupported = apiTypes
            .stream()
            .filter(apiType -> apiType != SUPPORTED_API_TYPE)
            .map(ApiType::getLabel)
            .sorted()
            .toList();
        if (!unsupported.isEmpty()) {
            return List.of(
                Error.severe("Only %s guardians are supported, not %s.", SUPPORTED_API_TYPE.getLabel(), String.join(", ", unsupported))
            );
        }
        return validateSteps(guardian.getPolicies(), apiTypes, guardian.getPhase(), correction);
    }

    private List<Error> validateSteps(List<Step> steps, Set<ApiType> apiTypes, Phase phase, Correction correction) {
        if (steps != null && steps.stream().anyMatch(Objects::isNull)) {
            return List.of(Error.severe("Guardian policies must not contain null steps."));
        }
        if (steps == null || steps.stream().noneMatch(Step::isEnabled)) {
            return List.of();
        }
        var policies = policyPluginProvider.findAllCapabilities();
        var errors = new ArrayList<Error>();
        for (var step : steps) {
            if (!step.isEnabled()) {
                continue;
            }
            var policy = policies.get(step.getPolicy());
            if (policy == null) {
                errors.add(Error.severe("Step [%s] references policy [%s], which is not installed.", step.getName(), step.getPolicy()));
                continue;
            }
            if (!policy.runsInGuardian(phase)) {
                errors.add(notRunningInGuardian(step, policy, phase));
            }
            if (policy.guardrail()) {
                var protocolError = policy.supports(apiTypes)
                    ? scopeProtocolError(step, apiTypes)
                    : Optional.of(incompatibleProtocol(step, apiTypes));
                if (protocolError.isPresent()) {
                    // the schema of a guardrail usually constrains its scope too: judging it would report
                    // this cause a second time, or a configuration moot on a Guardian it cannot run in
                    errors.add(protocolError.get());
                    continue;
                }
            }
            validateConfigurationSchema(step, errors, correction);
        }
        return errors;
    }

    /**
     * Judges the configuration of a step against the schema its policy declares, through the service the
     * API flows validate their own steps with, and names the step and the schema error as they do.
     *
     * <p>The gateway builds the policy from the stored configuration when it starts the Guardian: one
     * the policy cannot be created from leaves a revision the API answers {@code DEPLOYED} and the
     * console shows "Deployed", that no gateway ever runs (AIAM-1750). What the policy accepts is not
     * always what was given to it, so the configuration that ends up stored must be the accepted one:
     * storing the submitted one instead would store a configuration the policy was never validated
     * with, which is the same bug by another route. {@link Correction} says which of the two things a
     * path does with that difference.
     *
     * <p>A step carrying no configuration is judged as one carrying an empty object — what the gateway
     * would build the policy from anyway. A schema requiring a property therefore refuses it, where
     * skipping the check would accept exactly the Guardian this is about, and a schema that completes
     * the empty object with defaults has its completed configuration stored like any other.
     */
    private void validateConfigurationSchema(Step step, List<Error> errors, Correction correction) {
        var judged = configurationAsObject(step);
        if (judged.isEmpty()) {
            // refused here rather than by the shared service, which would answer the parser message: it
            // quotes the configuration it could not read, and that may carry a credential
            errors.add(Error.severe("Step [%s] has an invalid configuration: it must be a JSON object.", step.getName()));
            return;
        }
        String accepted;
        try {
            accepted = policyValidation.validateAndSanitizeConfiguration(step.getPolicy(), configurationOrEmpty(step));
        } catch (InvalidDataException e) {
            // the message carries the schema paths and keywords the validator reported. A configuration
            // value only appears in it when the schema itself names it — an enum, a pattern — with one
            // exception: a `discriminator` mismatch quotes the discriminating value of the instance.
            errors.add(
                Error.severe(
                    "Step [%s] has an invalid configuration: %s",
                    step.getName(),
                    asSentence(Objects.requireNonNullElse(e.getMessage(), "it does not match the schema of the policy"))
                )
            );
            return;
        } catch (PluginNotFoundException e) {
            // the registry read above is a snapshot, and this service resolves the plugin again: a policy
            // uninstalled in between is a refused step, not the 404 this exception would answer
            errors.add(Error.severe("Step [%s] has an invalid configuration: the policy is not installed.", step.getName()));
            return;
        }
        if (isSameConfiguration(accepted, judged.get())) {
            return;
        }
        switch (correction) {
            case STORE -> step.setConfiguration(accepted);
            case REFUSE -> errors.add(
                Error.severe(
                    "Step [%s] has a configuration its policy only accepts corrected: edit and save the Guardian to store the configuration the policy accepts.",
                    step.getName()
                )
            );
        }
    }

    /** The refusals are joined into one message, so each has to read as a sentence of its own. */
    private static String asSentence(String message) {
        return message.endsWith(".") ? message : message + ".";
    }

    /**
     * Compared as JSON rather than as text: the validator rebuilds the document it returns, so the same
     * configuration may come back with its properties in another order or spaced differently, and a
     * deployment refusing that difference would refuse a draft nothing is wrong with.
     */
    private boolean isSameConfiguration(String accepted, JsonNode judged) {
        return asJsonObject(accepted).filter(judged::equals).isPresent();
    }

    private static boolean carriesAConfiguration(Step step) {
        return step.getConfiguration() != null && !step.getConfiguration().isBlank();
    }

    private static String configurationOrEmpty(Step step) {
        return carriesAConfiguration(step) ? step.getConfiguration() : EMPTY_CONFIGURATION;
    }

    /** The configuration of the step as a JSON object, or nothing when it is not one. */
    private Optional<JsonNode> configurationAsObject(Step step) {
        return asJsonObject(configurationOrEmpty(step));
    }

    private Optional<JsonNode> asJsonObject(String configuration) {
        try {
            JsonNode node = jsonDeserializer.deserialize(configuration, JsonNode.class);
            return node != null && node.isObject() ? Optional.of(node) : Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /**
     * Refuses a guardrail whose {@code scope.protocol} designates another API type than the Guardian's.
     *
     * <p>A configuration that is no JSON object is left alone here: the schema check refuses it on its
     * own, naming one cause rather than two, and there is nothing to read in it anyway.
     */
    private Optional<Error> scopeProtocolError(Step step, Set<ApiType> apiTypes) {
        var configuration = configurationAsObject(step);
        if (configuration.isEmpty()) {
            return Optional.empty();
        }
        var scope = configuration.get().get("scope");
        if (scope == null || scope.isNull()) {
            return Optional.empty();
        }
        if (!scope.isObject()) {
            return Optional.of(Error.severe("Guardrail step [%s] has an invalid scope configuration.", step.getName()));
        }
        var protocol = scope.get("protocol");
        if (protocol == null) {
            return Optional.empty();
        }
        ApiType scopeApiType = protocol.isTextual() ? scopeApiType(protocol.textValue()) : null;
        if (scopeApiType == null) {
            return Optional.of(Error.severe("Guardrail step [%s] has an invalid scope protocol.", step.getName()));
        }
        if (apiTypes.stream().anyMatch(apiType -> apiType != scopeApiType)) {
            return Optional.of(
                Error.severe(
                    "Guardrail step [%s] declares scope protocol [%s], incompatible with Guardian API types [%s].",
                    step.getName(),
                    protocol.textValue(),
                    labels(apiTypes)
                )
            );
        }
        return Optional.empty();
    }

    private static Error notRunningInGuardian(Step step, PolicyCapabilities policy, Phase phase) {
        var supported = policy.guardianPhases().stream().sorted().toList();
        if (supported.isEmpty()) {
            return Error.severe(
                "Step [%s] using policy [%s] cannot run in a Guardian: the installed plugin does not support Guardians. Remove the step or install a version that supports them.",
                step.getName(),
                step.getPolicy()
            );
        }
        return Error.severe(
            "Step [%s] using policy [%s] cannot run in a Guardian of phase [%s]: it only supports Guardians of phase %s.",
            step.getName(),
            step.getPolicy(),
            phase,
            supported
        );
    }

    /** The API type a guardrail {@code scope.protocol} designates, or {@code null} for any other value: see {@link PolicyFlowType}. */
    private static ApiType scopeApiType(String protocol) {
        return PolicyFlowType.fromScopeProtocol(protocol).flatMap(PolicyFlowType::guardrailApiType).orElse(null);
    }

    private static Error incompatibleProtocol(Step step, Set<ApiType> apiTypes) {
        return Error.severe(
            "Guardrail step [%s] using policy [%s] does not support Guardian API types [%s].",
            step.getName(),
            step.getPolicy(),
            labels(apiTypes)
        );
    }

    private static String labels(Set<ApiType> apiTypes) {
        return apiTypes.stream().map(ApiType::getLabel).sorted().collect(Collectors.joining(", "));
    }
}
