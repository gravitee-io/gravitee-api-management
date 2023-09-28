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
package io.gravitee.apim.infra.domain_service.api;

import com.google.common.net.InternetDomainName;
import io.gravitee.apim.core.api.domain_service.ApiHostValidatorDomainService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApiHostValidatorDomainServiceImpl implements ApiHostValidatorDomainService {

    @Override
    public boolean isValidDomainOrSubDomain(String domain, List<String> domainRestrictions) {
        boolean isSubDomain = false;
        if (domainRestrictions.isEmpty()) {
            return true;
        }
        for (String domainRestriction : domainRestrictions) {
            InternetDomainName domainIDN = InternetDomainName.from(domain);
            InternetDomainName parentIDN = InternetDomainName.from(domainRestriction);

            if (domainIDN.equals(parentIDN)) {
                return true;
            }
            while (!isSubDomain && domainIDN.hasParent()) {
                isSubDomain = parentIDN.equals(domainIDN);
                domainIDN = domainIDN.parent();
            }
            if (isSubDomain) {
                break;
            }
        }
        return isSubDomain;
    }
}
