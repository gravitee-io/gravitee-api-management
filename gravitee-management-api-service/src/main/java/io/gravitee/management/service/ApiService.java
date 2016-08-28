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
package io.gravitee.management.service;

import io.gravitee.management.model.*;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApiService {

    ApiEntity findById(String apiId);

    Set<ApiEntity> findAll();

    Set<ApiEntity> findByUser(String username);

    Set<ApiEntity> findByVisibility(Visibility visibility);
    
    int countByApplication(String applicationId);

    ApiEntity create(NewApiEntity api, String username);

    ApiEntity update(String apiId, UpdateApiEntity api);

    void delete(String apiId);

    void start(String apiId, String username);

    void stop(String apiId, String username);

    Set<MemberEntity> getMembers(String apiId, MembershipType membershipType);

    MemberEntity getMember(String apiId, String username);

    void addOrUpdateMember(String apiId, String username, MembershipType membershipType);

    void deleteMember(String apiId, String username);
    
    boolean isAPISynchronized(String apiId);
    
    ApiEntity deploy(String apiId, String username, EventType eventType);
    
    ApiEntity rollback(String apiId, UpdateApiEntity api);

    String exportAsJson(String apiId, MembershipType membershipType);

    ApiEntity createOrUpdateWithDefinition(ApiEntity apiEntity, String apiDefinition, String username);

    ImageEntity getPicture(String apiId);
}
