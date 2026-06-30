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
package io.gravitee.definition.model.v4.agent.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * One comparison {@code <variable> <op> <value>}, typed by {@code valueType}. Operators:
 * {@code >= > <= < eq neq contains startsWith endsWith matches isTrue isFalse isPresent isAbsent}.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Clause {

    /** Scope key it reads. */
    private String variable;

    private String op;

    /** Literal operand (number/string/boolean); omitted for unary ops. */
    private Object value;

    /** {@code number} | {@code string} | {@code boolean}. */
    private String valueType;
}
