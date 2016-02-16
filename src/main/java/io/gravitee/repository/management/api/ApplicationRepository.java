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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.*;

import java.util.Collection;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApplicationRepository extends CrudRepository<Application, String> {

    /**
     * List all applications.
     *
     * @return All public applications.
     */
    Set<Application> findAll() throws TechnicalException;

    /**
     * List all applications hold by a {@link User}.
     *
     * @param username The name of the user.
     * @return All applications from a user.
     */
    Set<Application> findByUser(String username, MembershipType membershipType) throws TechnicalException;

    void saveMember(String applicationId, String username, MembershipType membershipType) throws TechnicalException;

    void deleteMember(String applicationId, String username) throws TechnicalException;

    Collection<Membership> getMembers(String applicationId, MembershipType membershipType) throws TechnicalException;

    Membership getMember(String applicationId, String username) throws TechnicalException;
}