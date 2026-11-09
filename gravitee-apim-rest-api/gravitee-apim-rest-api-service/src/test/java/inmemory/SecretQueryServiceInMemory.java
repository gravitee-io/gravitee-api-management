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

import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.query_service.SecretQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Comparator;

public class SecretQueryServiceInMemory extends AbstractQueryServiceInMemory<Secret> implements SecretQueryService {

    @Override
    public Page<Secret> search(String organizationId, String environmentId, Pageable pageable) {
        var all = storage
            .stream()
            .filter(s -> s.organizationId().equals(organizationId) && s.environmentId().equals(environmentId))
            .sorted(Comparator.comparing(Secret::key))
            .toList();
        var content = all
            .stream()
            .skip((long) (pageable.getPageNumber() - 1) * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .toList();
        return new Page<>(content, pageable.getPageNumber(), content.size(), all.size());
    }
}
