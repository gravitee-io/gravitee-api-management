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
package io.gravitee.definition.model.v4.ssl.pem;

import io.gravitee.definition.model.v4.ssl.KeyStore;
import io.gravitee.definition.model.v4.ssl.KeyStoreType;
import java.io.Serial;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@Builder
public class PEMKeyStore extends KeyStore {

    @Serial
    private static final long serialVersionUID = 1051430527272519608L;

    private String keyPath;
    private String keyContent;
    private String certPath;
    private String certContent;

    public PEMKeyStore() {
        super(KeyStoreType.PEM);
    }

    public PEMKeyStore(String keyPath, String keyContent, String certPath, String certContent) {
        super(KeyStoreType.PEM);
        this.keyPath = keyPath;
        this.keyContent = keyContent;
        this.certPath = certPath;
        this.certContent = certContent;
    }
}
