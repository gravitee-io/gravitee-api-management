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
package io.gravitee.rest.api.model.configuration.application;


import java.util.List;

public class ApplicationTypeEntity {

    private String id;
    private String name;
    private String description;
    private Boolean requires_redirect_uris;
    private List<ApplicationGrantTypeEntity> allowed_grant_types;
    private List<ApplicationGrantTypeEntity> default_grant_types;
    private List<ApplicationGrantTypeEntity> mandatory_grant_types;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getRequires_redirect_uris() {
        return requires_redirect_uris;
    }

    public void setRequires_redirect_uris(Boolean requires_redirect_uris) {
        this.requires_redirect_uris = requires_redirect_uris;
    }

    public List<ApplicationGrantTypeEntity> getAllowed_grant_types() {
        return allowed_grant_types;
    }

    public void setAllowed_grant_types(List<ApplicationGrantTypeEntity> allowed_grant_types) {
        this.allowed_grant_types = allowed_grant_types;
    }

    public List<ApplicationGrantTypeEntity> getDefault_grant_types() {
        return default_grant_types;
    }

    public void setDefault_grant_types(List<ApplicationGrantTypeEntity> default_grant_types) {
        this.default_grant_types = default_grant_types;
    }

    public List<ApplicationGrantTypeEntity> getMandatory_grant_types() {
        return mandatory_grant_types;
    }

    public void setMandatory_grant_types(List<ApplicationGrantTypeEntity> mandatory_grant_types) {
        this.mandatory_grant_types = mandatory_grant_types;
    }
}
