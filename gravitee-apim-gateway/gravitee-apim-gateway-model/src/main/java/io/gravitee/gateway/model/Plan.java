package io.gravitee.gateway.model;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Plan {

    private String id;

    private final String name;

    private final String type;

    private boolean requiresSubscription;

    private List<Flow> flows;

    public Plan(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isRequiresSubscription() {
        return requiresSubscription;
    }

    public void setRequiresSubscription(boolean requiresSubscription) {
        this.requiresSubscription = requiresSubscription;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }
}
