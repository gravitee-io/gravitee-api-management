package io.gravitee.definition.model.v4.tcp;

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
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class TcpProxyOptions implements Serializable {

    private static final long serialVersionUID = 6710746676968205250L;

    private boolean enabled;

    private boolean useSystemProxy;

    private String host;

    private int port;

    private String username;

    private String password;

    private TcpProxyType type = TcpProxyType.SOCKS5;
}
