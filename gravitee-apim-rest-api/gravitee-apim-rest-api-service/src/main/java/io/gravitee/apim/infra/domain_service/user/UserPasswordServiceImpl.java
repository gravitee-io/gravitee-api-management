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
package io.gravitee.apim.infra.domain_service.user;

import io.gravitee.apim.core.user.model.EncodedPassword;
import io.gravitee.apim.core.user.model.RawPassword;
import io.gravitee.apim.core.user.service_provider.UserPasswordService;
import io.gravitee.rest.api.service.PasswordValidator;
import io.gravitee.rest.api.service.exceptions.PasswordFormatInvalidException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserPasswordServiceImpl implements UserPasswordService {

    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserPasswordServiceImpl(PasswordValidator passwordValidator) {
        this.passwordValidator = passwordValidator;
    }

    @Override
    public void validate(RawPassword password) {
        if (!passwordValidator.validate(password.value())) {
            throw new PasswordFormatInvalidException();
        }
    }

    @Override
    public EncodedPassword encode(RawPassword password) {
        return new EncodedPassword(passwordEncoder.encode(password.value()));
    }
}
