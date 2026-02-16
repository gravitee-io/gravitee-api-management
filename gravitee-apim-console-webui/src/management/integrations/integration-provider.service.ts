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
import { Injectable } from '@angular/core';

import { IntegrationProvider } from './integrations.model';

@Injectable({
  providedIn: 'root',
})
export class IntegrationProviderService {
  private readonly integrationProviders: {
    active: IntegrationProvider[];
    comingSoon: IntegrationProvider[];
  } = {
    active: [
      { icon: 'a2a', value: 'A2A', apimDocsName: 'aws-api-gateway' },
      { icon: 'aws-api-gateway', value: 'aws-api-gateway', apimDocsName: 'aws-api-gateway' },
      { icon: 'solace', value: 'solace', apimDocsName: 'solace' },
      { icon: 'apigee', value: 'apigee', apimDocsName: 'apigee-x' },
      { icon: 'azure', value: 'azure-api-management', apimDocsName: 'azure-api-management' },
      { icon: 'ibm-api-connect', value: 'ibm-api-connect', apimDocsName: 'ibm-api-connect' },
      { icon: 'confluent', value: 'confluent-platform', apimDocsName: 'confluent-platform' },
      { icon: 'mulesoft', value: 'mulesoft', apimDocsName: 'mulesoft-anypoint' },
      { icon: 'edge-stack', value: 'edge-stack', apimDocsName: 'edge-stack' },
    ],
    comingSoon: [],
  };

  getApimDocsNameByValue(value: string): string | undefined {
    return this.integrationProviders.active.find(p => p.value === value)?.apimDocsName;
  }

  getActiveProviders(): IntegrationProvider[] {
    return this.integrationProviders.active;
  }

  getComingSoonProviders(): IntegrationProvider[] {
    return this.integrationProviders.comingSoon;
  }
}
