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
package io.gravitee.apim.common.secret;

/**
 * Wraps and unwraps the data keys of the organizations. A KMS provider can implement it without ever exposing the key
 * encryption key, and a provider that holds a previous key encryption key as well can unwrap the data keys stored under it.
 */
public interface KeyEncryptionKeyProvider {
    /**
     * A stable identifier of the key encryption key that wraps new data keys. It holds no key material and is stored
     * beside each wrapped data key.
     */
    String id();

    /**
     * Whether this provider holds the key encryption key of that id, and so can unwrap the data keys stored under it.
     */
    boolean canUnwrap(String kekId);

    byte[] wrap(String organizationId, byte[] dataKey);

    /**
     * @param kekId the id stored beside the wrapped data key
     * @throws KeyEncryptionKeyMismatchException when this provider does not hold the key encryption key of that id
     */
    byte[] unwrap(String organizationId, String kekId, byte[] wrappedDataKey);
}
