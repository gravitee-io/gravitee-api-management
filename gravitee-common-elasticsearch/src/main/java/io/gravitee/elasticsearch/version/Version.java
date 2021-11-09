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
package io.gravitee.elasticsearch.version;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Version {

    private static final String OPENSEARCH_DISTRIBUTION = "opensearch";

    private String number;

    @JsonProperty("lucene_version")
    private String luceneVersion;

    private String distribution;

    private int majorVersion = -1;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getLuceneVersion() {
        return luceneVersion;
    }

    public void setLuceneVersion(String luceneVersion) {
        this.luceneVersion = luceneVersion;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public int getMajorVersion() {
        if (majorVersion == -1) {
            majorVersion = Integer.valueOf(getNumber().substring(0, 1));
        }
        return majorVersion;
    }

    public boolean isOpenSearch() {
        return OPENSEARCH_DISTRIBUTION.equals(distribution);
    }

    public boolean canUseTypeRequests() {
        // from ES version 7, specifying types in requests is deprecated
        return !isOpenSearch() && getMajorVersion() < 7;
    }

    public boolean canUseMultiTypeIndex() {
        // from ES version 6, using multiple mapping types is no more supported
        return !isOpenSearch() && getMajorVersion() < 6;
    }

    public boolean canUseIlmIndex() {
        // from ES version 6, we can use ILM indexes
        return isOpenSearch() || getMajorVersion() >= 6;
    }
}
