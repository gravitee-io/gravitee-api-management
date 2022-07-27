package io.gravitee.gateway.handlers.api.definition;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.reactor.Reactable;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ReactableApi<T> implements Reactable, Serializable {

    protected final T definition;

    private boolean enabled = true;

    private Date deployedAt;

    private String environmentId;

    private String environmentHrid;

    private String organizationId;

    private String organizationHrid;

    private DefinitionContext definitionContext = new DefinitionContext();

    protected ReactableApi(T definition) {
        this.definition = definition;
    }

    public abstract String getApiVersion();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean enabled() {
        return isEnabled();
    }

    public Date getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Date deployedAt) {
        this.deployedAt = deployedAt;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getEnvironmentHrid() {
        return environmentHrid;
    }

    public void setEnvironmentHrid(String environmentHrid) {
        this.environmentHrid = environmentHrid;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationHrid() {
        return organizationHrid;
    }

    public void setOrganizationHrid(String organizationHrid) {
        this.organizationHrid = organizationHrid;
    }

    public DefinitionContext getDefinitionContext() {
        return definitionContext;
    }

    public void setDefinitionContext(DefinitionContext definitionContext) {
        this.definitionContext = definitionContext;
    }

    public T getDefinition() {
        return this.definition;
    }

    public abstract DefinitionVersion getDefinitionVersion();

    public abstract Set<String> getTags();

    public abstract String getId();

    public abstract String getName();

    public abstract List<Plan> getPlans();

    @Override
    public String toString() {
        return "API " + "id[" + this.getId() + "] name[" + this.getName() + "] version[" + this.getApiVersion() + ']';
    }
}
