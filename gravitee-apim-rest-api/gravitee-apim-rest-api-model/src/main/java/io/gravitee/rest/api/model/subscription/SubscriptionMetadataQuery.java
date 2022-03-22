/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.subscription;

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;
import java.util.function.BiFunction;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionMetadataQuery {

    private String organization;

    private String environment;

    private Collection<SubscriptionEntity> subscriptions;

    private boolean withApplications = false;

    private boolean withPlans = false;

    private boolean withApis = false;

    private boolean withSubscribers = false;

    private boolean details = false;

    private Map<DelegateType, List<BiFunction>> delegates = new HashMap<>();

    public SubscriptionMetadataQuery(String organization, String environment, Collection<SubscriptionEntity> subscriptions) {
        this.organization = organization;
        this.environment = environment;
        this.subscriptions = subscriptions;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Collection<SubscriptionEntity> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Collection<SubscriptionEntity> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Optional<Boolean> ifApplications() {
        return ifTrue(withApplications);
    }

    private Optional<Boolean> ifTrue(boolean feature) {
        if (feature) {
            return Optional.of(true);
        }
        return Optional.empty();
    }

    public SubscriptionMetadataQuery withApplications(boolean withApplications) {
        this.withApplications = withApplications;
        return this;
    }

    public Optional<Boolean> ifPlans() {
        return ifTrue(withPlans);
    }

    public SubscriptionMetadataQuery withPlans(boolean withPlans) {
        this.withPlans = withPlans;
        return this;
    }

    public Optional<Boolean> ifApis() {
        return ifTrue(withApis);
    }

    public SubscriptionMetadataQuery withApis(boolean withApis) {
        this.withApis = withApis;
        return this;
    }

    public Optional<Boolean> ifSubscribers() {
        return ifTrue(withSubscribers);
    }

    public SubscriptionMetadataQuery withSubscribers(boolean withSubscribers) {
        this.withSubscribers = withSubscribers;
        return this;
    }

    public boolean hasDetails() {
        return details;
    }

    public SubscriptionMetadataQuery excludeDetails() {
        this.details = false;
        return this;
    }

    public SubscriptionMetadataQuery includeDetails() {
        this.details = true;
        return this;
    }

    public SubscriptionMetadataQuery fillMetadata(DelegateType type, BiFunction<Metadata, ?, ?>... delegate) {
        this.delegates.put(type, Arrays.asList(delegate));
        return this;
    }

    public SubscriptionMetadataQuery fillApiMetadata(BiFunction<Metadata, ApiEntity, ApiEntity>... delegate) {
        return this.fillMetadata(DelegateType.API, delegate);
    }

    public List<BiFunction> getApiDelegate() {
        if (this.delegates.containsKey(DelegateType.API)) {
            return this.delegates.get(DelegateType.API);
        }
        return Collections.emptyList();
    }

    @Schema(enumAsRef = true)
    public enum DelegateType {
        API,
    }
}
