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
package io.gravitee.rest.api.model.configuration.dictionary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encryption options for a single dictionary property, carried beside the property map and keyed by
 * the same key.
 *
 * <p>Both flags are nullable on purpose: {@code null} means the caller said nothing, so the
 * property keeps the classification it already has. That is what lets a client save a dictionary it
 * only partly edited without restating the classification of every untouched key.
 *
 * @author GraviteeSource Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryPropertyOptions {

    /**
     * The value is the stored ciphertext. Supplied on a write, it declares an already-encrypted
     * value that must be stored verbatim.
     */
    private Boolean encrypted;

    /** The submitted plaintext value has to be encrypted on save. */
    private Boolean encryptable;
}
