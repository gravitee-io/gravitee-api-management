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

import { ApiV2, ApiV4, HttpListener, KafkaListener, TcpListener } from '../../entities/management-api-v2';

export const getApiAccess = (api: ApiV4 | ApiV2): string[] | null => {
  if (api.definitionVersion === 'V2') {
    return api.proxy.virtualHosts?.length > 0 ? api.proxy.virtualHosts.map(vh => `${vh.host ?? ''}${vh.path}`) : [api.contextPath];
  }

  if (api.type === 'NATIVE') {
    const kafkaListenerHosts = api.listeners
      .filter(listener => listener.type === 'KAFKA')
      .map((kafkaListener: KafkaListener) => {
        const host = kafkaListener.host ?? '';
        const port = kafkaListener.port ? `:${kafkaListener.port}` : '';
        return `${host}${port}`;
      });

    return kafkaListenerHosts.length > 0 ? kafkaListenerHosts : null;
  }

  const tcpListenerHosts = api.listeners.filter(listener => listener.type === 'TCP').flatMap((listener: TcpListener) => listener.hosts);

  const httpListenerPaths = api.listeners
    .filter(listener => listener.type === 'HTTP')
    .map((listener: HttpListener) => listener.paths.map(path => `${path.host ?? ''}${path.path}`))
    .flat();

  return tcpListenerHosts.length > 0 ? tcpListenerHosts : httpListenerPaths.length > 0 ? httpListenerPaths : null;
};
