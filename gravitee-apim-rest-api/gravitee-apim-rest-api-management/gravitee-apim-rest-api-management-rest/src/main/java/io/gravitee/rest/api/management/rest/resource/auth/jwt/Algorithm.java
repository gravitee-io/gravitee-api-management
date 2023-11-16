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
package io.gravitee.rest.api.management.rest.resource.auth.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@RequiredArgsConstructor
public enum Algorithm {
    RS256("RS256"),
    RS384("RS384"),
    RS512("RS512"),
    HS256("HS256"),
    HS384("HS384"),
    HS512("HS512"),
    ES256("ES256"),
    ES384("ES384"),
    ES512("ES512");

    public final String alg;

    public JWSAlgorithm getAlgorithm() {
        return JWSAlgorithm.parse(this.alg);
    }
}
