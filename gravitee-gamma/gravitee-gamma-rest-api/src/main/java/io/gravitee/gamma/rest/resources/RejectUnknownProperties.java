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
package io.gravitee.gamma.rest.resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a request type whose unknown JSON keys are refused with a 400 naming the key, although the Gamma
 * mapper ignores unknown keys elsewhere. Applies to the annotated type only: a nested type must carry it
 * too to be strict.
 *
 * <p>Jackson's {@code @JsonIgnoreProperties(ignoreUnknown = false)} looks equivalent but is not: it cannot
 * re-enable a check the mapper disables.
 *
 * @author GraviteeSource Team
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RejectUnknownProperties {}
