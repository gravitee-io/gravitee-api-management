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

import io.gravitee.apim.core.member.domain_service.CRDMembersDomainService;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import java.util.HashMap;
import java.util.Set;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CRDMembersDomainServiceInMemory implements CRDMembersDomainService {

    private final HashMap<String, Set<MemberCRD>> apiMembers = new HashMap<>();
    private final HashMap<String, Set<MemberCRD>> applicationMembers = new HashMap<>();

    @Override
    public void updateApiMembers(String organizationId, String apiId, Set<MemberCRD> members) {
        apiMembers.put(apiId, members);
    }

    @Override
    public void updateApplicationMembers(String organizationId, String applicationId, Set<MemberCRD> members) {
        applicationMembers.put(applicationId, members);
    }

    public void reset() {
        apiMembers.clear();
        applicationMembers.clear();
    }

    public Set<MemberCRD> getApiMembers(String id) {
        return apiMembers.getOrDefault(id, Set.of());
    }

    public Set<MemberCRD> getApplicationMembers(String id) {
        return applicationMembers.getOrDefault(id, Set.of());
    }
}
