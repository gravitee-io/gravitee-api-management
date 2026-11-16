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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sent by a gateway, as an {@code APPROVAL_OUTCOME} command, once it has run an approved call or told its client the call will not run. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalOutcomeCommand implements Serializable {

    public enum Outcome {
        /**
         * Written before the call is sent upstream, under the lease: from then on AIM never hands the call to another
         * node, however long the upstream takes.
         */
        STARTED,
        /** The upstream answered with a result. */
        EXECUTED,
        /** The upstream answered with an error. */
        UPSTREAM_ERROR,
        /** The call was sent but its answer never came back. */
        UNKNOWN,
        /** The client was told the call was rejected or expired: later calls with the same arguments start afresh. */
        CONCLUSION_TOLD,
    }

    @JsonProperty(required = true)
    private String approvalRequestId;

    @JsonProperty(required = true)
    private String organizationId;

    @JsonProperty(required = true)
    private String environmentId;

    @JsonProperty(required = true)
    private String gatewayNodeId;

    /** The executor lease the call runs under; {@code null} for {@link Outcome#CONCLUSION_TOLD}. */
    @JsonProperty
    private String leaseId;

    /** An {@link Outcome} name. */
    @JsonProperty(required = true)
    private String outcome;

    @JsonProperty(required = true)
    private String sentAt;
}
