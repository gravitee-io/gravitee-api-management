package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.management.model.Visibility;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class ApiRepositoryProxy extends AbstractProxy<ApiRepository> implements ApiRepository {

    @Override
    public void deleteMember(String s, String s1) throws TechnicalException {
        target.deleteMember(s, s1);
    }

    @Override
    public Set<Api> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<Api> findByMember(String s, MembershipType membershipType, Visibility visibility) throws TechnicalException {
        return target.findByMember(s, membershipType, visibility);
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
    public Api create(Api api) throws TechnicalException {
        return target.create(api);
    }

    @Override
    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    @Override
    public Optional<Api> findById(String s) throws TechnicalException {
        return target.findById(s);
    }

    @Override
    public Api update(Api api) throws TechnicalException {
        return target.update(api);
    }
}
