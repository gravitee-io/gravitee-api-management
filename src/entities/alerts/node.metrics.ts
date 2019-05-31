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


import {CompareCondition, Metrics, StringCondition, ThresholdCondition, ThresholdRangeCondition, Tuple} from "../alert";
import ApiService from "../../services/api.service";

export class NodeType {

  application: string;
  name: string;

  static API_GATEWAY: NodeType = new NodeType('gio-apim-gateway', 'API Gateway');
  static MANAGEMENT_API: NodeType = new NodeType('gio-apim-management', 'Management API');

  static TYPES: NodeType[] = [
    NodeType.API_GATEWAY,
    NodeType.MANAGEMENT_API
  ];

  constructor(application: string, name: string) {
    this.application = application;
    this.name = name;
  }
}

export class NodeMetrics extends Metrics {
  static NODE_HOSTNAME: NodeMetrics = new NodeMetrics('node.hostname', 'Hostname',
    [StringCondition.TYPE]);

  static NODE_APPLICATION: NodeMetrics = new NodeMetrics('node.application', 'Type',
    [StringCondition.TYPE],undefined, (type: number, id: string, $injector: any) => {
      let applications: Tuple[] = [];

      NodeType.TYPES.forEach(app => {
        applications.push(new Tuple(app.application, app.name));
      });

      return applications;
    });

  static OS_CPU_PERCENT: NodeMetrics = new NodeMetrics('os.cpu.percent', 'OS CPU (%)',
    [ThresholdCondition.TYPE, ThresholdRangeCondition.TYPE, CompareCondition.TYPE]);

  static PROCESS_CPU_PERCENT: NodeMetrics = new NodeMetrics('process.cpu.percent', 'Process CPU (%)',
    [ThresholdCondition.TYPE, ThresholdRangeCondition.TYPE, CompareCondition.TYPE]);

  static PROCESS_CPU_TOTAL: NodeMetrics = new NodeMetrics('process.cpu.total', 'Process CPU (total)',
    [ThresholdCondition.TYPE, ThresholdRangeCondition.TYPE, CompareCondition.TYPE]);

  static JVM_MEM_HEAP_USED: NodeMetrics = new NodeMetrics('jvm.mem.heap.used', 'JVM Heap used',
    [ThresholdCondition.TYPE, ThresholdRangeCondition.TYPE, CompareCondition.TYPE]);

  static JVM_MEM_HEAP_MAX: NodeMetrics = new NodeMetrics('jvm.mem.heap.max', 'JVM Heap max',
    [ThresholdCondition.TYPE, ThresholdRangeCondition.TYPE, CompareCondition.TYPE]);

  static JVM_MEM_HEAP_PERCENT: NodeMetrics = new NodeMetrics('jvm.mem.heap.percent', 'JVM Heap (%))',
    [ThresholdCondition.TYPE, ThresholdRangeCondition.TYPE, CompareCondition.TYPE]);

  static METRICS: NodeMetrics[] = [
    NodeMetrics.NODE_HOSTNAME,
    NodeMetrics.NODE_APPLICATION,
    NodeMetrics.OS_CPU_PERCENT,
    NodeMetrics.PROCESS_CPU_PERCENT,
    NodeMetrics.PROCESS_CPU_TOTAL,
    NodeMetrics.JVM_MEM_HEAP_USED,
    NodeMetrics.JVM_MEM_HEAP_MAX,
    NodeMetrics.JVM_MEM_HEAP_PERCENT
  ];
}

export class NodeLifecycleMetrics extends Metrics {
  static NODE_HOSTNAME: NodeLifecycleMetrics = new NodeLifecycleMetrics('node.hostname', 'Hostname',
    [StringCondition.TYPE]);

  static NODE_APPLICATION: NodeLifecycleMetrics = new NodeLifecycleMetrics('node.application', 'Type',
    [StringCondition.TYPE],undefined, (type: number, id: string, $injector: any) => {
      let applications: Tuple[] = [];

      NodeType.TYPES.forEach(app => {
        applications.push(new Tuple(app.application, app.name));
      });

      return applications;
    });

  static NODE_EVENT: NodeLifecycleMetrics = new NodeLifecycleMetrics('node.event', 'Event',
    [StringCondition.TYPE],undefined, (type: number, id: string, $injector: any) => {
      let events: Tuple[] = [];
      events.push(new Tuple("NODE_START", "Start"));
      events.push(new Tuple("NODE_STOP", "Stop"));
      return events;
    });

  static METRICS: NodeLifecycleMetrics[] = [
    NodeLifecycleMetrics.NODE_HOSTNAME,
    NodeLifecycleMetrics.NODE_APPLICATION,
    NodeLifecycleMetrics.NODE_EVENT
  ];
}
