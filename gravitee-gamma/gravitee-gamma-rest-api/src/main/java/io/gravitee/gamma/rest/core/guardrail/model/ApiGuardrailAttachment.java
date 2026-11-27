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

/**
 * A Guardrail an API runs, and the calls it runs it on.
 *
 * @param condition the expression-language condition the gateway evaluates before the Guardrail's step, running the
 *     Guardrail only on the calls it holds for; {@code null} runs it on every call. A blank condition is read as
 *     {@code null}: the gateway would read it as text, fail to evaluate it and skip the Guardrail on every call.
 * @author GraviteeSource Team
 */
public record ApiGuardrailAttachment(String guardrailId, String condition) {
    public ApiGuardrailAttachment {
        condition = condition == null || condition.isBlank() ? null : condition;
    }
}
