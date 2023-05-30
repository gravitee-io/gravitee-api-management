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
import { TestBed } from '@angular/core/testing';
import { set } from 'lodash';

import { ConstantsService, PLAN_SECURITY_TYPES, PlanSecurityVM } from './constants.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';

describe('ConstantsService', () => {
  let constantsService: ConstantsService;
  const init = async (securityValue: any) => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
      providers: [
        {
          provide: 'Constants',
          useFactory: () => {
            const constants = CONSTANTS_TESTING;
            set(constants, 'env.settings.plan.security', securityValue);
            return constants;
          },
        },
      ],
    });
    constantsService = TestBed.inject<ConstantsService>(ConstantsService);
  };

  describe('get enabled plan security types', () => {
    it('should return full list when all enabled', async () => {
      await init({
        apikey: {
          enabled: true,
        },
        jwt: {
          enabled: true,
        },
        keyless: {
          enabled: true,
        },
        oauth2: {
          enabled: true,
        },
        customApiKey: {
          enabled: true,
        },
        sharedApiKey: {
          enabled: true,
        },
      });
      const expectedPlanSecurityTypes = PLAN_SECURITY_TYPES;

      expect(constantsService.getEnabledPlanSecurityTypes()).toMatchObject(expectedPlanSecurityTypes);
    });
    it('should return empty list when all disabled', async () => {
      await init({
        apikey: {
          enabled: false,
        },
        jwt: {
          enabled: false,
        },
        keyless: {
          enabled: false,
        },
        oauth2: {
          enabled: false,
        },
        customApiKey: {
          enabled: false,
        },
        sharedApiKey: {
          enabled: false,
        },
      });
      const expectedPlanSecurityTypes: PlanSecurityVM[] = [];

      expect(constantsService.getEnabledPlanSecurityTypes()).toMatchObject(expectedPlanSecurityTypes);
    });

    it('should return empty list if no plan settings', async () => {
      await init({});
      const expectedPlanSecurityTypes: PlanSecurityVM[] = [];

      expect(constantsService.getEnabledPlanSecurityTypes()).toMatchObject(expectedPlanSecurityTypes);
    });
  });
});
