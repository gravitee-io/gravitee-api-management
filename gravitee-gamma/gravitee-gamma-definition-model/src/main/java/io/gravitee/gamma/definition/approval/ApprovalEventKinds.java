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

/**
 * The kinds of the generic events that carry approval rules and approval states from the control plane to the gateways,
 * written once for both sides.
 */
public final class ApprovalEventKinds {

    public static final String FEATURE = "approval";
    /** The payload is an {@link ApprovalRule}; the event id is the rule id. */
    public static final String RULE = FEATURE + ".rule";
    /** The payload is an {@link ApprovalState}; the event id is the approval request id. */
    public static final String STATE = FEATURE + ".state";

    private ApprovalEventKinds() {}
}
