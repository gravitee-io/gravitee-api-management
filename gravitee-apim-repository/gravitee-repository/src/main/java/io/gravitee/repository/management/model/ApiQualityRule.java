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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiQualityRule {
    public enum AuditEvent implements Audit.AuditEvent {
        API_QUALITY_RULE_CREATED, API_QUALITY_RULE_UPDATED, API_QUALITY_RULE_DELETED
    }

    private String api;
    private String qualityRule;
    private boolean checked;
    private Date createdAt;
    private Date updatedAt;

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiQualityRule that = (ApiQualityRule) o;
        return Objects.equals(api, that.api) &&
                Objects.equals(qualityRule, that.qualityRule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api, qualityRule);
    }

    @Override
    public String toString() {
        return "ApiQualityRule{" +
                "api='" + api + '\'' +
                ", qualityRule='" + qualityRule + '\'' +
                ", checked=" + checked +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
