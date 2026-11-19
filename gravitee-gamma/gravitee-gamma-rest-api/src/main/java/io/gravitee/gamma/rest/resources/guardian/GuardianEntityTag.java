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
package io.gravitee.gamma.rest.resources.guardian;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.guardian.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.resources.guardian.dto.GuardianDto;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The one place the {@code ETag} of a Guardian is written and its {@code If-Match} read back.
 *
 * <p>The version of the draft travels as an {@code ETag} / {@code If-Match} pair, as it does for the
 * observability dashboards: every response carrying a single Guardian names its version, and an update
 * has to state the one it was built on. Writing and reading the validator side by side is what keeps the
 * value handed out on one response the value accepted on the next request.
 *
 * @author GraviteeSource Team
 */
public final class GuardianEntityTag {

    private static final int PRECONDITION_REQUIRED_428 = 428;
    private static final String WILDCARD_VALIDATOR = "*";

    private GuardianEntityTag() {}

    /** Attaches the Guardian as the response entity, plus its {@code ETag}. */
    public static Response.ResponseBuilder withETag(Response.ResponseBuilder builder, Guardian guardian) {
        return withETagOnly(builder.entity(GuardianDto.from(guardian)), guardian);
    }

    /**
     * Attaches the {@code ETag} alone, for a response carrying something other than the Guardian itself.
     *
     * <p>No version, no tag: a Guardian written by a node older than the counter has nothing to match on,
     * and the only way to save it is {@code If-Match: *}. That write gives it a version. The Guardians
     * stored before the counter existed are not in that case: the upgrade gave them version 1.
     */
    public static Response.ResponseBuilder withETagOnly(Response.ResponseBuilder builder, Guardian guardian) {
        return guardian.getVersion() == null ? builder : builder.tag(new EntityTag(String.valueOf(guardian.getVersion())));
    }

    /**
     * Turns {@code If-Match} into the precondition an update is subject to.
     *
     * <p>Every value of the header is read, since HTTP lets a client write a list in one field or repeat
     * the field. A weak validator ({@code W/"4"}) names the same version as a strong one. {@code *} is a
     * deliberate overwrite, which is why it is safe to offer: it cannot be reached by forgetting the
     * header, only by choosing it.
     *
     * @throws WebApplicationException 428 when the header is missing: an update that states no version is
     *     refused rather than read as an overwrite
     * @throws ValidationDomainException when a validator is not a version this API issued
     */
    public static DraftPrecondition requiredPrecondition(List<String> ifMatch) {
        List<String> validators = ifMatch == null
            ? List.of()
            : ifMatch
                .stream()
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(validator -> !validator.isEmpty())
                .toList();
        if (validators.isEmpty()) {
            throw new WebApplicationException(
                Response.status(PRECONDITION_REQUIRED_428)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(
                        new ErrorEntity(
                            "If-Match is required on a Guardian update: send back the ETag you received, or `*` to overwrite whatever version is current.",
                            PRECONDITION_REQUIRED_428
                        )
                    )
                    .build()
            );
        }
        if (validators.contains(WILDCARD_VALIDATOR)) {
            if (validators.size() > 1) {
                // guessing which was meant could silently overwrite
                throw new ValidationDomainException("If-Match must be either `*` or a list of ETags, not both.");
            }
            return DraftPrecondition.anyVersion();
        }
        return DraftPrecondition.oneOf(validators.stream().map(GuardianEntityTag::parseVersion).collect(Collectors.toSet()));
    }

    private static int parseVersion(String validator) {
        String value = validator.startsWith("W/") ? validator.substring(2) : validator;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ValidationDomainException("If-Match '%s' is not a Guardian ETag issued by this API.".formatted(validator));
        }
    }
}
