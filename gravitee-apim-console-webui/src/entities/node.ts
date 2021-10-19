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

export class NodeType {
  static API_GATEWAY: NodeType = new NodeType('gio-apim-gateway', 'API Gateway');
  static MANAGEMENT_API: NodeType = new NodeType('gio-apim-management', 'Management API');

  static TYPES: NodeType[] = [NodeType.API_GATEWAY, NodeType.MANAGEMENT_API];

  application: string;
  name: string;

  constructor(application: string, name: string) {
    this.application = application;
    this.name = name;
  }
}

export class NodeMetrics {
  static CPU: NodeMetrics = new NodeMetrics('process.cpu.percent', 'CPU %');
  static MEM_HEAP: NodeMetrics = new NodeMetrics('jvm.mem.heap.percent', 'Heap size %');
  static THREADS_ACTIVE: NodeMetrics = new NodeMetrics('jvm.threads.count', 'Active threads');
  static FILE_DESCRIPTORS_OPEN: NodeMetrics = new NodeMetrics('process.fd.open', 'Open file descriptors');

  static METRICS: NodeMetrics[] = [NodeMetrics.CPU, NodeMetrics.MEM_HEAP, NodeMetrics.THREADS_ACTIVE, NodeMetrics.FILE_DESCRIPTORS_OPEN];
  key: string;
  name: string;

  constructor(key: string, name: string) {
    this.key = key;
    this.name = name;
  }
}
