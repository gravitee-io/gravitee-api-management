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

import io.gravitee.common.utils.UUID;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class Command {

    private String id;
    private String environmentId;
    private String organizationId;
    private String from;
    private String to;
    private List<String> tags;
    private String content;
    private List<String> acknowledgments;
    private Date expiredAt;
    private Date createdAt;
    private Date updatedAt;

    public Command() {
        var now = new Date();
        this.id = UUID.random().toString();
        this.expiredAt = Date.from(now.toInstant().plus(Duration.ofMinutes(5)));
        this.createdAt = now;
        this.updatedAt = now;
    }
}
