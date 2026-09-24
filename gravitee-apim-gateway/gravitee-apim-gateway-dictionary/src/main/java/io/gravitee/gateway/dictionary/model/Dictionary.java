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
package io.gravitee.gateway.dictionary.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import java.util.Date;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Dictionary {

    @ToString.Include
    @EqualsAndHashCode.Include
    private String id;

    @ToString.Include
    private String key;

    @ToString.Include
    @EqualsAndHashCode.Include
    private String environmentId;

    @ToString.Include
    private String name;

    private Date deployedAt;

    /**
     * A dictionary saved before property values were validated can carry a null-valued entry. This
     * payload is history — it cannot be rejected, only read — so the null entry is dropped and the
     * rest of the dictionary still reaches the gateway.
     */
    @JsonSetter(contentNulls = Nulls.SKIP)
    private Map<String, DictionaryProperty> properties;
}
