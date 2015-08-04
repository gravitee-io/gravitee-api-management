package io.gravitee.management.api.builder;

import io.gravitee.repository.model.Api;

import java.net.URI;
import java.util.Date;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiBuilder {

    private final Api api = new Api();

    public ApiBuilder name(String name) {
        this.api.setName(name);
        return this;
    }

    public ApiBuilder target(String target) {
        this.api.setTargetURI(URI.create(target));
        return this;
    }

    public ApiBuilder origin(String origin) {
        this.api.setPublicURI(URI.create(origin));
        return this;
    }

    public ApiBuilder createdAt(Date createdAt) {
        this.api.setCreatedAt(createdAt);
        this.api.setUpdatedAt(createdAt);
        return this;
    }

    public ApiBuilder updatedAt(Date updatedAt) {
        this.api.setUpdatedAt(updatedAt);
        return this;
    }

    public Api build() {
        return this.api;
    }
}
