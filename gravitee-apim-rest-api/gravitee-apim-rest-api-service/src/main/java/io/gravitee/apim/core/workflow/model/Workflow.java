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
package io.gravitee.apim.core.workflow.model;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Workflow {

    private String id;
    private ReferenceType referenceType;
    private String referenceId;
    private Type type;
    private State state;
    private String comment;
    private String user;
    private ZonedDateTime createdAt;

    public enum ReferenceType {
        API,
        APPLICATION,
    }

    public enum Type {
        REVIEW,
    }

    public enum State {
        DRAFT,
        IN_REVIEW,
        REQUEST_FOR_CHANGES,
        REVIEW_OK,
    }

    public static Workflow newApiReviewWorkflow(String apiId, String userId) {
        return Workflow.builder()
            .id(UuidString.generateRandom())
            .referenceType(Workflow.ReferenceType.API)
            .referenceId(apiId)
            .type(Workflow.Type.REVIEW)
            .state(Workflow.State.DRAFT)
            .comment("")
            .user(userId)
            .createdAt(TimeProvider.now())
            .build();
    }
}
