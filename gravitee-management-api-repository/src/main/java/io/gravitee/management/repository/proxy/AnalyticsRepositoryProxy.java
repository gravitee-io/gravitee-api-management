package io.gravitee.management.repository.proxy;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.response.HealthResponse;
import io.gravitee.repository.analytics.query.response.Response;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class AnalyticsRepositoryProxy extends AbstractProxy<AnalyticsRepository> implements AnalyticsRepository {

    @Override
    public <T extends Response> T query(Query<T> query) throws AnalyticsException {
        return target.query(query);
    }

    @Override
    public HealthResponse query(String s, long l, long l1, long l2) throws AnalyticsException {
        return target.query(s, l, l1, l2);
    }
}
