/*
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
import { Definition } from './Definition';

import { Api, ApiPlan, ApiProperty, ApiResource } from '../../../../entities/api';
import { Services } from '../../../../entities/services';

export interface ApiDefinition extends Definition {
  resources: ApiResource[];
  plans?: ApiPlan[];
  properties: ApiProperty[];
  services: Services;
}

export function toApiDefinition(api: Api): ApiDefinition {
  return {
    id: api.id,
    name: api.name,
    flows: api.flows,
    flow_mode: api.flow_mode,
    resources: api.resources,
    plans: api.plans,
    version: api.version,
    properties: api.properties,
    services: api.services,
  };
}
