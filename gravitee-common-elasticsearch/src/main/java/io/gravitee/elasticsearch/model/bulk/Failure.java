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
package io.gravitee.elasticsearch.model.bulk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Represents a failure in Elasticsearch 2.x.
 * 
 * @see "https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/action/bulk/BulkItemResponse.java"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Failure implements Serializable {
	
	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The type of the action.
	 */
	private String type;

	/**
	 * The failure message.
	 */
	private String reason;

	/**
	 * The failure cause.
	 */
	@JsonProperty("caused_by")
	private CausedBy causedBy;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public CausedBy getCausedBy() {
		return causedBy;
	}

	public void setCausedBy(CausedBy causedBy) {
		this.causedBy = causedBy;
	}
}


