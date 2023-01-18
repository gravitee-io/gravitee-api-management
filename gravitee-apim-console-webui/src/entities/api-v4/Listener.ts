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
import { Entrypoint } from './Entrypoint';

import { Cors } from '../cors';
import { Logging } from '../logging';

export type Listener = HttpListener | SubscriptionListener | TcpListener;

export interface AbstractListener {
  type?: ListenerType;
  entrypoints?: Entrypoint[];
}

export interface HttpListener extends AbstractListener {
  type: 'http';
  entrypoints?: Entrypoint[];
  paths?: HttpListenerPath[];
  pathMappings?: string[];
  pathMappingsPattern?: Record<string, RegExp | string>;
  cors?: Cors;
  logging?: Logging;
}

export interface SubscriptionListener extends AbstractListener {
  type: 'subscription';
}

export interface TcpListener extends AbstractListener {
  type: 'tcp';
}

export type ListenerType = 'http' | 'subscription' | 'tcp';

export interface HttpListenerPath {
  host?: string;
  path?: string;
  overrideAccess?: boolean;
}
