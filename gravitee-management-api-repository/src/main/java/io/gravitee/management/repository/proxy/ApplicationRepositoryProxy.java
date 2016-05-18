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
