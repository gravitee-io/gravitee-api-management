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
import io.gravitee.management.service.AuditService;
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

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.VIEW;
import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_CREATED;
import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_DELETED;
import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_UPDATED;

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

    @Autowired
    private AuditService auditService;

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
                View view = convert(viewEntity);
                savedViews.add(convert(viewRepository.create(view)));
                auditService.createPortalAuditLog(
                        Collections.singletonMap(VIEW, view.getId()),
                        VIEW_CREATED,
                        new Date(),
                        null,
                        view);
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
                View view = convert(viewEntity);
                Optional<View> viewOptional = viewRepository.findById(view.getId());
                if (viewOptional.isPresent()) {
                    savedViews.add(convert(viewRepository.update(view)));
                    auditService.createPortalAuditLog(
                            Collections.singletonMap(VIEW, view.getId()),
                            VIEW_UPDATED,
                            new Date(),
                            viewOptional.get(),
                            view);
                }
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to update view {}", viewEntity.getName(), ex);
                throw new TechnicalManagementException("An error occurs while trying to update view " + viewEntity.getName(), ex);
            }
        });
        return savedViews;
    }

    @Override
    public void delete(final String viewId) {
        if (View.ALL_ID.equals(viewId)) {
            LOGGER.error("Delete the default view is forbidden");
            throw new TechnicalManagementException("Delete the default view is forbidden");
        }
        try {
            Optional<View> viewOptional = viewRepository.findById(viewId);
            if (viewOptional.isPresent()) {
                viewRepository.delete(viewId);
                auditService.createPortalAuditLog(
                        Collections.singletonMap(VIEW, viewId),
                        VIEW_DELETED,
                        new Date(),
                        null,
                        viewOptional.get());

                // delete all reference on APIs
                apiService.deleteViewFromAPIs(viewId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete view {}", viewId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete view " + viewId, ex);
        }
    }

    @Override
    public void createDefaultView() {
            View view = new View();
            view.setId(View.ALL_ID);
            view.setName("All");
            view.setDefaultView(true);
            view.setOrder(0);
            view.setCreatedAt(new Date());
            view.setUpdatedAt(view.getCreatedAt());
        try{
            viewRepository.create(view);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create view {}", view.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create view " + view.getName(), ex);
        }
    }

    private View convert(final NewViewEntity viewEntity) {
        final View view = new View();
        view.setId(IdGenerator.generate(viewEntity.getName()));
        view.setName(viewEntity.getName());
        view.setDescription(viewEntity.getDescription());
        view.setOrder(viewEntity.getOrder());
        view.setHidden(viewEntity.isHidden());
        return view;
    }

    private View convert(final UpdateViewEntity viewEntity) {
        final View view = new View();
        view.setId(viewEntity.getId());
        view.setName(viewEntity.getName());
        view.setDescription(viewEntity.getDescription());
        view.setDefaultView(viewEntity.isDefaultView());
        view.setOrder(viewEntity.getOrder());
        view.setHidden(viewEntity.isHidden());
        return view;
    }

    private ViewEntity convert(final View view) {
        final ViewEntity viewEntity = new ViewEntity();
        viewEntity.setId(view.getId());
        viewEntity.setName(view.getName());
        viewEntity.setDescription(view.getDescription());
        viewEntity.setDefaultView(view.isDefaultView());
        viewEntity.setOrder(view.getOrder());
        viewEntity.setHidden(view.isHidden());
        viewEntity.setUpdatedAt(view.getUpdatedAt());
        viewEntity.setCreatedAt(view.getCreatedAt());
        return viewEntity;
    }
}
