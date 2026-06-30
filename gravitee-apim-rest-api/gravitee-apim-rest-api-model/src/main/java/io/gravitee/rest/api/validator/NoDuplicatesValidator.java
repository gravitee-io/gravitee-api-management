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
package io.gravitee.rest.api.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author GraviteeSource Team
 */
public class NoDuplicatesValidator implements ConstraintValidator<NoDuplicates, List<String>> {

    private boolean ignoreCase;

    @Override
    public void initialize(NoDuplicates constraint) {
        this.ignoreCase = constraint.ignoreCase();
    }

    @Override
    public boolean isValid(List<String> values, ConstraintValidatorContext context) {
        if (values == null) {
            return true;
        }
        final Set<String> seen = new HashSet<>();
        for (final String value : values) {
            if (value == null) {
                continue;
            }
            final String trimmed = value.trim();
            final String key;
            if (ignoreCase) {
                key = trimmed.toLowerCase(Locale.ROOT);
            } else {
                key = trimmed;
            }
            if (!seen.add(key)) {
                return false;
            }
        }
        return true;
    }
}
