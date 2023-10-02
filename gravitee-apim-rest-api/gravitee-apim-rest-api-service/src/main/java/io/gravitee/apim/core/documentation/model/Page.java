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
package io.gravitee.apim.core.documentation.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    private String id;

    /**
     * The page crossId uniquely identifies a page across environments.
     * Pages promoted between environments will share the same crossId.
     */
    private String crossId;

    private String referenceId;
    private ReferenceType referenceType;
    private String name;
    private Type type;
    private String content;
    private String lastContributor;
    private int order;
    private boolean published;
    private Visibility visibility;
    private boolean homepage;
    private Date createdAt;
    private Date updatedAt;
    private String parentId;

    public enum Visibility {
        PUBLIC,
        PRIVATE,
    }

    public enum Type {
        FOLDER,
        MARKDOWN,
    }

    public enum ReferenceType {
        ENVIRONMENT,
        API,
    }
}
