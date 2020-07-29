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
 * @author Eric LELEU
 */
public class PageRevisionEntity {

    private String pageId;
    private int revision;
    private String name;
    private String hash;
    private String content;
    private String contributor;
    private Date modificationDate;

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageRevisionEntity)) {
            return false;
        }
        PageRevisionEntity that = (PageRevisionEntity) o;
        return Objects.equals(pageId, that.pageId) && Objects.equals(revision, that.revision) ;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public int hashCode() {
        return Objects.hash(pageId, revision);
    }

	@Override
	public String toString() {
		return "PageEntity{" +
				" name='" + name + '\'' +
				", pageId='" + pageId + '\'' +
				", revision=" + revision +
				", content='" + content + '\'' +
				", hash='" + hash + '\'' +
				", contributor='" + contributor + '\'' +
				", modificationDate=" + modificationDate +
				'}';
	}
}
