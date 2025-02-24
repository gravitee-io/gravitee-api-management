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
package io.gravitee.repository.management.model;

import io.gravitee.common.utils.TimeProvider;
import java.util.Date;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class AsyncJob {

    String id;
    String sourceId;
    String environmentId;
    String initiatorId;
    String type;

    Date createdAt;
    Date updatedAt;

    @Nullable
    Date deadLine;

    String status;
    String errorMessage;

    Long upperLimit;

    public boolean isLate() {
        return "PENDING".equalsIgnoreCase(status) && deadLine != null && deadLine.before(Date.from(TimeProvider.instantNow()));
    }

    public boolean isTimedOut() {
        return "TIMEOUT".equalsIgnoreCase(status);
    }
}
