package io.gravitee.gateway.resource;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceDefinition {

    private boolean enabled = true;

    private final String name;

    private final String type;

    private final String configuration;

    public ResourceDefinition(String name, String type, String configuration) {
        this.name = name;
        this.type = type;
        this.configuration = configuration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getConfiguration() {
        return configuration;
    }
}
