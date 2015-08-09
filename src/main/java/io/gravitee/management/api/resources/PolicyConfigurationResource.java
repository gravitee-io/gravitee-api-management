package io.gravitee.management.api.resources;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyConfigurationResource {

    private String apiName;
    private String policyName;


    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }
}
