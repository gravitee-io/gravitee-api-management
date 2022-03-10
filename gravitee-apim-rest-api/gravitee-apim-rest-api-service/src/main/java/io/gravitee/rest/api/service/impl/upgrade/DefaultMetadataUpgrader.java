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

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewMetadataEntity;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultMetadataUpgrader implements Upgrader {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultMetadataUpgrader.class);

    public static final String METADATA_EMAIL_SUPPORT_KEY = "email-support";
    public static final String DEFAULT_METADATA_EMAIL_SUPPORT = "support@change.me";

    @Autowired
    private MetadataService metadataService;

    @Override
    public Completable upgrade() {
        return Completable.fromRunnable(
            new Runnable() {
                @Override
                public void run() {
                    // FIXME : this upgrader uses the default ExecutionContext, but should handle all environments/organizations
                    ExecutionContext executionContext = GraviteeContext.getExecutionContext();

                    // initialize default metadata
                    final MetadataEntity defaultEmailSupportMetadata = metadataService.findDefaultByKey(METADATA_EMAIL_SUPPORT_KEY);

                    if (defaultEmailSupportMetadata == null) {
                        logger.info("    No default metadata for email support found. Add default one.");
                        final NewMetadataEntity metadata = new NewMetadataEntity();
                        metadata.setFormat(MetadataFormat.MAIL);
                        metadata.setName("Email support");
                        metadata.setValue(DEFAULT_METADATA_EMAIL_SUPPORT);
                        final MetadataEntity metadataEntity = metadataService.create(executionContext, metadata);
                        logger.info("    Added default metadata for email support with success: {}", metadataEntity);
                    }
                }
            }
        );
    }

    @Override
    public int order() {
        return 100;
    }
}
