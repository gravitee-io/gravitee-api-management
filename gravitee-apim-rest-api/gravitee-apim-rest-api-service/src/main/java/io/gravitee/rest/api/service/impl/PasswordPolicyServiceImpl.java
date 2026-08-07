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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.model.PasswordPolicyEntity;
import io.gravitee.rest.api.service.PasswordPolicyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private static final String DEFAULT_DESCRIPTION =
        "Password must be at least 12 characters long, contain at least one digit, one upper case letter, one lower case letter, one special character, and no more than 2 consecutive equal characters.";

    private final PasswordPolicyPatternParser patternParser = new PasswordPolicyPatternParser();

    @Value("${user.password.policy.description:}")
    private String passwordPolicyDescription;

    @Value(
        "${user.password.policy.pattern:^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[!~<>.,;:_=?/*+\\-#\\\"'&§`£€%°()|\\[\\]$^@])(?!.*(.)\\1{2,}).{12,128}$}"
    )
    private String passwordPolicyPattern;

    @Override
    public PasswordPolicyEntity getPasswordPolicy() {
        return PasswordPolicyEntity.builder()
            .description(StringUtils.hasText(passwordPolicyDescription) ? passwordPolicyDescription.trim() : DEFAULT_DESCRIPTION)
            .pattern(passwordPolicyPattern)
            .rules(patternParser.parse(passwordPolicyPattern))
            .build();
    }
}
