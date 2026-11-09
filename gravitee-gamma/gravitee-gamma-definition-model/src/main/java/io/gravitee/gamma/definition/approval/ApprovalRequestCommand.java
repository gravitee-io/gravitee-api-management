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
import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Sent by the gateway holding a call a rule matched, as an {@code APPROVAL_REQUEST} command. AIM treats every field as
 * untrusted: it recomputes the digest, checks the rule and the API belong to the environment, and is idempotent, since
 * two Management API nodes may both read the command before it is deleted.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalRequestCommand implements Serializable {

    /** The first time a call is held, the client coming back, or a node asking to run an approved call. */
    public enum Kind {
        OPEN,
        RETURNED,
        HANDOVER,
    }

    /** Proposed by the gateway for a new request, or the id the client presented. */
    @JsonProperty(required = true)
    private String approvalRequestId;

    /** A {@link Kind} name. */
    @JsonProperty(required = true)
    private String kind;

    @JsonProperty(required = true)
    private String organizationId;

    @JsonProperty(required = true)
    private String environmentId;

    @JsonProperty(required = true)
    private String apiId;

    @JsonProperty(required = true)
    private String ruleId;

    /** The {@code updatedAt} of the rule copy that matched, so AIM knows which version held the call. */
    @JsonProperty(required = true)
    private String ruleUpdatedAt;

    @JsonProperty(required = true)
    private String toolName;

    /**
     * Sent with an {@link Kind#OPEN} and with a {@link Kind#RETURNED}, the two commands AIM can open an approval from: the
     * catalog entity id the called tool resolved to on the gateway, which is the id the MCP reactor registers for a studio
     * tool (the resolver returns the tool name itself when none is registered). It lets AIM check a rule anchored on that
     * tool. Not sent with a {@link Kind#HANDOVER}, which only acts on an approval AIM already holds. {@code null} from a
     * gateway that predates it.
     */
    @JsonProperty
    private String toolEntityId;

    @JsonProperty(required = true)
    @ToString.Exclude
    private JsonNode arguments;

    /** {@link ArgumentsDigest#of} of the tool name and arguments. */
    @JsonProperty(required = true)
    private String argumentsDigest;

    @JsonProperty(required = true)
    private ApprovalCaller caller;

    /** {@code MRTR} or {@code CALL_AGAIN}: how the client comes back. */
    @JsonProperty(required = true)
    private String deliveryPath;

    /** The node holding the call; AIM names it as the one to run the call once approved. */
    @JsonProperty(required = true)
    private String gatewayNodeId;

    @JsonProperty(required = true)
    private String sentAt;
}
