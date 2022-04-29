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
import java.util.List;


/**
 * The hits of a search request.
 * 
 * @see https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/search/SearchHits.java
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHits implements Serializable {

	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The total number of hits that matches the search request.
	 */
	@JsonProperty("total")
	private TotalHits total;

	/**
	 * The maximum score of this query.
	 */
	@JsonProperty("max_score")
	private String maxScore;

	/**
	 * The hits of the search request (based on the search type, and from / size provided).
	 */
	private List<SearchHit> hits;

	public TotalHits getTotal() {
		return total;
	}

	public void setTotal(TotalHits total) {
		this.total = total;
	}

	public String getMaxScore() {
		return maxScore;
	}

	public void setMaxScore(String maxScore) {
		this.maxScore = maxScore;
	}

	public List<SearchHit> getHits() {
		return hits;
	}

	public void setHits(List<SearchHit> hits) {
		this.hits = hits;
	}
}
