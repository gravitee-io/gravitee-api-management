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
package io.gravitee.rest.api.service;

import java.util.List;
import java.util.Set;

import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.NewViewEntity;
import io.gravitee.rest.api.model.UpdateViewEntity;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ViewService {
    List<ViewEntity> findAll();
    ViewEntity findById(String id);
    ViewEntity findNotHiddenById(String id);
    ViewEntity create(NewViewEntity view);
    ViewEntity update(String viewId, UpdateViewEntity view);
    List<ViewEntity> update(List<UpdateViewEntity> views);
    void delete(String viewId);
    void initialize(String environmentId);
    long getTotalApisByView(Set<ApiEntity> apis, ViewEntity view);
    InlinePictureEntity getPicture(String viewId);
}
