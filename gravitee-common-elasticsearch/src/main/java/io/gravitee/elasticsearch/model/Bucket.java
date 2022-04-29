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
package io.gravitee.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bucket implements Serializable {

	/** UID */
	private static final long serialVersionUID = 1L;

	/** Key of the bucket */
	private String key;

	@JsonProperty("key_as_string")
	private String keyAsString;

	/** Number of documents in the bucket */
	@JsonProperty("doc_count")
	private long docCount;
	
	/**
	 * The aggregations of the query.
	 */
	@JsonProperty("by_result")
	private JsonNode terms;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getDocCount() {
		return docCount;
	}

	public void setDocCount(long docCount) {
		this.docCount = docCount;
	}

	public String getKeyAsString() {
		return keyAsString;
	}

	public void setKeyAsString(String keyAsString) {
		this.keyAsString = keyAsString;
	}

	public JsonNode getTerms() {
		return terms;
	}

	public void setTerms(JsonNode terms) {
		this.terms = terms;
	}
}
