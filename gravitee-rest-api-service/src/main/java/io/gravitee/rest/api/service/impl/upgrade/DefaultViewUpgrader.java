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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.repository.management.model.View;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.ViewService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    @Override
    public boolean upgrade() {
        // Initialize default view
        Optional<ViewEntity> optionalAllView = viewService.findAll().
                stream().
                filter(v -> v.getId().equals(View.ALL_ID)).
                findFirst();
        if(!optionalAllView.isPresent()) {
            logger.info("Create default View");
            viewService.initialize(GraviteeContext.getDefaultEnvironment());
        }

        return true;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
