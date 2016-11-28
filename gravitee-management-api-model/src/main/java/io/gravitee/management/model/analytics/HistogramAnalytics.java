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
package io.gravitee.management.model.analytics;

import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HistogramAnalytics implements Analytics {

    private List<Long> timestamps = new ArrayList<>();
    private List<Bucket> values = new ArrayList<>();
    private Map<String, Map<String, String>> metadata;

    public List<Bucket> getValues() {
        return values;
    }

    public void setValues(List<Bucket> values) {
        this.values = values;
    }

    public List<Long> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(List<Long> timestamps) {
        this.timestamps = timestamps;
    }

    public Map<String, Map<String, String>> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Map<String, String>> metadata) {
        this.metadata = metadata;
    }
}
