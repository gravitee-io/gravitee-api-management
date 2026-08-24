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
package io.gravitee.apim.core.log.model;

import java.util.Locale;
import java.util.Set;

/**
 * Whether a log projection may return the {@code security-token} of a request document.
 *
 * <p>The field means different things depending on the plan that authenticated the request, and for one of
 * them it <b>is</b> the secret:
 *
 * <ul>
 *   <li>{@code OAUTH2} / {@code JWT} — the application's client id, a public identifier
 *       ({@code CheckSubscriptionPolicy}, {@code JwtPlanBasedAuthenticationHandler}).</li>
 *   <li>{@code API_KEY} — the API key verbatim ({@code ApiKeyAuthenticationHandler}). Returning it hands a
 *       subscriber's live credential to anyone holding log-read permission.</li>
 * </ul>
 *
 * <p>This is an allow-list rather than an {@code API_KEY} denial on purpose: a plan type added later, or a
 * gateway that starts writing something else, is excluded until someone decides otherwise. Keying on the
 * credential type also keeps the rule where the danger is — an earlier guard keyed on "is this a native
 * connection document", which fails open, since a native Kafka API may carry an API-key plan and the
 * invariant that made it safe (the native reactor never writing the key) lives in another repository where
 * nothing here would catch its regression.
 *
 * @author GraviteeSource Team
 */
public final class SecurityTokenProjection {

    /** Credential types whose token is a public identifier rather than the credential itself. */
    private static final Set<String> PROJECTABLE_SECURITY_TYPES = Set.of("OAUTH2", "JWT");

    private SecurityTokenProjection() {}

    /**
     * {@code true} when the token recorded for this credential type is safe to return to a log reader.
     * An unknown, blank or {@code null} type is not projectable.
     */
    public static boolean isTokenProjectable(String securityType) {
        return securityType != null && PROJECTABLE_SECURITY_TYPES.contains(securityType.trim().toUpperCase(Locale.ROOT));
    }
}
