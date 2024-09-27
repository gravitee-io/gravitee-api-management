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

import { AVAILABLE_PLANS_FOR_MENU, ConstantsService, PlanMenuItemVM } from './constants.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { Constants } from '../entities/Constants';

describe('ConstantsService', () => {
  let constantsService: ConstantsService;
  const init = async (securityValue: any) => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
      providers: [
        {
          provide: Constants,
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
        push: {
          enabled: true,
        },
        mtls: {
          enabled: true,
        },
      });
      const expectedPlanSecurityTypes = AVAILABLE_PLANS_FOR_MENU;

      expect(constantsService.getEnabledPlanMenuItems()).toMatchObject(expectedPlanSecurityTypes);
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
      const expectedPlanSecurityTypes: PlanMenuItemVM[] = [];

      expect(constantsService.getEnabledPlanMenuItems()).toMatchObject(expectedPlanSecurityTypes);
    });

    it('should return empty list if no plan settings', async () => {
      await init({});
      const expectedPlanSecurityTypes: PlanMenuItemVM[] = [];

      expect(constantsService.getEnabledPlanMenuItems()).toMatchObject(expectedPlanSecurityTypes);
    });
  });

  describe('get plan menu items by listeners types', () => {
    describe('with all plans enabled', () => {
      const everythingEnabled = {
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
        push: {
          enabled: true,
        },
        mtls: {
          enabled: true,
        },
      };

      beforeEach(async () => {
        await init(everythingEnabled);
      });

      it('should filter PUSH plan menu items when user has only HTTP listeners types selected', () => {
        const result = constantsService.getPlanMenuItems('V4', ['HTTP']);

        expect(result).toMatchObject([
          {
            planFormType: 'MTLS',
            name: 'mTLS',
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
        ]);
      });

      it('should filter PUSH plan and mTLS menu items when API definition version is V2', () => {
        const result = constantsService.getPlanMenuItems('V2', null);

        expect(result).toMatchObject([
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
        ]);
      });

      it('should return all plan menu items when user has HTTP and SUBSCRIPTION listeners types selected', () => {
        const result = constantsService.getPlanMenuItems('V4', ['HTTP', 'SUBSCRIPTION']);

        expect(result).toMatchObject(AVAILABLE_PLANS_FOR_MENU);
      });

      it('should return only PUSH plan menu items when user has only SUBSCRIPTION listeners types selected', () => {
        const result = constantsService.getPlanMenuItems('V4', ['SUBSCRIPTION']);

        expect(result).toMatchObject([
          {
            planFormType: 'PUSH',
            name: 'Push plan',
          },
        ]);
      });

      it('should return only KEYLESS and mTLS plans menu items when user has only TCP listeners types selected', () => {
        const result = constantsService.getPlanMenuItems('V4', ['TCP']);

        expect(result).toMatchObject([
          {
            planFormType: 'KEY_LESS',
            name: 'Keyless (public)',
          },
        ]);
      });
    });

    describe('with plans disabled', () => {
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
          push: {
            enabled: false,
          },
        });

        const result = constantsService.getPlanMenuItems('V4', ['HTTP']);

        expect(result).toMatchObject([]);
      });
    });

    it('should return empty list if keyless disabled and user has only TCP listeners types selected', async () => {
      await init({
        apikey: {
          enabled: true,
        },
        jwt: {
          enabled: true,
        },
        keyless: {
          enabled: false,
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
        push: {
          enabled: true,
        },
      });

      const result = constantsService.getPlanMenuItems('V4', ['TCP']);

      expect(result).toMatchObject([]);
    });
  });
});
