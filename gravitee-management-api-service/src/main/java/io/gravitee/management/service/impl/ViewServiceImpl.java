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
package io.gravitee.management.service.impl;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.management.model.NewViewEntity;
import io.gravitee.management.model.UpdateViewEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ViewService;
import io.gravitee.management.service.exceptions.DuplicateViewNameException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ViewServiceImpl extends TransactionalService implements ViewService {

    private final Logger LOGGER = LoggerFactory.getLogger(ViewServiceImpl.class);

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private ApiService apiService;

    @Override
    public List<ViewEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            return viewRepository.findAll()
                    .stream()
                    .map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all views", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all views", ex);
        }
    }

    @Override
    public List<ViewEntity> create(final List<NewViewEntity> viewEntities) {
        // First we prevent the duplicate view name
        final List<String> viewNames = viewEntities.stream()
                .map(NewViewEntity::getName)
                .collect(Collectors.toList());

        final Optional<ViewEntity> optionalView = findAll().stream()
                .filter(view -> viewNames.contains(view.getName()))
                .findAny();

        if (optionalView.isPresent()) {
            throw new DuplicateViewNameException(optionalView.get().getName());
        }

        final List<ViewEntity> savedViews = new ArrayList<>(viewEntities.size());
        viewEntities.forEach(viewEntity -> {
            try {
                savedViews.add(convert(viewRepository.create(convert(viewEntity))));
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to create view {}", viewEntity.getName(), ex);
                throw new TechnicalManagementException("An error occurs while trying to create view " + viewEntity.getName(), ex);
            }
        });
        return savedViews;
    }

    @Override
    public List<ViewEntity> update(final List<UpdateViewEntity> viewEntities) {
        final List<ViewEntity> savedViews = new ArrayList<>(viewEntities.size());
        viewEntities.forEach(viewEntity -> {
            try {
                savedViews.add(convert(viewRepository.update(convert(viewEntity))));
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to update view {}", viewEntity.getName(), ex);
                throw new TechnicalManagementException("An error occurs while trying to update view " + viewEntity.getName(), ex);
            }
        });
        return savedViews;
    }

    @Override
    public void delete(final String viewId) {
        try {
            viewRepository.delete(viewId);

            // delete all reference on APIs
            apiService.deleteViewFromAPIs(viewId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete view {}", viewId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete view " + viewId, ex);
        }
    }

    private View convert(final NewViewEntity viewEntity) {
        final View view = new View();
        view.setId(IdGenerator.generate(viewEntity.getName()));
        view.setName(viewEntity.getName());
        view.setDescription(viewEntity.getDescription());
        return view;
    }

    private View convert(final UpdateViewEntity viewEntity) {
        final View view = new View();
        view.setId(viewEntity.getId());
        view.setName(viewEntity.getName());
        view.setDescription(viewEntity.getDescription());
        return view;
    }

    private ViewEntity convert(final View view) {
        final ViewEntity viewEntity = new ViewEntity();
        viewEntity.setId(view.getId());
        viewEntity.setName(view.getName());
        viewEntity.setDescription(view.getDescription());
        return viewEntity;
    }
}
