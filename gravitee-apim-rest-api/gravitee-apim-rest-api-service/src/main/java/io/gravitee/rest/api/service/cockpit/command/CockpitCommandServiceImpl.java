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
package io.gravitee.rest.api.service.cockpit.command;

import io.gravitee.cockpit.api.CockpitConnector;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeReply;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CockpitCommandServiceImpl implements CockpitCommandService {

    private final CockpitConnector cockpitConnector;

    public CockpitCommandServiceImpl(
        // Need to make this injection lazy to be sure cockpit-connector plugin has been loaded before looking for this bean
        @Lazy CockpitConnector cockpitConnector
    ) {
        this.cockpitConnector = cockpitConnector;
    }

    @Override
    public BridgeReply send(BridgeCommand command) {
        return cockpitConnector
            .sendCommand(command)
            .onErrorResumeNext(error -> {
                log.debug("Failed on call of cockpit", error);
                return Single.just(new BridgeReply(command.getId(), error.getMessage() != null ? error.getMessage() : error.toString()));
            })
            .flatMap(reply ->
                reply instanceof BridgeReply bridgeReply
                    ? Single.just(bridgeReply)
                    : Single.just(new BridgeReply(command.getId(), "Unknown reply type: " + reply.getClass().getName()))
            )
            .blockingGet();
    }
}
