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

import { ConnectorFeature } from './connectorFeature';
import { ConnectorMode } from './connectorMode';
import { PlatformPlugin } from './platformPlugin';

import { ApiType, ListenerType, Qos } from '../api';

export interface ConnectorPlugin extends PlatformPlugin {
  supportedApiType?: ApiType;
  supportedModes?: ConnectorMode[];
  supportedQos?: Qos[];
  supportedListenerType?: ListenerType;
  availableFeatures?: ConnectorFeature[];
  /**
   * The schema of the plugin.
   */
  schema?: string;
  /**
   * The icon of the plugin.
   */
  icon?: string;
  /**
   * The subscription schema of the plugin.
   */
  subscriptionSchema?: string;
  deployed: boolean;
}
