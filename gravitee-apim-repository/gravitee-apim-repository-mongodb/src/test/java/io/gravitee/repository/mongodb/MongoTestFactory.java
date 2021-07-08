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
package io.gravitee.repository.mongodb;

import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.tests.MongodForTestsFactory;
import java.io.IOException;

public class MongoTestFactory extends MongodForTestsFactory {

    public static MongoTestFactory with(final IFeatureAwareVersion version) throws IOException {
        return new MongoTestFactory(version);
    }

    public MongoTestFactory() throws IOException {
        this(Version.Main.PRODUCTION);
    }

    public MongoTestFactory(IFeatureAwareVersion version) throws IOException {
        super(version);
    }

    @Override
    protected IMongodConfig newMongodConfig(IFeatureAwareVersion version) throws IOException {
        return new MongodConfigBuilder()
            .version(version)
            .cmdOptions(new MongoCmdOptionsBuilder().useStorageEngine("ephemeralForTest").build())
            .build();
    }
}
