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
package io.gravitee.apim.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EncryptedValueMask {

    /**
     * Stands in for an encrypted value on a read, so the ciphertext never leaves through one. Resubmitting it
     * on a write means "keep the stored value". The Console renders the same string, see
     * {@code shared/utils/encrypted-value-mask.util.ts}.
     */
    public static final String ENCRYPTED_VALUE_MASK = "••••••••••••";
}
