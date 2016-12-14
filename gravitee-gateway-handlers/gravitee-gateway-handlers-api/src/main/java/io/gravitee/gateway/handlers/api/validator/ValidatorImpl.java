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
package io.gravitee.gateway.handlers.api.validator;

import io.gravitee.gateway.handlers.api.definition.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ValidatorImpl implements Validator {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorImpl.class);

    private static final Validator [] VALIDATORS = {
            new ApiValidator(),
            new ProxyValidator(),
            new PathValidator(),
            new HttpClientValidator()
    };

    @Override
    public void validate(Api definition) {
        logger.debug("Validate API Definition for API: {}", definition.getName());

        for (Validator validator : VALIDATORS) {
            validator.validate(definition);
        }
    }
}
