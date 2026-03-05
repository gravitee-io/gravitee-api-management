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

import io.gravitee.apim.core.application_member.model.ApplicationMemberSearchUser;
import io.gravitee.apim.core.application_member.query_service.ApplicationMemberUserQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApplicationMemberUserQueryServiceInMemory
    implements ApplicationMemberUserQueryService, InMemoryAlternative<ApplicationMemberSearchUser> {

    private final List<ApplicationMemberSearchUser> storage = new ArrayList<>();

    @Override
    public List<ApplicationMemberSearchUser> search(String query) {
        if (query == null || query.isBlank() || "*".equals(query)) {
            return new ArrayList<>(storage);
        }

        var normalizedQuery = query.toLowerCase(Locale.ROOT);
        return storage
            .stream()
            .filter(user -> matchesQuery(user, normalizedQuery))
            .toList();
    }

    @Override
    public void initWith(List<ApplicationMemberSearchUser> items) {
        this.storage.addAll(items);
    }

    @Override
    public void reset() {
        this.storage.clear();
    }

    @Override
    public List<ApplicationMemberSearchUser> storage() {
        return this.storage;
    }

    private boolean matchesQuery(ApplicationMemberSearchUser user, String query) {
        return (
            contains(user.displayName(), query) ||
            contains(user.email(), query) ||
            contains(user.firstName(), query) ||
            contains(user.lastName(), query) ||
            contains(user.reference(), query)
        );
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
