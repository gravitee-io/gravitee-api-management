/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.idp.memory.authentication.spring;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author David Brassely (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class InMemoryAuthenticationProviderConfiguration {

    private static final String ENCODING_ALGORITHM_PROPERTY_NAME = "password-encoding-algo";

    private static final String NOOP_ALGORITHM = "none";

    private static final String BCRYPT_ALGORITHM = "bcrypt";

    @Autowired
    private Environment environment;

    @Bean
    public PasswordEncoder passwordEncoder() {
        String encodingAlgo = environment.getProperty(ENCODING_ALGORITHM_PROPERTY_NAME, BCRYPT_ALGORITHM);
        if (encodingAlgo == null || encodingAlgo.isEmpty()) {
            encodingAlgo = BCRYPT_ALGORITHM;
        }
        switch (encodingAlgo.toLowerCase()) {
            case BCRYPT_ALGORITHM:
                return new BCryptPasswordEncoder();
            case NOOP_ALGORITHM:
                return NoOpPasswordEncoder.getInstance();
            default:
                throw new IllegalArgumentException("Unsupported password encoding algorithm : " + encodingAlgo);
        }
    }

    @Bean
    public InMemoryGraviteeUserDetailsManager userDetailsService() {
        return new InMemoryGraviteeUserDetailsManager(Collections.emptyList());
    }
}
