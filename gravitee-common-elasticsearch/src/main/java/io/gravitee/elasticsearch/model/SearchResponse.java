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

import java.io.Serializable;
import java.util.Map;

/**
 * A response of a search request.
 * 
 * @see https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/action/search/SearchResponse.java
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse implements Response, Serializable {

	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * How long the search took in milliseconds.
	 */
	private Long took;

	/**
	 * Has the search operation timed out.
	 */
	@JsonProperty("timed_out")
	private Boolean timedOut;

	/**
	 * The search hits.
	 */
	@JsonProperty("hits")
	private SearchHits searchHits;

	/**
	 * Number of documents that match the query.
	 */
	private Long count;

	/**
	 * The aggregations of the query.
	 */
	private Map<String, Aggregation> aggregations;

	public Long getTook() {
		return took;
	}

	public void setTook(Long took) {
		this.took = took;
	}

	public Boolean getTimedOut() {
		return timedOut;
	}

	public void setTimedOut(Boolean timedOut) {
		this.timedOut = timedOut;
	}

	public SearchHits getSearchHits() {
		return searchHits;
	}

	public void setSearchHits(SearchHits searchHits) {
		this.searchHits = searchHits;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public Map<String, Aggregation> getAggregations() {
		return aggregations;
	}

	public void setAggregations(Map<String, Aggregation> aggregations) {
		this.aggregations = aggregations;
	}
}
