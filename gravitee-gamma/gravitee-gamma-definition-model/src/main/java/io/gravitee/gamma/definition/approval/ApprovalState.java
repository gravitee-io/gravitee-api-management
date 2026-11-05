/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.definition.approval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a gateway needs to know about an approval request, published by AIM as a {@code PUBLISH_APPROVAL_STATE} latest
 * event after every change. It never carries the call's arguments. Timestamps are UTC ISO-8601 instants as
 * {@link java.time.Instant#toString()} writes them, compared once parsed, never as text; {@code status} and
 * {@code scope} are strings so that a gateway keeps working when a newer control plane adds a value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalState implements Serializable {

    @JsonProperty(required = true)
    private String approvalRequestId;

    /** Ids other gateways proposed for the same call before they saw this request, which AIM joined into it. */
    @JsonProperty
    private List<String> aliasIds;

    @JsonProperty(required = true)
    private String environmentId;

    @JsonProperty(required = true)
    private String apiId;

    @JsonProperty(required = true)
    private String ruleId;

    @JsonProperty(required = true)
    private String ruleUpdatedAt;

    /**
     * Identifies the same call, so a client calling again on any node finds this request; {@code null} once the
     * request is released (it ran, or its rejection or expiry was told, or it ended), so the same call opens a new one.
     */
    @JsonProperty
    private String openKey;

    /** An {@link ApprovalStatus} name, read with {@link ApprovalStatus#parse(String)}. */
    @JsonProperty(required = true)
    private String status;

    /** When the call was first held. */
    @JsonProperty(required = true)
    private String createdAt;

    /**
     * When the call started running, or, for a call that ran without its start reported, when its outcome came back;
     * {@code null} before it runs, and from a control plane that does not send it.
     */
    @JsonProperty
    private String startedAt;

    /** When a pending request is denied if nobody decides. */
    @JsonProperty
    private String expiresAt;

    /** The approver's reason for a rejection. */
    @JsonProperty
    private String reason;

    @JsonProperty
    private Grant grant;

    @JsonProperty
    private ExecutorLease executor;

    /** The gateway forgets this state after it, and AIM deletes the latest event a little later. */
    @JsonProperty(required = true)
    private String keepUntil;

    /** When AIM published this copy. */
    @JsonProperty(required = true)
    private String updatedAt;

    /**
     * Raised by AIM on every change of the request: a gateway keeps the copy with the highest version, whichever
     * arrives last. {@code null} from a control plane that does not send it, which then orders copies by
     * {@code updatedAt}.
     */
    @JsonProperty
    private Long version;

    /** What an approval covers: this call once, or the same call again until {@code coversUntil}. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Grant implements Serializable {

        /** ONCE or TIMED. */
        @JsonProperty(required = true)
        private String scope;

        @JsonProperty
        private String coversUntil;

        @JsonProperty(required = true)
        private ApprovalCaller caller;

        @JsonProperty(required = true)
        private String argumentsDigest;
    }

    /** The one node allowed to run the approved call, and until when. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExecutorLease implements Serializable {

        @JsonProperty(required = true)
        private String nodeId;

        @JsonProperty(required = true)
        private String leaseId;

        @JsonProperty(required = true)
        private String leaseUntil;
    }
}
