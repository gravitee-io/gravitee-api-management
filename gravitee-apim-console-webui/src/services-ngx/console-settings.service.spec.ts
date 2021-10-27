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
import { ProviderToken } from '@angular/core';

import { ConsoleSettingsService } from './console-settings.service';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../shared/testing';
import { ConsoleSettings } from '../entities/consoleSettings';
import { Constants } from '../entities/Constants';

describe('ConsoleSettingsService', () => {
  let httpTestingController: HttpTestingController;
  let consoleSettingsService: ConsoleSettingsService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    consoleSettingsService = TestBed.inject<ConsoleSettingsService>(ConsoleSettingsService);
  });

  describe('save', () => {
    it('should save consoleSettings', (done) => {
      const consoleSettingsPayload: ConsoleSettings = {
        theme: {
          css: 'hello.css',
        },
      };
      consoleSettingsService.save(consoleSettingsPayload).subscribe((consoleSettings) => {
        expect(consoleSettings).toEqual(newConsoleSettings());
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(consoleSettingsPayload);

      req.flush(newConsoleSettings());
    });

    it('should merge saved consoleSettings result to Constants provider', (done) => {
      const consoleSettingsPayload: ConsoleSettings = {
        theme: {
          css: 'hello.css',
        },
      };
      const constants = TestBed.inject('Constants' as unknown as ProviderToken<Constants>);

      consoleSettingsService.save(consoleSettingsPayload).subscribe(() => {
        expect(constants.org.settings).toEqual(newConsoleSettings());
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(consoleSettingsPayload);

      req.flush(newConsoleSettings());
    });

    it('should enable localLogin if no Idp defined', (done) => {
      const consoleSettingsPayload: ConsoleSettings = {
        authentication: {
          localLogin: { enabled: false },
        },
      };

      consoleSettingsService.save(consoleSettingsPayload).subscribe((consoleSettings) => {
        expect(consoleSettings?.authentication?.localLogin?.enabled).toEqual(true);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(consoleSettingsPayload);

      req.flush(newConsoleSettings());
    });
  });

  describe('get', () => {
    it('should return consoleSettings', (done) => {
      consoleSettingsService.get().subscribe((consoleSettings) => {
        expect(consoleSettings).toEqual(newConsoleSettings());
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/settings`);
      expect(req.request.method).toEqual('GET');

      req.flush(newConsoleSettings());
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});

const newConsoleSettings = (): ConsoleSettings => {
  return {
    alert: {
      enabled: false,
    },
    authentication: {
      google: {},
      github: {},
      oauth2: {},
      localLogin: {
        enabled: true,
      },
    },
    reCaptcha: {
      enabled: false,
      siteKey: '',
    },
    scheduler: {
      tasks: 10,
      notifications: 10,
    },
    logging: {
      maxDurationMillis: 0,
      audit: {
        enabled: false,
        trail: {
          enabled: false,
        },
      },
      user: {
        displayed: false,
      },
    },
    maintenance: {
      enabled: false,
    },
    management: {
      support: {
        enabled: true,
      },
      title: 'Gravitee.io Management Test GME',
      url: 'https://nightly.gravitee.io',
      userCreation: {
        enabled: true,
      },
      automaticValidation: {
        enabled: false,
      },
    },
    newsletter: {
      enabled: true,
    },
    theme: {
      name: 'default',
      logo: 'themes/assets/gravitee-logo.svg',
      loader: 'assets/gravitee_logo_anim.gif',
    },
  };
};
