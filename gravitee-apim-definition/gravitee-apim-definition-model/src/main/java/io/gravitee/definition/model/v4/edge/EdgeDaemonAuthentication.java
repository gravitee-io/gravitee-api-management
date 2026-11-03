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
package io.gravitee.definition.model.v4.edge;

import java.io.Serializable;

/**
 * The authentication methods the daemon fleet of the Edge API's environment may use. Keyless and mTLS can be
 * enabled together, which is how a fleet moves from one to the other.
 */
public record EdgeDaemonAuthentication(EdgeKeylessAuthentication keyless, EdgeMtlsAuthentication mtls) implements Serializable {}
