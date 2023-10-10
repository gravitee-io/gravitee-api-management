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
package io.gravitee.gateway.reactor.handler;

import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@ToString
public class DefaultTcpAcceptor implements TcpAcceptor {

    private final ReactorHandler reactor;
    private final String host;

    @Getter(AccessLevel.NONE)
    @Accessors(fluent = true)
    private final List<String> serverIds;

    @Override
    public String host() {
        return host;
    }

    @Override
    public ReactorHandler reactor() {
        return reactor;
    }

    @Override
    public boolean accept(String sni, String serverId) {
        Objects.requireNonNull(sni);
        Objects.requireNonNull(serverId);
        return sni.equals(this.host) && (serverIds == null || serverIds.isEmpty() || serverIds.contains(serverId));
    }

    @Override
    public int compareTo(TcpAcceptor o) {
        return this.host().toLowerCase().compareTo(o.host().toLowerCase());
    }
}
