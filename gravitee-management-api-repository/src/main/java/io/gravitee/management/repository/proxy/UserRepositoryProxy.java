package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class UserRepositoryProxy extends AbstractProxy<UserRepository> implements UserRepository {

    @Override
    public User create(User user) throws TechnicalException {
        return target.create(user);
    }

    @Override
    public Set<User> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Optional<User> findByUsername(String s) throws TechnicalException {
        return target.findByUsername(s);
    }
}
