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
import { Step } from './plan';

export interface Services {
  discovery: EndpointDiscoveryService;
  'health-check': HealthCheckService;
  'dynamic-property': DynamicPropertyService;
}

export interface EndpointDiscoveryService {
  enabled: boolean;
  provider: string;
  configuration: string;
}

export interface Endpoint {
  name: string;
  target: string;
  weight: number;
  backup: boolean;
  tenants: string[];
  type: string;
  inherit: boolean;
  healthcheck: EndpointHealthCheckService;
}

export interface EndpointHealthCheckService {
  enabled: boolean;
  schedule: string;
  steps: Step[];
  inherit: boolean;
}

export interface HealthCheckService {
  enabled: boolean;
  schedule: string;
  steps: Step[];
}
export interface DynamicPropertyService {
  enabled: boolean;
  provider: string;
  configuration: {};
  schedule: string;
}
