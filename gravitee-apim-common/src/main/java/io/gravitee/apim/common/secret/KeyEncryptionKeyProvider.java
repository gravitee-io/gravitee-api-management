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
 * encryption key.
 */
public interface KeyEncryptionKeyProvider {
    /**
     * A stable identifier of the key encryption key that holds no key material, stored beside each wrapped data key so a
     * wrong key or a rotation can be detected.
     */
    String id();

    byte[] wrap(String organizationId, byte[] dataKey);

    byte[] unwrap(String organizationId, byte[] wrappedDataKey);
}
