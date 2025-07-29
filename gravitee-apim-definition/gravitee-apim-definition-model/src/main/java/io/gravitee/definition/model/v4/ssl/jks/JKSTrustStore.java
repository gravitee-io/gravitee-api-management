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
package io.gravitee.definition.model.v4.ssl.jks;

import io.gravitee.definition.model.v4.ssl.TrustStore;
import io.gravitee.definition.model.v4.ssl.TrustStoreType;
import io.gravitee.secrets.api.annotation.Secret;
import io.gravitee.secrets.api.el.FieldKind;
import java.io.Serial;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
public class JKSTrustStore extends TrustStore {

    @Serial
    private static final long serialVersionUID = -6603840868190194763L;

    @Secret
    private String path;

    @Secret
    private String content;

    @Secret(FieldKind.PASSWORD)
    private String password;

    @Secret
    private String alias;

    public JKSTrustStore() {
        super(TrustStoreType.JKS);
    }

    public JKSTrustStore(String path, String content, String password, String alias) {
        super(TrustStoreType.JKS);
        this.path = path;
        this.content = content;
        this.password = password;
        this.alias = alias;
    }
}
