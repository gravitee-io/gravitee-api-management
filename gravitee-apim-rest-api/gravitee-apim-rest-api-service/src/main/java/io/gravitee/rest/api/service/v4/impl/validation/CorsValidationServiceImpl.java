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
package io.gravitee.rest.api.service.v4.impl.validation;

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.exceptions.AllowOriginNotAllowedException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CorsValidationServiceImpl extends TransactionalService implements CorsValidationService {

    private static final Pattern CORS_REGEX_PATTERN = Pattern.compile("^((\\*)|(null)|(^(([^:\\/?#]+):)?(\\/\\/([^\\/?#]*))?))$");
    private static final String[] CORS_REGEX_CHARS = new String[] { "{", "[", "(", "*" };

    @Override
    public Cors validateAndSanitize(Cors cors) {
        if (cors != null) {
            final Set<String> accessControlAllowOrigin = cors.getAccessControlAllowOrigin();
            if (accessControlAllowOrigin != null && !accessControlAllowOrigin.isEmpty()) {
                for (String allowOriginItem : accessControlAllowOrigin) {
                    if (!CORS_REGEX_PATTERN.matcher(allowOriginItem).matches()) {
                        if (StringUtils.indexOfAny(allowOriginItem, CORS_REGEX_CHARS) >= 0) {
                            try {
                                //the origin could be a regex
                                Pattern.compile(allowOriginItem);
                            } catch (PatternSyntaxException e) {
                                throw new AllowOriginNotAllowedException(allowOriginItem);
                            }
                        } else {
                            throw new AllowOriginNotAllowedException(allowOriginItem);
                        }
                    }
                }
            }
        }
        return cors;
    }
}
