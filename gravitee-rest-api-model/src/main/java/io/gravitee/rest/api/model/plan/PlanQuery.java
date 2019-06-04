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
package io.gravitee.rest.api.model.plan;

import io.gravitee.rest.api.model.PlanSecurityType;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class PlanQuery {

    private String api;
    private String name;
    private PlanSecurityType security;

    private PlanQuery(Builder builder) {
        api = builder.api;
        name = builder.name;
        security = builder.security;
    }

    public String getApi() {
        return api;
    }

    public String getName() {
        return name;
    }

    public PlanSecurityType getSecurity() {
        return security;
    }

    public static class Builder {
        private String api;
        private String name;
        private PlanSecurityType security;

        public PlanQuery build() {
            return new PlanQuery(this);
        }

        public Builder api(String api) {
            this.api = api;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder security(PlanSecurityType security) {
            this.security = security;
            return this;
        }
    }
}
