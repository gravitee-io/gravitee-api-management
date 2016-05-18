package io.gravitee.management.repository.proxy;

import io.gravitee.repository.monitoring.MonitoringRepository;
import io.gravitee.repository.monitoring.model.MonitoringResponse;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Component
public class MonitoringRepositoryProxy extends AbstractProxy<MonitoringRepository> implements MonitoringRepository {

    @Override
    public MonitoringResponse query(String s) {
        return target.query(s);
    }
}
