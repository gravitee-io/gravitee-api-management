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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.ResourceReference;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailWithoutEnabledGuardrailPolicyException;
import io.gravitee.gamma.rest.core.guardrail.model.CreateGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.UpdateGuardrailCommand;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Validates Guardrail structure, supported API types and phase, the policies its steps reference and the
 * resources it holds.
 *
 * <p><strong>Structure</strong> — the required fields, the field sizes and the shape of the id —
 * is declared as bean validation constraints on {@link CreateGuardrail} and
 * {@link UpdateGuardrailCommand}, and run here <em>explicitly</em>. Spring method validation is not
 * wired in this product, so a {@code @Valid} on a use-case parameter would be inert; running the
 * validator by hand is what makes the constraints hold for every caller of the use case rather than
 * only for one arriving through JAX-RS. {@code RemoteApiDefinitionParser} does the same in
 * management v2, for the same reason. The {@link ConstraintViolationException} it throws already has
 * a mapper registered by {@code GammaModuleApplication}, so it answers 400.
 *
 * <p><strong>The product rule</strong> — the gateway only runs a Guardrail on an {@code llm-proxy} in
 * 4.14, so accepting any other API type would store a Guardrail nothing can execute. A Guardrail also
 * needs a phase.
 *
 * <p><strong>The steps</strong> — every enabled step, guardrail policy or not, must reference an installed
 * policy whose plugin declares the Guardrail phase in its {@code guardrail} flow, and carry a
 * configuration that policy's schema accepts. A guardrail policy must also support the Guardrail API types, in
 * its plugin metadata and in any explicit scope protocol: the {@code guardrail} flow says where it
 * runs, its API type flows only which API types it applies to. A property its schema marks as a resource
 * picker ({@code gioConfig.uiType: resource-type}) must name an enabled resource of the Guardrail, of a type
 * the picker accepts.
 *
 * <p><strong>The resources</strong> — judged by {@link ValidateGuardrailResourcesDomainService}: every
 * enabled resource must be of a type the node installed, carry a configuration that plugin's schema
 * accepts, and share its name with no other enabled one.
 *
 * <p>These rules apply to creation and to update, a {@code PUT} replacing the whole definition, and
 * again to the stored draft when it is deployed: a draft may predate a rule, or have been written
 * against plugins that have changed since, and a deployment turns it into a revision the gateways
 * must be able to start. One rule runs there only, {@link #validateDeployable(Guardrail)} — the
 * sequence must hold an enabled guardrail policy — so a draft may be saved empty and still be refused at
 * deployment. What separates the two passes is the configuration a plugin accepts only corrected: a
 * write path stores it, a deployment or a rollback refuses the step or the resource carrying it.
 *
 * <p><strong>The name is free</strong> — two Guardrails of one environment may not share a name. This
 * is the check users meet, and the only one that reads the store; the unique index behind it is a
 * backstop for a race, not the enforcement, and it cannot be relied on for the case-insensitive rule
 * nor on a store that predates it.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateGuardrailDomainService implements Validator<ValidateGuardrailDomainService.Input> {

    private static final jakarta.validation.Validator BEAN_VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ApiType SUPPORTED_API_TYPE = ApiType.LLM_PROXY;

    private final GuardrailRepository guardrailRepository;
    private final PolicyPluginProvider policyPluginProvider;
    private final PolicyValidationDomainService policyValidation;
    private final ValidateGuardrailResourcesDomainService resources;
    private final JsonDeserializer jsonDeserializer;

    public record Input(Guardrail guardrail) implements Validator.Input {}

    /** Refuses a creation payload whose shape the store would not accept. */
    public void validateStructure(CreateGuardrail command) {
        refuseViolations(command);
    }

    /** Refuses an update payload whose shape the store would not accept. */
    public void validateStructure(UpdateGuardrailCommand command) {
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
    public void validateDefinition(io.gravitee.definition.model.v4.guardrail.Guardrail definition) {
        refuseViolations(definition);
        refuseAnOverlongName(definition);
    }

    /**
     * The length the guardrails table enforces, checked on the assembled definition because both writes
     * pass through it and the creation payload's own {@code @Size} only covers one of them.
     *
     * <p>Here rather than as an annotation on the definition model: that model is shared with the
     * gateway. Raising the limit belongs with the column.
     */
    private static void refuseAnOverlongName(io.gravitee.definition.model.v4.guardrail.Guardrail definition) {
        var name = definition.getName();
        if (name != null && name.length() > CreateGuardrail.NAME_MAX_LENGTH) {
            throw new ValidationDomainException(CreateGuardrail.NAME_TOO_LONG);
        }
    }

    /**
     * Refuses a name another Guardrail of the environment already carries, ignoring case.
     *
     * @param guardrailIdToExclude the Guardrail being written, so renaming one to the name it already
     *     has is not read as a clash with itself. On a creation it is the id about to be used, which
     *     no stored Guardrail carries — a caller-supplied id that is taken is refused on its own, by
     *     the store.
     */
    public void validateNameIsAvailable(String environmentId, String name, String guardrailIdToExclude) {
        // every match, then exclude this one: with a single row, "the only match is me" and "one of the
        // matches is me" are indistinguishable, and the answer would depend on which row came back
        boolean takenByAnother = guardrailRepository
            .findByEnvironmentIdAndName(environmentId, name)
            .stream()
            .anyMatch(existing -> !existing.getId().equals(guardrailIdToExclude));
        if (takenByAnother) {
            throw new GuardrailNameAlreadyExistsException(name);
        }
    }

    private static <C> void refuseViolations(C command) {
        var violations = BEAN_VALIDATOR.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    /**
     * Refuses a Guardrail that would enforce nothing: no enabled step of its sequence runs a guardrail policy
     * the platform has installed.
     *
     * <p>Run when a Guardrail is deployed, not when it is saved — a draft is expected to be empty while
     * its sequence is being designed, and only a deployment claims that the APIs using it are
     * protected. A deployment runs it beside {@link #validateStoredAgainstInstalledPlugins(Guardrail)}.
     */
    public void validateDeployable(Guardrail guardrail) {
        var installed = policyPluginProvider.findAllCapabilities();
        Predicate<String> isGuardrailPolicy = policy ->
            installed.get(policy) instanceof PolicyCapabilities capabilities && capabilities.guardrailPolicy();
        if (guardrail.hasEnabledGuardrailPolicy(isGuardrailPolicy)) {
            return;
        }
        throw new GuardrailWithoutEnabledGuardrailPolicyException(
            guardrail.getId(),
            guardrail
                .enabledPolicies()
                .filter(step -> !isGuardrailPolicy.test(step.getPolicy()))
                .toList()
        );
    }

    /**
     * Refuses a Guardrail the gateway could not run with the plugins installed now, and rewrites the
     * configuration of each enabled step and resource as its plugin accepts it.
     *
     * <p>For the write paths — creation, update — where what is stored has to be what was validated.
     */
    public void validateAgainstInstalledPlugins(Guardrail guardrail) {
        refuse(validate(guardrail, Correction.STORE));
    }

    /**
     * Refuses a Guardrail the gateway could not run, leaving the definition exactly as it is stored.
     *
     * <p>For the paths that publish or restore a stored definition — a deployment, a rollback. They may
     * not rewrite what they read: a deployment that sanitized the draft would publish a revision the
     * stored draft no longer matches, and the Guardrail would turn {@code PENDING} the moment it was
     * deployed. A configuration its plugin only accepts corrected is therefore refused here rather than
     * ignored, since publishing it would publish one the plugin was never validated with; editing and
     * saving the Guardrail stores what the plugin accepts, and that one deploys.
     */
    public void validateStoredAgainstInstalledPlugins(Guardrail guardrail) {
        refuse(validate(guardrail, Correction.REFUSE));
    }

    /**
     * Validates without touching the Guardrail it is given, the {@link Validator} contract being no
     * license to rewrite a caller's object: a configuration a policy or a resource plugin would correct
     * is reported as a violation here. The write paths call {@link #validateAgainstInstalledPlugins(Guardrail)}, which
     * stores the corrected configuration instead.
     */
    @Override
    public Result<Input> validateAndSanitize(Input input) {
        return Result.ofBoth(input, validate(input.guardrail(), Correction.REFUSE));
    }

    private static void refuse(List<Error> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationDomainException(errors.stream().map(Error::getMessage).collect(Collectors.joining(" ")));
        }
    }

    /**
     * @param correction what to do with a step or a resource whose configuration its plugin only accepts corrected
     */
    private List<Error> validate(Guardrail guardrail, Correction correction) {
        var apiTypes = guardrail.getApiTypes();
        if (apiTypes == null || apiTypes.isEmpty()) {
            return List.of(Error.severe("At least one API type is required."));
        }
        if (guardrail.getPhase() == null) {
            return List.of(Error.severe("A Guardrail phase is required."));
        }
        var unsupported = apiTypes
            .stream()
            .filter(apiType -> apiType != SUPPORTED_API_TYPE)
            .map(ApiType::getLabel)
            .sorted()
            .toList();
        if (!unsupported.isEmpty()) {
            return List.of(
                Error.severe("Only %s guardrails are supported, not %s.", SUPPORTED_API_TYPE.getLabel(), String.join(", ", unsupported))
            );
        }
        var errors = new ArrayList<>(validateSteps(guardrail, apiTypes, correction));
        errors.addAll(resources.validate(guardrail.getResources(), guardrail.getEnvironmentId(), correction));
        return errors;
    }

    private List<Error> validateSteps(Guardrail guardrail, Set<ApiType> apiTypes, Correction correction) {
        var steps = guardrail.getPolicies();
        var phase = guardrail.getPhase();
        if (steps != null && steps.stream().anyMatch(Objects::isNull)) {
            return List.of(Error.severe("Guardrail policies must not contain null steps."));
        }
        if (steps == null || steps.stream().noneMatch(Step::isEnabled)) {
            return List.of();
        }
        var policies = policyPluginProvider.findAllCapabilities();
        var references = new HashMap<String, List<ResourceReference>>();
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
            if (!policy.runsInGuardrail(phase)) {
                errors.add(notRunningInGuardrail(step, policy, phase));
            }
            if (policy.guardrailPolicy()) {
                var protocolError = policy.supports(apiTypes)
                    ? scopeProtocolError(step, apiTypes)
                    : Optional.of(incompatibleProtocol(step, apiTypes));
                if (protocolError.isPresent()) {
                    // the schema of a guardrail policy usually constrains its scope too: judging it would report
                    // this cause a second time, or a configuration moot on a Guardrail it cannot run in
                    errors.add(protocolError.get());
                    continue;
                }
            }
            validateConfigurationSchema(step, errors, correction).ifPresent(configuration ->
                validateResourceReferences(
                    step,
                    configuration,
                    references.computeIfAbsent(step.getPolicy(), policyPluginProvider::findResourceReferences),
                    guardrail,
                    errors
                )
            );
        }
        return errors;
    }

    /**
     * Refuses a step whose configuration names a resource the gateway would not hand it: one the Guardrail
     * does not hold, holds disabled, or holds of a type the policy does not accept in that property.
     *
     * <p>Which property names a resource is read from the policy schema, the one the console builds its
     * picker from ({@code gioConfig.uiType: resource-type}), so the validation accepts what the picker
     * offers. A property left empty is the schema's business, judged just before. Only a property holding a
     * single name is read: an array of names is not checked.
     */
    private static void validateResourceReferences(
        Step step,
        JsonNode configuration,
        List<ResourceReference> references,
        Guardrail guardrail,
        List<Error> errors
    ) {
        for (var reference : references) {
            var named = configuration.at(reference.property());
            if (!named.isTextual() || named.textValue().isBlank()) {
                continue;
            }
            resourceReferenceError(step, reference, named.textValue(), guardrail).ifPresent(errors::add);
        }
    }

    private static Optional<Error> resourceReferenceError(Step step, ResourceReference reference, String name, Guardrail guardrail) {
        var enabled = guardrail.enabledResourceNamed(name);
        if (enabled.isEmpty()) {
            return Optional.of(
                guardrail.holdsResourceNamed(name)
                    ? Error.severe("Step [%s] references resource [%s], which is disabled.", step.getName(), name)
                    : Error.severe("Step [%s] references resource [%s], which the Guardrail does not hold.", step.getName(), name)
            );
        }
        var type = enabled.get().getType();
        // a resource without a type is refused as such by the resource validation
        if (type == null || reference.accepts(type)) {
            return Optional.empty();
        }
        return Optional.of(
            Error.severe(
                "Step [%s] references a resource of the wrong type: [%s] is of type [%s], where the policy accepts [%s].",
                step.getName(),
                name,
                type,
                String.join(", ", reference.acceptedTypes())
            )
        );
    }

    /**
     * Judges the configuration of a step against the schema its policy declares, through the service the
     * API flows validate their own steps with, and names the step and the schema error as they do.
     *
     * <p>The gateway builds the policy from the stored configuration when it starts the Guardrail: one
     * the policy cannot be created from leaves a revision the API answers {@code DEPLOYED} and the
     * console shows "Deployed", that no gateway ever runs (AIAM-1750). What the policy accepts is not
     * always what was given to it, so the configuration that ends up stored must be the accepted one:
     * storing the submitted one instead would store a configuration the policy was never validated
     * with, which is the same bug by another route. {@link Correction} says which of the two things a
     * path does with that difference.
     *
     * <p>A step carrying no configuration is judged as one carrying an empty object — what the gateway
     * would build the policy from anyway. A schema requiring a property therefore refuses it, where
     * skipping the check would accept exactly the Guardrail this is about, and a schema that completes
     * the empty object with defaults has its completed configuration stored like any other.
     *
     * @return the configuration the step now carries, when the policy accepts it as such; nothing when the step
     *     is refused
     */
    private Optional<JsonNode> validateConfigurationSchema(Step step, List<Error> errors, Correction correction) {
        var judged = configurationAsObject(step);
        if (judged.isEmpty()) {
            // refused here rather than by the shared service, which would answer the parser message: it
            // quotes the configuration it could not read, and that may carry a credential
            errors.add(Error.severe("Step [%s] has an invalid configuration: it must be a JSON object.", step.getName()));
            return Optional.empty();
        }
        String accepted;
        try {
            accepted = policyValidation.validateAndSanitizeConfiguration(
                step.getPolicy(),
                ConfigurationDocuments.orEmpty(step.getConfiguration())
            );
        } catch (InvalidDataException e) {
            // the message carries the schema paths and keywords the validator reported. A configuration
            // value only appears in it when the schema itself names it — an enum, a pattern — with one
            // exception: a `discriminator` mismatch quotes the discriminating value of the instance.
            errors.add(
                Error.severe(
                    "Step [%s] has an invalid configuration: %s",
                    step.getName(),
                    ConfigurationDocuments.asSentence(
                        Objects.requireNonNullElse(e.getMessage(), "it does not match the schema of the policy")
                    )
                )
            );
            return Optional.empty();
        } catch (PluginNotFoundException e) {
            // the registry read above is a snapshot, and this service resolves the plugin again: a policy
            // uninstalled in between is a refused step, not the 404 this exception would answer
            errors.add(Error.severe("Step [%s] has an invalid configuration: the policy is not installed.", step.getName()));
            return Optional.empty();
        }
        if (ConfigurationDocuments.isSame(jsonDeserializer, accepted, judged.get())) {
            return judged;
        }
        return switch (correction) {
            case STORE -> {
                step.setConfiguration(accepted);
                yield ConfigurationDocuments.asObject(jsonDeserializer, accepted);
            }
            case REFUSE -> {
                errors.add(
                    Error.severe(
                        "Step [%s] has a configuration its policy only accepts corrected: edit and save the Guardrail to store the configuration the policy accepts.",
                        step.getName()
                    )
                );
                yield Optional.empty();
            }
        };
    }

    /** The configuration of the step as a JSON object, or nothing when it is not one. */
    private Optional<JsonNode> configurationAsObject(Step step) {
        return ConfigurationDocuments.asObject(jsonDeserializer, ConfigurationDocuments.orEmpty(step.getConfiguration()));
    }

    /**
     * Refuses a guardrail policy whose {@code scope.protocol} designates another API type than the Guardrail's.
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
            return Optional.of(Error.severe("Guardrail policy step [%s] has an invalid scope configuration.", step.getName()));
        }
        var protocol = scope.get("protocol");
        if (protocol == null) {
            return Optional.empty();
        }
        ApiType scopeApiType = protocol.isTextual() ? scopeApiType(protocol.textValue()) : null;
        if (scopeApiType == null) {
            return Optional.of(Error.severe("Guardrail policy step [%s] has an invalid scope protocol.", step.getName()));
        }
        if (apiTypes.stream().anyMatch(apiType -> apiType != scopeApiType)) {
            return Optional.of(
                Error.severe(
                    "Guardrail policy step [%s] declares scope protocol [%s], incompatible with Guardrail API types [%s].",
                    step.getName(),
                    protocol.textValue(),
                    labels(apiTypes)
                )
            );
        }
        return Optional.empty();
    }

    private static Error notRunningInGuardrail(Step step, PolicyCapabilities policy, Phase phase) {
        var supported = policy.guardrailPhases().stream().sorted().toList();
        if (supported.isEmpty()) {
            return Error.severe(
                "Step [%s] using policy [%s] cannot run in a Guardrail: the installed plugin does not support Guardrails. Remove the step or install a version that supports them.",
                step.getName(),
                step.getPolicy()
            );
        }
        return Error.severe(
            "Step [%s] using policy [%s] cannot run in a Guardrail of phase [%s]: it only supports Guardrails of phase %s.",
            step.getName(),
            step.getPolicy(),
            phase,
            supported
        );
    }

    /** The API type a guardrail policy {@code scope.protocol} designates, or {@code null} for any other value: see {@link PolicyFlowType}. */
    private static ApiType scopeApiType(String protocol) {
        return PolicyFlowType.fromScopeProtocol(protocol).flatMap(PolicyFlowType::guardrailPolicyApiType).orElse(null);
    }

    private static Error incompatibleProtocol(Step step, Set<ApiType> apiTypes) {
        return Error.severe(
            "Guardrail policy step [%s] using policy [%s] does not support Guardrail API types [%s].",
            step.getName(),
            step.getPolicy(),
            labels(apiTypes)
        );
    }

    private static String labels(Set<ApiType> apiTypes) {
        return apiTypes.stream().map(ApiType::getLabel).sorted().collect(Collectors.joining(", "));
    }
}
