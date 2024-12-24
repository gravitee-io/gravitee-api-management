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
package io.gravitee.definition.model.v4.http;

import io.gravitee.secrets.api.annotation.Secret;
import io.gravitee.secrets.api.el.FieldKind;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpProxyOptions implements Serializable {

    @Serial
    private static final long serialVersionUID = -1133018497662951240L;

    private boolean enabled;
    private boolean useSystemProxy;

    private String host;

    private int port;

    @Secret
    private String username;

    @Secret(FieldKind.PASSWORD)
    private String password;

    @Builder.Default
    private HttpProxyType type = HttpProxyType.HTTP;
}
