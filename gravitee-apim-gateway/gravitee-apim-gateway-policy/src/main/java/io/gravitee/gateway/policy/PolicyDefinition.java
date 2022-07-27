package io.gravitee.gateway.policy;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyDefinition {

    private final String name;

    private final String configuration;

    public PolicyDefinition(String name, String configuration) {
        this.name = name;
        this.configuration = configuration;
    }

    public String getName() {
        return name;
    }

    public String getConfiguration() {
        return configuration;
    }
}
