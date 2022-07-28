package io.gravitee.gateway.jupiter.handlers.api.definition;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.model.Plan;
import io.gravitee.gateway.model.ReactableApi;
import io.gravitee.gateway.policy.PolicyDefinition;
import io.gravitee.gateway.reactor.handler.Entrypoint;
import io.gravitee.gateway.resource.ResourceDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api extends ReactableApi<io.gravitee.definition.model.v4.Api> {

    public Api(io.gravitee.definition.model.v4.Api api) {
        super(api);
    }

    @Override
    public String getApiVersion() {
        return definition.getApiVersion();
    }

    @Override
    public DefinitionVersion getDefinitionVersion() {
        return definition.getDefinitionVersion();
    }

    @Override
    public Set<String> getTags() {
        return definition.getTags();
    }

    @Override
    public String getId() {
        return definition.getId();
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public List<Plan> getPlans() {
        return null;
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        if (PolicyDefinition.class.equals(type)) {
            //TODO: complete me
            return null;
        } else if (ResourceDefinition.class.equals(type)) {
            //TODO: complete me
            return null;
        }

        return Collections.emptySet();
    }

    @Override
    public List<Entrypoint> entrypoints() {
        return null;
    }
}
