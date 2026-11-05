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
package io.gravitee.gamma.rest.core.guardian.model;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What creating a Guardian takes: every attribute of the definition, flattened, plus the description
 * the control plane keeps beside it.
 *
 * <p>The constraints live here rather than on the request DTO so they hold for <em>any</em> caller of
 * {@code CreateGuardianUseCase}, not only one arriving through JAX-RS. Spring method validation is
 * not wired in this product, so nothing enforces them by magic:
 * {@code ValidateGuardianDomainService} runs them explicitly, the way
 * {@code RemoteApiDefinitionParser} does in management v2 and for the same reason.
 *
 * <p>{@code policies} and {@code resources} cascade, which is safe — {@code Step} and
 * {@code Resource} validate cleanly, as {@code GuardianDefinitionValidationTest} shows.
 *
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CreateGuardian {

    /**
     * The sizes the guardians table enforces, from
     * {@code liquibase/changelogs/v4_14_0/07_create_guardians_table.yml}. Without the check the two
     * backends disagree: Mongo accepts any length while JDBC raises a constraint violation, which the
     * infra layer can only report as a technical failure — never as "this value is too long".
     */
    public static final int NAME_MAX_LENGTH = 512;

    public static final int DESCRIPTION_MAX_LENGTH = 1024;

    /**
     * A UUID and nothing else, when the caller supplies one: the id reaches a URL — the
     * {@code Location} of the creation, then every later path — and a 64-character primary key, and
     * the server generates UUIDs itself.
     */
    @Pattern(regexp = UUID_PATTERN, message = "must be a UUID")
    private String id;

    @NotBlank
    @Size(max = NAME_MAX_LENGTH)
    private String name;

    @Size(max = DESCRIPTION_MAX_LENGTH)
    private String description;

    @NotNull
    private Phase phase;

    @NotEmpty
    private Set<ApiType> apiTypes;

    @Valid
    private List<Step> policies;

    @Valid
    private List<Resource> resources;

    static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    /** The definition the gateway reads, without the fields the control plane keeps for itself. */
    public io.gravitee.definition.model.v4.guardian.Guardian toDefinition() {
        return io.gravitee.definition.model.v4.guardian.Guardian.builder()
            .id(id)
            .name(name)
            .phase(phase)
            .apiTypes(apiTypes)
            .policies(policies)
            .resources(resources)
            .build();
    }
}
