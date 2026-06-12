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
package inmemory;

import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors {@code ValidatePortalDomainServiceImpl}. Uses the same shared
 * {@link NavigationPathValidator} so the test path stays aligned with production.
 */
public class ValidatePortalDomainServiceInMemory implements ValidatePortalDomainService {

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        List<NavigationPath> navigation = input.navigation() == null ? List.of() : input.navigation();
        for (int i = 0; i < navigation.size(); i++) {
            errors.addAll(NavigationPathValidator.validate(navigation.get(i).path(), "navigation[" + i + "].path"));
        }
        return Result.ofBoth(input, errors);
    }
}
