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
package io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.nimbusds.jose.proc.SecurityContext;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidKeyException;
import org.junit.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MACJWKSourceResolverTest {

    private MACJWKSourceResolver<SecurityContext> sourceResolver;

    @Test
    public void resolveValidPublicKey() {
        sourceResolver = new MACJWKSourceResolver<>(() -> "ozhbx5HJCS41NzKrBSQ0vZU1WOmG0Uhm");
        assertNotNull(sourceResolver.resolve());
    }

    @Test(expected = InvalidKeyException.class)
    public void shouldNotResolveInvalidPublicKey() {
        sourceResolver = new MACJWKSourceResolver<>(() -> null);
        sourceResolver.resolve();
        fail("Source resolver must fail for null public keys");
    }
}
