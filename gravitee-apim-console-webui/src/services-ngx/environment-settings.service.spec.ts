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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EnvironmentSettingsService } from './environment-settings.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { EnvSettings } from '../entities/Constants';

describe('EnvironmentSettingsService', () => {
  let httpTestingController: HttpTestingController;
  let environmentSettingsService: EnvironmentSettingsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    environmentSettingsService = TestBed.inject<EnvironmentSettingsService>(EnvironmentSettingsService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should load environment settings', () => {
    const envSettingsMock = {
      analytics: {
        clientTimeout: 300,
      },
    };

    expect(environmentSettingsService.getSnapshot()).toBeNull();

    // Subscribe before calling load
    const expectedGetSettingsValue = [];
    environmentSettingsService.get().subscribe(settings => {
      expectedGetSettingsValue.push(settings);
    });

    // First call
    environmentSettingsService.load().subscribe();
    expectGetEnvironmentSettingsRequest(envSettingsMock);

    expect(environmentSettingsService.getSnapshot()).toEqual(envSettingsMock);

    expect(expectedGetSettingsValue).toEqual([envSettingsMock]);

    // Second call
    environmentSettingsService.load().subscribe();
    expectGetEnvironmentSettingsRequest(envSettingsMock);

    expect(expectedGetSettingsValue).toEqual([envSettingsMock, envSettingsMock]);

    // Subscribe after calling load
    const expectedGetSettingsSecondValue = [];
    environmentSettingsService.get().subscribe(settings => {
      expectedGetSettingsSecondValue.push(settings);
    });
    expect(expectedGetSettingsSecondValue).toEqual([envSettingsMock]);
  });

  function expectGetEnvironmentSettingsRequest(envSettings: Partial<EnvSettings>) {
    httpTestingController
      .expectOne({
        url: `${CONSTANTS_TESTING.env.baseURL}/portal`,
        method: 'GET',
      })
      .flush(envSettings);
  }
});
