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
import java.util.List;

/**
 * A single search hit.
 *
 * @see SearchHits
 * @see https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/search/SearchHit.java
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHit implements Serializable {

	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The index of the hit.
	 */
	@JsonProperty("_index")
	private String index;

	/**
	 * The type of the document.
	 */
	@JsonProperty("_type")
	private String type;

	/**
	 * The id of the document.
	 */
	@JsonProperty("_id")
	private String id;

	/**
	 * The version of the hit.
	 */
	@JsonProperty("_version")
	private Long version;

	/**
	 * The score.
	 */
	@JsonProperty("_score")
	private Float score;

	/**
	 * The source of the document (can be <tt>null</tt>).
	 */
	@JsonProperty("_source")
	private JsonNode source;

	/**
	 * A list of the sort values used.
	 */
	private List<Sort> sort;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public JsonNode getSource() {
        return source;
    }

    public void setSource(JsonNode source) {
        this.source = source;
    }

    public List<Sort> getSort() {
        return sort;
    }

    public void setSort(List<Sort> sort) {
        this.sort = sort;
    }
}
