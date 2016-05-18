package io.gravitee.management.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class ApiKeyRepositoryProxy extends AbstractProxy<ApiKeyRepository> implements ApiKeyRepository {

    public ApiKey create(String s, String s1, ApiKey apiKey) throws TechnicalException {
        return target.create(s, s1, apiKey);
    }

    public Set<ApiKey> findByApi(String s) throws TechnicalException {
        return target.findByApi(s);
    }

    public void delete(String s) throws TechnicalException {
        target.delete(s);
    }

    public Set<ApiKey> findByApplication(String s) throws TechnicalException {
        return target.findByApplication(s);
    }

    public Set<ApiKey> findByApplicationAndApi(String s, String s1) throws TechnicalException {
        return target.findByApplicationAndApi(s, s1);
    }

    public Optional<ApiKey> retrieve(String s) throws TechnicalException {
        return target.retrieve(s);
    }

    public ApiKey update(ApiKey apiKey) throws TechnicalException {
        return target.update(apiKey);
    }
}
