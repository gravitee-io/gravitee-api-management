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
package io.gravitee.repository.media.model;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume Gillon
 */

@Getter
@Setter
@ToString
public class Media {

    private String id;
    private String type;
    private String subType;
    private String fileName;
    private Date createdAt;
    private String hash;
    private Long size;
    private byte[] data;

    private String api;
    private String environment;
    private String organization;

    public Media() {}

    public Media(String type, String subType, String fileName, long size) {
        this.type = type;
        this.subType = subType;
        this.fileName = fileName;
        this.size = size;
    }
}
