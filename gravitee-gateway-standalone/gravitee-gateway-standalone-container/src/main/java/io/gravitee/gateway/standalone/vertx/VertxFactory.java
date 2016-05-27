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
package io.gravitee.gateway.standalone.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class VertxFactory implements FactoryBean<Vertx> {

    @Autowired
    private GraviteeVerticleFactory graviteeVerticleFactory;

    @Override
    public Vertx getObject() throws Exception {
        VertxOptions options = new VertxOptions();//.setWorkerPoolSize(20);
        Vertx instance = Vertx.vertx(options);
        instance.registerVerticleFactory(graviteeVerticleFactory);
        return instance;
    }

    @Override
    public Class<?> getObjectType() {
        return Vertx.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
