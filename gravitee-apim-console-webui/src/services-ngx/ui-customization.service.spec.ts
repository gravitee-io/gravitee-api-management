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

import { UiCustomizationService } from './ui-customization.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { ConsoleCustomization } from '../entities/management-api-v2/consoleCustomization';

describe('UiCustomizationService', () => {
  let httpTestingController: HttpTestingController;
  let service: UiCustomizationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<UiCustomizationService>(UiCustomizationService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should call the API', done => {
    const customization: ConsoleCustomization = {
      title: 'custom title',
      logo: 'custom logo',
      favicon: 'custom favicon',
      theme: {
        menuBackground: 'menuBackground',
        menuActive: 'menuActive',
      },
      ctaConfiguration: {
        customEnterpriseName: 'EE',
        title: 'custom CTA title',
        trialButtonLabel: 'trial',
        trialURL: 'http://url.com',
        hideDays: true,
      },
    };

    service.getConsoleCustomization().subscribe(res => {
      expect(res).toEqual(customization);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.org.v2BaseURL}/ui/customization`,
      method: 'GET',
    });

    req.flush(customization);
  });
});
