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
package io.gravitee.rest.api.model.quality;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpdateApiQualityRuleEntity {

    private String api;

    @JsonProperty("quality_rule")
    private String qualityRule;

    private boolean checked;

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

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateApiQualityRuleEntity that = (UpdateApiQualityRuleEntity) o;
        return Objects.equals(api, that.api) && Objects.equals(qualityRule, that.qualityRule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api, qualityRule);
    }

    @Override
    public String toString() {
        return "NewApiQualityRuleEntity{" + "api='" + api + '\'' + ", qualityRule='" + qualityRule + '\'' + ", checked=" + checked + '}';
    }
}
