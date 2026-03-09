/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { isFunction } from 'rxjs/internal/util/isFunction';

import { Connector, ConnectorsResponse } from './connector';

export function fakeConnector(modifier?: Partial<Connector> | ((baseConnector: Connector) => Connector)) {
  const base: Connector = {
    id: 'mock',
    name: 'Mock',
  };

  return isFunction(modifier) ? modifier(base) : { ...base, ...modifier };
}

export function fakeConnectorsResponse(
  modifier?: Partial<ConnectorsResponse> | ((baseConnectorsResponse: ConnectorsResponse) => ConnectorsResponse),
) {
  const base: ConnectorsResponse = {
    data: [fakeConnector()],
  };

  return isFunction(modifier) ? modifier(base) : { ...base, ...modifier };
}
