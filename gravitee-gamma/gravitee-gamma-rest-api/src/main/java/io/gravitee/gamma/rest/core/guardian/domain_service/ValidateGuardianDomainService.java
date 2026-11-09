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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * The checks a Guardian has to pass, kept apart because they answer different questions.
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
 * 4.14, so accepting any other API type would store a Guardian nothing can execute. It applies to
 * creation and to update alike, a {@code PUT} replacing the whole definition.
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

    private final GuardianRepository guardianRepository;

    private static final ApiType SUPPORTED_API_TYPE = ApiType.LLM_PROXY;

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

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var apiTypes = input.guardian().getApiTypes();
        if (apiTypes == null || apiTypes.isEmpty()) {
            return Result.ofBoth(input, List.of(Error.severe("At least one API type is required.")));
        }
        var unsupported = apiTypes
            .stream()
            .filter(apiType -> apiType != SUPPORTED_API_TYPE)
            .map(ApiType::getLabel)
            .sorted()
            .toList();
        if (!unsupported.isEmpty()) {
            return Result.ofBoth(
                input,
                List.of(
                    Error.severe("Only %s guardians are supported, not %s.", SUPPORTED_API_TYPE.getLabel(), String.join(", ", unsupported))
                )
            );
        }
        return Result.ofValue(input);
    }
}
