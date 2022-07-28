package io.gravitee.gateway.model;

import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.step.Step;

import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Flow {

    private String name;

    private boolean enabled = true;

    private List<Selector> selectors;

    private List<Step> request;

    private List<Step> response;

    private List<Step> subscribe;

    private List<Step> publish;

    private Set<String> tags;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Selector> getSelectors() {
        return selectors;
    }

    public void setSelectors(List<Selector> selectors) {
        this.selectors = selectors;
    }

    public List<Step> getRequest() {
        return request;
    }

    public void setRequest(List<Step> request) {
        this.request = request;
    }

    public List<Step> getResponse() {
        return response;
    }

    public void setResponse(List<Step> response) {
        this.response = response;
    }

    public List<Step> getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(List<Step> subscribe) {
        this.subscribe = subscribe;
    }

    public List<Step> getPublish() {
        return publish;
    }

    public void setPublish(List<Step> publish) {
        this.publish = publish;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
