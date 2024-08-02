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

import { UiPortalThemeService } from './ui-theme.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { ThemePortalNext } from '../entities/management-api-v2';

describe('UiPortalThemeService', () => {
  let httpTestingController: HttpTestingController;
  let service: UiPortalThemeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<UiPortalThemeService>(UiPortalThemeService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should call the API', (done) => {
    const themePortalNext: ThemePortalNext = {
      id: 'theme id',
      name: 'theme name',
      type: 'PORTAL_NEXT',
      createdAt: new Date(),
      updatedAt: new Date(),
      enabled: true,
      logo: 'theme logo',
      optionalLogo: 'theme optionalLogo',
      favicon: 'theme favicon',
      definition: {
        color: {
          primary: '#000000',
          secondary: '#333333',
          tertiary: '#555555',
          error: '#777777',
          pageBackground: '#999999',
          cardBackground: '#AAAAAA',
        },
        font: {
          fontFamily: '"Roboto", sans serif',
        },
        customCss: '.style {}',
      },
    };

    service.getDefaultTheme('PORTAL_NEXT').subscribe((res) => {
      expect(res).toEqual(themePortalNext);
      done();
    });

    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/themes/_default?type=PORTAL_NEXT`,
      method: 'GET',
    });

    req.flush(themePortalNext);
  });
});
