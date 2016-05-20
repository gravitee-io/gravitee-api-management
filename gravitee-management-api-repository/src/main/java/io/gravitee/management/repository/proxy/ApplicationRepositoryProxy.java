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
package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class ApplicationRepositoryProxy extends AbstractProxy<ApplicationRepository> implements ApplicationRepository {

    @Override
    public void deleteMember(String s, String s1) throws TechnicalException {
        target.deleteMember(s, s1);
    }

    @Override
    public Set<Application> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<Application> findByUser(String s, MembershipType membershipType) throws TechnicalException {
        return target.findByUser(s, membershipType);
    }

    @Override
    public Membership getMember(String s, String s1) throws TechnicalException {
        return target.getMember(s, s1);
    }

    @Override
    public Collection<Membership> getMembers(String s, MembershipType membershipType) throws TechnicalException {
        return target.getMembers(s, membershipType);
    }

    @Override
    public void saveMember(String s, String s1, MembershipType membershipType) throws TechnicalException {
        target.saveMember(s, s1, membershipType);
    }

    @Override
    public Application create(Application application) throws TechnicalException {
        return target.create(application);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Optional<Application> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Application update(Application application) throws TechnicalException {
        return target.update(application);
    }
}
