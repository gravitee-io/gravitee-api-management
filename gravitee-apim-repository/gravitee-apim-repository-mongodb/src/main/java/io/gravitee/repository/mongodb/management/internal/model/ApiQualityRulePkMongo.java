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
package io.gravitee.repository.mongodb.management.internal.model;

import java.io.Serializable;
import java.util.Objects;
import org.springframework.data.annotation.Id;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiQualityRulePkMongo implements Serializable {

    private String api;
    private String qualityRule;

    public ApiQualityRulePkMongo() {}

    public ApiQualityRulePkMongo(String api, String qualityRule) {
        this.api = api;
        this.qualityRule = qualityRule;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getQualityRule() {
        return qualityRule;
    }

    public void setQualityRule(String qualityRule) {
        this.qualityRule = qualityRule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiQualityRulePkMongo that = (ApiQualityRulePkMongo) o;
        return Objects.equals(api, that.api) && Objects.equals(qualityRule, that.qualityRule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api, qualityRule);
    }

    @Override
    public String toString() {
        return "ApiQualityRulePkMongo{" + "api='" + api + '\'' + ", qualityRule='" + qualityRule + '\'' + '}';
    }
}
