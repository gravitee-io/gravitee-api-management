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

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'mapProviderName',
  standalone: true,
})
export class MapProviderNamePipe implements PipeTransform {
  private names: { [key: string]: string } = {
    'aws-api-gateway': 'AWS API Gateway',
    AWS: 'AWS API Gateway',
    A2A: 'A2A Protocol',
    solace: 'Solace',
    apigee: 'Apigee',
    'confluent-platform': 'Confluent Platform',
    'azure-api-management': 'Azure API Management',
    kong: 'Kong',
    'ibm-api-connect': 'IBM API Connect',
    mulesoft: 'Mulesoft',
    'dell-boomi': 'Boomi',
    'edge-stack': 'Edge Stack',
  };

  transform(value: string): string {
    return this.names[value] || value;
  }
}
