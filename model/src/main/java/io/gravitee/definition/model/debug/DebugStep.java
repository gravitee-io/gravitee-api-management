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
package io.gravitee.definition.model.debug;

import io.gravitee.definition.model.PolicyScope;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing a debug step.
 *
 * ⚠️ There can be multiple debug steps for the same policy if the latter is updating both content and headers of the request/response.
 */
public class DebugStep {

    /**
     * An identifier for the policy instance executed or streamed dun this step
     */
    private String policyInstanceId;

    /**
     * The name of the policy executed (or streamed) during this step
     */
    private String policyId;

    /**
     * Duration to execute either headers or body modification, in nanoseconds
     */
    private Long duration;

    /**
     * "COMPLETED" | "SKIPPED" | "ERROR"
     */
    private DebugStepStatus status;

    /**
     * "ON_REQUEST" | "ON_REQUEST_CONTENT" | "ON_RESPONSE" | "ON_RESPONSE_CONTENT"
     */
    private PolicyScope scope;

    /**
     * Where the policy has been configured.
     * SECURITY | PLATFORM | PLAN | FLOW
     */
    private String stage;

    /**
     * A map containing only the headers, body, path, params, etc. that were modified during this step
     */
    private Map<String, Object> result = new HashMap<>();

    public String getPolicyInstanceId() {
        return policyInstanceId;
    }

    public void setPolicyInstanceId(String policyInstanceId) {
        this.policyInstanceId = policyInstanceId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public DebugStepStatus getStatus() {
        return status;
    }

    public void setStatus(DebugStepStatus status) {
        this.status = status;
    }

    public PolicyScope getScope() {
        return scope;
    }

    public void setScope(PolicyScope scope) {
        this.scope = scope;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
