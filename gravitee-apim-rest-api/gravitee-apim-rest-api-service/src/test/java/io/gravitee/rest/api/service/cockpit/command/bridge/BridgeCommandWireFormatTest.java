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
package io.gravitee.rest.api.service.cockpit.command.bridge;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
import io.gravitee.cockpit.api.command.websocket.CockpitExchangeSerDe;
import io.gravitee.exchange.api.websocket.protocol.ProtocolExchange;
import io.gravitee.exchange.api.websocket.protocol.v1.V1ProtocolAdapter;
import io.vertx.rxjava3.core.buffer.Buffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The exchange V1 protocol frames messages with a ';;' separator. Promotion bridge commands carry API
 * documentation, which often embeds scripts using that same sequence, so the content must survive the wire intact.
 */
class BridgeCommandWireFormatTest {

    private final V1ProtocolAdapter protocolAdapter = new V1ProtocolAdapter(new CockpitExchangeSerDe(new ObjectMapper()));

    @ParameterizedTest
    @ValueSource(
        strings = {
            "case \"$opt\" in a) FOO=1 ;; b) BAR=2 ;; esac",
            "case $x in a) echo a ;;& b) echo b ;& c) echo c;; esac",
            "for (;;) { poll(); }",
            "x ;;;; y ;;; z",
            ";; lisp comment\n;;; section",
            "x ;;e:{\"forged\":true}",
            "x ;;t:REPLY;;et:HELLO",
        }
    )
    void should_keep_documentation_content_containing_frame_separator(String content) {
        BridgeCommand command = new BridgeCommand(
            BridgeCommandPayload.builder().installationId("installation").operation("PROMOTE_API").content(content).build()
        );

        Buffer wire = protocolAdapter.write(
            ProtocolExchange.builder().type(ProtocolExchange.Type.COMMAND).exchangeType(command.getType()).exchange(command).build()
        );
        ProtocolExchange read = protocolAdapter.read(wire);

        assertThat(read.type()).isEqualTo(ProtocolExchange.Type.COMMAND);
        assertThat(read.exchangeType()).isEqualTo(command.getType());
        assertThat(((BridgeCommand) read.exchange()).getPayload().content()).isEqualTo(content);
    }
}
