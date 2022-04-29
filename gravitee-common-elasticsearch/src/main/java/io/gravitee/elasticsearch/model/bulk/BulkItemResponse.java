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
import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;

/**
 * 
 * Elasticsearch response for a bulk query
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkItemResponse implements Serializable {
	
	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Response on an index, update or delete operation.
	 */
	private Index index;
	
	/**
	 * Response on an index operation.
	 * @param index es response
	 */
	@JsonSetter("index")
	public void setIndex(final Index index) {
		this.index = index;
	}
	
	/**
	 * Response on an update operation.
	 * @param index es response
	 */
	@JsonSetter("update")
	public void setUpdate(final Index index) {
		this.index = index;
	}
	
	/**
	 * Response on an delete operation.
	 * @param index es response
	 */
	@JsonSetter("delete")
	public void setDelete(final Index index) {
		this.index = index;
	}

	public Index getIndex() {
		return index;
	}
}
