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

import java.io.Serializable;
import java.util.List;

/**
 * A response of a bulk execution. Holding a response for each item responding (in order) of the bulk requests. Each item holds the
 * index/type/id is operated on, and if it failed or not (with the failure message).
 * 
 * @see https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/action/bulk/BulkResponse.java
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkResponse implements Serializable {

	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * How long the bulk execution took.
	 */
	private Long took;

	/**
	 * Has anything failed with the execution. True if an error occurs.
	 */
	private Boolean errors;

	/**
	 * The items representing each action performed in the bulk operation (in the same order!).
	 */
	private List<BulkItemResponse> items;

	/**
	 * Optional Failure details.
	 */
	private Failure error;

	public Long getTook() {
		return took;
	}

	public void setTook(Long took) {
		this.took = took;
	}

	public Boolean getErrors() {
		return errors;
	}

	public void setErrors(Boolean errors) {
		this.errors = errors;
	}

	public List<BulkItemResponse> getItems() {
		return items;
	}

	public void setItems(List<BulkItemResponse> items) {
		this.items = items;
	}

	public Failure getError() {
		return error;
	}

	public void setError(Failure error) {
		this.error = error;
	}
}
