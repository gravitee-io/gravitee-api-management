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
import { ApiType } from './ApiType';
import { EndpointGroup } from './EndpointGroup';
import { Flow } from './Flow';
import { FlowMode } from './FlowMode';
import { Listener } from './Listener';

export interface NewApiEntity {
  name?: string;
  apiVersion?: string;
  definitionVersion?: '4.0.0';
  type?: ApiType;
  description?: string;
  tags?: string[];
  listeners?: Listener[];
  endpointGroups?: EndpointGroup[];
  flowMode?: FlowMode;
  flows?: Flow[];
  groups?: string[];
}
