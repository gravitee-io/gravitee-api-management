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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.VIEW;
import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_CREATED;
import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_DELETED;
import static io.gravitee.repository.management.model.View.AuditEvent.VIEW_UPDATED;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.NewViewEntity;
import io.gravitee.rest.api.model.UpdateViewEntity;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.ViewService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DuplicateViewNameException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.ViewNotFoundException;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ViewServiceImpl extends TransactionalService implements ViewService {

    private final Logger LOGGER = LoggerFactory.getLogger(ViewServiceImpl.class);

    @Value("${configuration.default-icon:${gravitee.home}/assets/default_api_logo.png}")
    private String defaultIcon;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private ApiService apiService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EnvironmentService environmentService;

    @Override
    public List<ViewEntity> findAll() {
        try {
            LOGGER.debug("Find all views");
            return viewRepository.findAllByEnvironment(GraviteeContext.getCurrentEnvironment())
                    .stream()
                    .map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all views", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all views", ex);
        }
    }

    @Override
    public ViewEntity findById(String id) {
        try {
            LOGGER.debug("Find view by id : {}", id);
            Optional<View> view = viewRepository.findById(id, GraviteeContext.getCurrentEnvironment());

            if (view.isPresent()) {
                return convert(view.get());
            }

            throw new ViewNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a view using its ID: {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find a view using its ID: " + id, ex);
        }
    }

    @Override
    public ViewEntity findNotHiddenById(String id) {
        try {
            LOGGER.debug("Find not hidden view by id : {}", id);
            Optional<View> view = viewRepository.findById(id, GraviteeContext.getCurrentEnvironment());

            if (view.isPresent() && !view.get().isHidden()) {
                return convert(view.get());
            }

            throw new ViewNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a view using its ID: {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find a view using its ID: " + id, ex);
        }
    }
    
    @Override
    public ViewEntity create(NewViewEntity viewEntity) {
        // First we prevent the duplicate view name
        final Optional<ViewEntity> optionalView = findAll().stream()
                .filter(v -> v.getName().equals((viewEntity.getName())))
                .findAny();

        if (optionalView.isPresent()) {
            throw new DuplicateViewNameException(optionalView.get().getName());
        }

        try {
            // check if environment exists
            String environment = GraviteeContext.getCurrentEnvironment();
            this.environmentService.findById(environment);
            
            View view = convert(viewEntity);
            view.setEnvironment(environment);
            ViewEntity createdView = convert(viewRepository.create(view));
            auditService.createPortalAuditLog(
                    Collections.singletonMap(VIEW, view.getId()),
                    VIEW_CREATED,
                    new Date(),
                    null,
                    view);

            return createdView;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create view {}", viewEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to create view " + viewEntity.getName(), ex);
        }
    }

    @Override
    public ViewEntity update(String viewId, UpdateViewEntity viewEntity) {
        try {
            LOGGER.debug("Update View {}", viewId);

            Optional<View> optViewToUpdate = viewRepository.findById(viewId, GraviteeContext.getCurrentEnvironment());
            if (!optViewToUpdate.isPresent()) {
                throw new ViewNotFoundException(viewId);
            }

            View view = convert(viewEntity, optViewToUpdate.get().getEnvironment());

            // check if picture has been set
            if (viewEntity.getPicture() == null) {
                view.setPicture(optViewToUpdate.get().getPicture());
            }

            ViewEntity updatedView = convert(viewRepository.update(view));
            auditService.createPortalAuditLog(
                    Collections.singletonMap(VIEW, view.getId()),
                    VIEW_UPDATED,
                    new Date(),
                    optViewToUpdate.get(),
                    view);

            return updatedView;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update view {}", viewEntity.getName(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update view " + viewEntity.getName(), ex);
        }
    }

    @Override
    public List<ViewEntity> update(final List<UpdateViewEntity> viewEntities) {
        final List<ViewEntity> savedViews = new ArrayList<>(viewEntities.size());
        viewEntities.forEach(viewEntity -> {
            try {
                Optional<View> viewOptional = viewRepository.findById(viewEntity.getId(), GraviteeContext.getCurrentEnvironment());
                if (viewOptional.isPresent()) {
                    View view = convert(viewEntity, viewOptional.get().getEnvironment());

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
            Optional<View> viewOptional = viewRepository.findById(viewId, GraviteeContext.getCurrentEnvironment());
            if (viewOptional.isPresent()) {
                viewRepository.delete(viewId, GraviteeContext.getCurrentEnvironment());
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
    public void initialize(String environmentId) {
            View view = new View();
            view.setId(View.ALL_ID);
            view.setEnvironment(environmentId);
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

    @Override
    public InlinePictureEntity getPicture(String viewId) {
        ViewEntity viewEntity = findById(viewId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (viewEntity.getPicture() == null) {
            imageEntity.setType("image/png");
            imageEntity.setContent(getDefaultPicture());
        } else {
            String[] parts = viewEntity.getPicture().split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = viewEntity.getPicture().split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
        }

        return imageEntity;
    }

    private byte[] getDefaultPicture() {
        try {
            return IOUtils.toByteArray(new FileInputStream(defaultIcon));
        } catch (IOException ioe) {
            LOGGER.error("Default icon for View does not exist", ioe);
        }
        return null;
    }

    private View convert(final NewViewEntity viewEntity) {
        final View view = new View();
        view.setId(IdGenerator.generate(viewEntity.getName()));
        view.setName(viewEntity.getName());
        view.setDescription(viewEntity.getDescription());
        view.setOrder(viewEntity.getOrder());
        view.setHidden(viewEntity.isHidden());
        view.setHighlightApi(viewEntity.getHighlightApi());
        view.setPicture(viewEntity.getPicture());
        return view;
    }

    private View convert(final UpdateViewEntity viewEntity, final String environment) {
        final View view = new View();
        view.setId(viewEntity.getId());
        view.setEnvironment(environment);
        view.setName(viewEntity.getName());
        view.setDescription(viewEntity.getDescription());
        view.setDefaultView(viewEntity.isDefaultView());
        view.setOrder(viewEntity.getOrder());
        view.setHidden(viewEntity.isHidden());
        view.setHighlightApi(viewEntity.getHighlightApi());
        view.setPicture(viewEntity.getPicture());
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
        viewEntity.setHighlightApi(view.getHighlightApi());
        viewEntity.setPicture(view.getPicture());
        viewEntity.setUpdatedAt(view.getUpdatedAt());
        viewEntity.setCreatedAt(view.getCreatedAt());
        return viewEntity;
    }
}
