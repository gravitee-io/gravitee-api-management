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


import {Metrics, Scope} from "../alert";
import {ApiMetrics} from "./api.metrics";
import {NodeLifecycleMetrics, NodeMetrics} from "./node.metrics";

export class Rule {
  public source: string;
  public type: string;
  public description: string;
  public category: string;
  public scopes: Scope[];
  public metrics: Metrics[];

  static API_METRICS_THRESHOLD: Rule = new Rule(
    'REQUEST',
    'METRICS_SIMPLE_CONDITION',
    'Alert when a metric of the request validates a condition',
    [Scope.API],
    'API metrics',
    ApiMetrics.METRICS);

  static API_METRICS_AGGREGATION: Rule = new Rule(
    'REQUEST',
    'METRICS_AGGREGATION',
    'Alert when the aggregated value of a request metric rises a threshold',
    [Scope.API],
    'API metrics',
    [ApiMetrics.RESPONSE_TIME, ApiMetrics.UPSTREAM_RESPONSE_TIME, ApiMetrics.REQUEST_CONTENT_LENGTH, ApiMetrics.RESPONSE_CONTENT_LENGTH]);

  static API_METRICS_RATE: Rule = new Rule(
    'REQUEST',
    'METRICS_RATE',
    'Alert when the rate of a given condition rises a threshold',
    [Scope.API],
    'API metrics',
    ApiMetrics.METRICS);

  static API_HC_ENDPOINT_STATUS_CHANGED: Rule = new Rule(
    'ENDPOINT_HEALTH_CHECK',
    'API_HC_ENDPOINT_STATUS_CHANGED',
    'Alert when the health status of an endpoint has changed',
    [Scope.API],
    'Health-check');

  static APPLICATION_QUOTA: Rule = new Rule(
    'REQUEST',
    'APPLICATION_QUOTA',
    'Alert when the quota rises a threshold',
    [Scope.APPLICATION],
    'Application');

  static NODE_LIFECYCLE_CHANGED: Rule = new Rule(
    'NODE_LIFECYCLE',
    'NODE_LIFECYCLE_CHANGED',
    'Alert when the lifecycle status of a node has changed',
    [Scope.PLATFORM],
    'Node');

  static NODE_METRICS_THRESHOLD: Rule = new Rule(
    'NODE_HEARTBEAT',
    'METRICS_SIMPLE_CONDITION',
    'Alert when a metric of the node validates a condition',
    [Scope.PLATFORM],
    'Node',
    NodeMetrics.METRICS);

  static NODE_METRICS_AGGREGATION: Rule = new Rule(
    'NODE_HEARTBEAT',
    'METRICS_AGGREGATION',
    'Alert when the aggregated value of a node metric rises a threshold',
    [Scope.PLATFORM],
    'Node',
    NodeMetrics.METRICS);

  static NODE_METRICS_RATE: Rule = new Rule(
    'NODE_HEARTBEAT',
    'METRICS_RATE',
    'Alert when the rate of a given condition rises a threshold',
    [Scope.PLATFORM],
    'Node',
    NodeMetrics.METRICS);

  static RULES: Rule[] = [
    Rule.API_METRICS_THRESHOLD,
    Rule.API_METRICS_AGGREGATION,
    Rule.API_METRICS_RATE,
    Rule.API_HC_ENDPOINT_STATUS_CHANGED,
    Rule.APPLICATION_QUOTA,
    Rule.NODE_LIFECYCLE_CHANGED,
    Rule.NODE_METRICS_THRESHOLD,
    Rule.NODE_METRICS_AGGREGATION,
    Rule.NODE_METRICS_RATE
  ];

  constructor(source: string, type: string, description: string, scopes: Scope[], category: string, metrics?: Metrics[]) {
    this.source = source;
    this.type = type;
    this.description = description;
    this.scopes = scopes;
    this.category = category;
    this.metrics = metrics;
  }

  static findByScope(scope: Scope): Rule[] {
    return Rule.RULES.filter(rule => rule.scopes.indexOf(scope) > -1);
  }

  static findByScopeAndType(scope: Scope, type: string): Rule {
    return Rule.RULES.find(rule => rule.type === type && rule.scopes.indexOf(scope) != -1);
  }
}
