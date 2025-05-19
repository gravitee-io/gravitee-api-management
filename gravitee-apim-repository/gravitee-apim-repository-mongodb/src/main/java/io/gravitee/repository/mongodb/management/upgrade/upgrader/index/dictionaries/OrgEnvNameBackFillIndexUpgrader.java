/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.dictionaries;

import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("DictionaryEnvironmentIdNameAndOrdIndexUpgrader")
public class OrgEnvNameBackFillIndexUpgrader extends MongoUpgrader {

    private static final Logger LOG = LoggerFactory.getLogger(
        OrgEnvNameBackFillIndexUpgrader.class
    );

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        try {
            Set<Dictionary> dictionaries = dictionaryRepository.findAll();
            int migratedCount = 0;

            for (Dictionary dict : dictionaries) {
                String oldId = dict.getId();

                if (oldId != null && oldId.split(":").length == 1) {
                    String envId = dict.getEnvironmentId();
                    Optional<Environment> env = environmentRepository.findById(envId);
                    String envName = env.map(Environment::getName).orElse("Default");
                    String orgId = env
                        .map(Environment::getOrganizationId)
                        .orElse("DEFAULT");

                    String newId = String.join(":", oldId, envName, orgId);

                    if (dictionaryRepository.findById(newId).isEmpty()) {
                        dict.setId(newId);
                        dictionaryRepository.create(dict);
                        dictionaryRepository.delete(oldId);
                        migratedCount++;
                    }
                }
            }

            LOG.info(
                "Migrated %d dictionary IDs to composite format%n {}",
                migratedCount
            );
            return true;
        } catch (Exception e) {
            LOG.error("An error occurred while backfilling EmailUniqueIndex", e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
