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
package io.gravitee.management.service.impl.upgrade;

import io.gravitee.common.utils.IdGenerator;
import io.gravitee.management.model.UpdateViewEntity;
import io.gravitee.management.model.ViewEntity;
import io.gravitee.management.model.api.UpdateApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.Upgrader;
import io.gravitee.management.service.ViewService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultViewUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultViewUpgrader.class);

    @Autowired
    private ViewService viewService;
    @Autowired
    private ViewRepository viewRepository;
    @Autowired
    private ApiRepository apiRepository;

    @Override
    public boolean upgrade() {
        // Initialize default view
        final Set<View> views;
        try {
            views = viewRepository.findAll();
            Optional<View> optionalAllView = views.
                    stream().
                    filter(v -> v.getId().equals(View.ALL_ID)).
                    findFirst();
            if (optionalAllView.isPresent()) {
                final String key = optionalAllView.get().getKey();
                if (key == null || key.isEmpty()) {
                    logger.info("Update views to add field key");
                    for (final View view : views) {
                        view.setKey(IdGenerator.generate(view.getName()));
                        viewRepository.update(view);
                    }

                    for (final Api api : apiRepository.search(null)) {
                        final Set<String> apiViews = api.getViews();
                        if (apiViews != null) {
                            final Set<String> newApiViews = new HashSet<>(apiViews.size());
                            for (final String apiView : apiViews) {
                                final Optional<View> optionalView = views.stream().filter(v -> apiView.equals(v.getId())).findAny();
                                optionalView.ifPresent(view -> newApiViews.add(view.getId()));
                            }
                            api.setViews(newApiViews);
                        }
                        apiRepository.update(api);
                    }
                }
            } else {
                logger.info("Create default View");
                viewService.createDefaultView();
            }
        } catch (TechnicalException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
