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

import io.gravitee.apim.core.portal.validation.NavigationPathValidator;
import io.gravitee.apim.core.portal_listing.domain_service.ValidatePortalListingDomainService;
import io.gravitee.apim.core.portal_listing.model.PortalListingApiEntry;
import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors {@code ValidatePortalListingDomainServiceImpl}. Uses the same shared
 * {@link NavigationPathValidator} so the test path stays aligned with production.
 */
public class ValidatePortalListingDomainServiceInMemory implements ValidatePortalListingDomainService {

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        var errors = new ArrayList<Error>();
        List<PortalListingApiEntry> apis = input.apis() == null ? List.of() : input.apis();
        for (int i = 0; i < apis.size(); i++) {
            errors.addAll(NavigationPathValidator.validate(apis.get(i).location(), "apis[" + i + "].location"));
        }
        return Result.ofBoth(input, errors);
    }
}
