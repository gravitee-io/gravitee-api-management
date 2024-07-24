/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { Theme, ThemeService } from './theme.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ThemeService', () => {
  let service: ThemeService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(ThemeService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('load theme', done => {
    const themeResponse: Theme = {
      definition: {
        color: {
          primary: '#000000',
          secondary: '#111111',
          tertiary: '#222222',
          error: '#333333',
          background: {
            page: '#444444',
            card: '#555555',
          },
        },
        font: { fontFamily: '"Roboto", sans serif' },
        customCss: 'app-root { background: blue; }',
      },
    };

    service.loadTheme().subscribe({
      next: _ => {
        expect(document.documentElement.style.getPropertyValue('--gio-app-background-color')).toEqual(
          themeResponse.definition.color?.background?.page,
        );
        expect(document.documentElement.style.getPropertyValue('--gio-app-card-background-color')).toEqual(
          themeResponse.definition.color?.background?.card,
        );
        expect(document.documentElement.style.getPropertyValue('--gio-app-primary-main-color')).toEqual('hsl(0, 0%, 0%)');
        expect(document.documentElement.style.getPropertyValue('--gio-app-secondary-main-color')).toEqual(
          'hsl(0, 0%, 0.06666666666666667%)',
        );
        expect(document.documentElement.style.getPropertyValue('--gio-app-tertiary-main-color')).toEqual(
          'hsl(0, 0%, 0.13333333333333333%)',
        );
        expect(document.documentElement.style.getPropertyValue('--gio-app-error-main-color')).toEqual('hsl(0, 0%, 0.2%)');
        expect(document.documentElement.style.getPropertyValue('--gio-app-font-family')).toEqual(themeResponse.definition.font.fontFamily);
        const style: HTMLStyleElement = document.getElementsByTagName('style')[0];
        expect(style.innerText).toEqual(themeResponse.definition.customCss);
        done();
      },
      error: _ => fail(),
    });

    httpTestingController.expectOne(`${TESTING_BASE_URL}/theme?type=PORTAL_NEXT`).flush(themeResponse);
  });
});
