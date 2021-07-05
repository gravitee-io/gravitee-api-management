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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageMedia {

	private String mediaHash;
	private String mediaName;
	private Date attachedAt;

	public PageMedia() {
	}

	public PageMedia(String mediaHash, String mediaName, Date attachedAt) {
		this.mediaHash = mediaHash;
		this.mediaName = mediaName;
		this.attachedAt = attachedAt;
	}

	public String getMediaHash() {
		return mediaHash;
	}

	public void setMediaHash(String mediaHash) {
		this.mediaHash = mediaHash;
	}

	public String getMediaName() {
		return mediaName;
	}

	public void setMediaName(String mediaName) {
		this.mediaName = mediaName;
	}

	public Date getAttachedAt() {
		return attachedAt;
	}

	public void setAttachedAt(Date attachedAt) {
		this.attachedAt = attachedAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PageMedia pageMedia = (PageMedia) o;
		return Objects.equals(mediaHash, pageMedia.mediaHash) &&
				Objects.equals(mediaName, pageMedia.mediaName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mediaHash, mediaName);
	}

	@Override
	public String toString() {
		return "PageMedia{" +
				"mediaHash='" + mediaHash + '\'' +
				", mediaName='" + mediaName + '\'' +
				", attachedAt='" + attachedAt + '\'' +
				'}';
	}
}
