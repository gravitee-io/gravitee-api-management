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
package io.gravitee.rest.api.model;

import java.util.Map;

/**
 * A page that can be fetched from an external source.
 *
 * @author GraviteeSource Team
 */
public abstract class FetchablePageEntity {

    private String content;

    private PageSourceEntity source;

    private Map<String, String> metadata;

    private Boolean useAutoFetch; // use Boolean to avoid default value of primitive type

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PageSourceEntity getSource() {
        return source;
    }

    public void setSource(PageSourceEntity source) {
        this.source = source;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Boolean getUseAutoFetch() {
        return useAutoFetch;
    }

    public void setUseAutoFetch(Boolean useAutoFetch) {
        this.useAutoFetch = useAutoFetch;
    }
}
