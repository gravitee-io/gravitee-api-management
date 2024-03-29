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
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MembershipAlreadyExistsException extends AbstractManagementException {

    private final String memberId;
    private final MembershipMemberType memberType;
    private final String referenceId;
    private final MembershipReferenceType referenceType;

    public MembershipAlreadyExistsException(
        String memberId,
        MembershipMemberType memberType,
        String referenceId,
        MembershipReferenceType referenceType
    ) {
        super();
        this.memberId = memberId;
        this.memberType = memberType;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatusCode.BAD_REQUEST_400;
    }

    @Override
    public String getMessage() {
        return (
            "A Membership for member : " +
            memberType +
            " " +
            memberId +
            " and ref : " +
            referenceType +
            " " +
            referenceId +
            " already exists."
        );
    }

    @Override
    public String getTechnicalCode() {
        return "api.exists";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("memberId", memberId);
        parameters.put("memberType", memberType.name());
        parameters.put("referenceId", referenceId);
        parameters.put("referenceType", referenceType.name());
        return parameters;
    }
}
