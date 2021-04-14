/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author Guillaume GILLON
 */
public class MediaEntity {

    private String id;
    private String type;
    private String subType;
    private String fileName;
    private Date createAt;
    private long size;
    private byte[] data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setUploadDate(Date createAt) {
        this.createAt = createAt;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getMimeType() {
        return this.type + '/' + this.subType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaEntity mediaEntity = (MediaEntity) o;
        return Objects.equals(id, mediaEntity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toString() {
        return (
            "MediaEntity{" +
            "id='" +
            id +
            '\'' +
            ", type='" +
            type +
            '\'' +
            ", subType='" +
            subType +
            '\'' +
            ", filename='" +
            fileName +
            '\'' +
            ", createAt=" +
            createAt +
            ", size=" +
            size +
            '}'
        );
    }
}
