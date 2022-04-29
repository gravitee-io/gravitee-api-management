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
 * A response of an index operation,
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Index implements Serializable {

	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The index the document was indexed into.
	 */
	@JsonProperty("_index")
	private String indexName;

	/**
	 * The type of the document indexed.
	 */
	@JsonProperty("_type")
	private String type;

	/**
	 * The id of the document indexed.
	 */
	@JsonProperty("_id")
	private String identifier;

	/**
	 * Returns the current version of the doc indexed.
	 */
	@JsonProperty("_version")
	private String version;

	/**
	 * Response status.
	 */
	private Integer status;

	/**
	 * Optional error information.
	 */
	private Failure error;

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Failure getError() {
		return error;
	}

	public void setError(Failure error) {
		this.error = error;
	}
}
