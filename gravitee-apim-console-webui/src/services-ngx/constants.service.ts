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
import { Inject, Injectable } from '@angular/core';
import { camelCase } from 'lodash';

import { Constants } from '../entities/Constants';
import { PlanSecurityType } from '../entities/management-api-v2';

export interface PlanSecurityVM {
  id: PlanSecurityType;
  name: string;
  policy?: string;
}
export const PLAN_SECURITY_TYPES: PlanSecurityVM[] = [
  {
    id: 'OAUTH2',
    name: 'OAuth2',
    policy: 'oauth2',
  },
  {
    id: 'JWT',
    name: 'JWT',
    policy: 'jwt',
  },
  {
    id: 'API_KEY',
    name: 'API Key',
    policy: 'api-key',
  },
  {
    id: 'KEY_LESS',
    name: 'Keyless (public)',
  },
];

@Injectable({
  providedIn: 'root',
})
export class ConstantsService {
  constructor(@Inject('Constants') private readonly constants: Constants) {}

  getEnabledPlanSecurityTypes(): PlanSecurityVM[] {
    const planSecuritySettings: [string, { enabled: boolean }][] = Object.entries(this.constants.env?.settings?.plan?.security ?? {});

    return PLAN_SECURITY_TYPES.filter((securityType) => {
      const cleanSecurityType = camelCase(securityType.id.replace('_', ''));

      // One of the portal settings security types matches the security type from PLAN_SECURITY_TYPES
      const matchedSecurityType = planSecuritySettings
        .filter(([portalSettingsSecurityType, _]) => cleanSecurityType === camelCase(portalSettingsSecurityType))
        .map(([_, value]) => value);

      return matchedSecurityType.length > 0 && matchedSecurityType[0].enabled;
    });
  }
}
