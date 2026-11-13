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

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What a guardrail does to a call it has evaluated, as the guardrail policies spell it in their configuration.
 *
 * <p>These constants name the actions a guardrail policy accepts, and they have to keep naming all of them: an action
 * this enum does not hold is reported as no effect at all, so a guardrail that alters a call would read as harmless.
 * The set mirrors the {@code Action} of the guardrail policies, and the order of the transformations mirrors the order
 * the gateway resolves a run's verdict in — most destructive first.
 *
 * <p>The order of the constants is the order the effects are reported in: what stops a call, then what alters it from
 * the most to the least destructive, then what lets it through. A screen therefore reads them strongest first without
 * sorting them again — this is the one place that decides.
 *
 * @author GraviteeSource Team
 */
public enum GuardrailEffect {
    /** The call fails. */
    BLOCK,
    /** The call goes through with the matched content replaced by the configured replacement. */
    REWRITE,
    /** The call goes through with the matched content removed. */
    REDACT,
    /** The call goes through with the matched content hidden. */
    MASK,
    /** The call goes through untouched. */
    CONTINUE;

    private static final Map<String, GuardrailEffect> BY_NAME = Stream.of(values()).collect(
        Collectors.toUnmodifiableMap(effect -> effect.name().toLowerCase(Locale.ROOT), Function.identity())
    );

    /** The effect a guardrail configuration names, empty when it names one this enum does not hold. */
    public static Optional<GuardrailEffect> byName(String action) {
        return action == null ? Optional.empty() : Optional.ofNullable(BY_NAME.get(action.toLowerCase(Locale.ROOT)));
    }
}
