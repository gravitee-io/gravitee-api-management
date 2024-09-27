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
import { DefinitionVersion, ListenerType, PlanSecurityType } from '../entities/management-api-v2';

export type PlanFormType = PlanSecurityType | 'PUSH';

export interface PlanMenuItemVM {
  planFormType: PlanFormType;
  name: string;
  policy?: string;
}
export const AVAILABLE_PLANS_FOR_MENU: PlanMenuItemVM[] = [
  {
    planFormType: 'MTLS',
    name: 'mTLS',
    policy: 'mtls',
  },
  {
    planFormType: 'OAUTH2',
    name: 'OAuth2',
    policy: 'oauth2',
  },
  {
    planFormType: 'JWT',
    name: 'JWT',
    policy: 'jwt',
  },
  {
    planFormType: 'API_KEY',
    name: 'API Key',
    policy: 'api-key',
  },
  {
    planFormType: 'KEY_LESS',
    name: 'Keyless (public)',
  },
  {
    planFormType: 'PUSH',
    name: 'Push plan',
  },
];

@Injectable({
  providedIn: 'root',
})
export class ConstantsService {
  constructor(@Inject(Constants) private readonly constants: Constants) {}

  getEnabledPlanMenuItems(): PlanMenuItemVM[] {
    const planSecuritySettings: [string, { enabled: boolean }][] = Object.entries(this.constants.env?.settings?.plan?.security ?? {});

    return AVAILABLE_PLANS_FOR_MENU.filter((planMenuItem) => {
      const cleanSecurityType = camelCase(planMenuItem.planFormType.replace('_', ''));

      // One of the portal settings security types matches the security type from PLAN_SECURITY_TYPES
      const matchedSecurityType = planSecuritySettings
        .filter(([portalSettingsSecurityType, _]) => cleanSecurityType === camelCase(portalSettingsSecurityType))
        .map(([_, value]) => value);

      return matchedSecurityType.length > 0 && matchedSecurityType[0].enabled;
    });
  }

  getPlanMenuItems(definitionVersion: DefinitionVersion, listenerTypes: ListenerType[]): PlanMenuItemVM[] {
    const availablePlanMenuItems = this.getEnabledPlanMenuItems();

    if (definitionVersion === 'V4' && listenerTypes?.every((listenerType) => listenerType === 'TCP')) {
      return availablePlanMenuItems.filter((p) => p.planFormType === 'KEY_LESS');
    }

    if (definitionVersion === 'V4' && listenerTypes?.every((listenerType) => listenerType === 'SUBSCRIPTION')) {
      return availablePlanMenuItems.filter((planMenuItem) => planMenuItem.planFormType === 'PUSH');
    }

    if (definitionVersion === 'V4' && listenerTypes?.every((listenerType) => ['HTTP', 'TCP'].includes(listenerType))) {
      return availablePlanMenuItems.filter((planMenuItem) => planMenuItem.planFormType !== 'PUSH');
    }

    if (definitionVersion !== 'V4') {
      return availablePlanMenuItems
        .filter((planMenuItem) => planMenuItem.planFormType !== 'PUSH')
        .filter((planMenuItem) => planMenuItem.planFormType !== 'MTLS');
    }

    return availablePlanMenuItems;
  }
}
