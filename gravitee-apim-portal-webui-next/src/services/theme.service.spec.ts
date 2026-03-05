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
      dark: {
        color: {
          primary: '#AAAAAA',
          secondary: '#BBBBBB',
          tertiary: '#CCCCCC',
          error: '#DDDDDD',
          background: {
            page: '#1C1B1F',
            card: '#2B2930',
          },
        },
        customCss: '.dark-override { color: white; }',
      },
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(ThemeService);
    httpTestingController = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpTestingController.verify();
    document.documentElement.classList.remove('dark-mode');
    document.getElementById('gio-theme-custom-css')?.remove();
  });

  it('should load light theme by default', done => {
    service.loadTheme().subscribe({
      next: _ => {
        expect(service.darkMode()).toBe(false);
        expect(document.documentElement.style.getPropertyValue('--gio-app-background-color')).toEqual(
          themeResponse.definition.color?.background?.page,
        );
        expect(document.documentElement.style.getPropertyValue('--gio-app-card-background-color')).toEqual(
          themeResponse.definition.color?.background?.card,
        );
        expect(document.documentElement.style.getPropertyValue('--gio-app-primary-main-color')).toEqual('hsl(0, 0%, 0%)');
        expect(document.documentElement.style.getPropertyValue('--gio-app-font-family')).toEqual(themeResponse.definition.font.fontFamily);
        expect(document.documentElement.classList.contains('dark-mode')).toBe(false);

        const style = document.getElementById('gio-theme-custom-css') as HTMLStyleElement;
        expect(style?.innerText).toEqual(themeResponse.definition.customCss);
        done();
      },
      error: _ => fail(),
    });

    httpTestingController.expectOne(`${TESTING_BASE_URL}/theme?type=PORTAL_NEXT`).flush(themeResponse);
  });

  it('should restore dark mode from localStorage', done => {
    localStorage.setItem('gio-portal-dark-mode', 'true');

    service.loadTheme().subscribe({
      next: _ => {
        expect(service.darkMode()).toBe(true);
        expect(document.documentElement.style.getPropertyValue('--gio-app-background-color')).toEqual(
          themeResponse.definition.dark?.color?.background?.page,
        );
        expect(document.documentElement.classList.contains('dark-mode')).toBe(true);

        const style = document.getElementById('gio-theme-custom-css') as HTMLStyleElement;
        expect(style?.innerText).toEqual(themeResponse.definition.dark?.customCss);
        done();
      },
      error: _ => fail(),
    });

    httpTestingController.expectOne(`${TESTING_BASE_URL}/theme?type=PORTAL_NEXT`).flush(themeResponse);
  });

  it('should toggle dark mode and persist preference', done => {
    service.loadTheme().subscribe({
      next: _ => {
        expect(service.darkMode()).toBe(false);

        service.toggleDarkMode();
        expect(service.darkMode()).toBe(true);
        expect(localStorage.getItem('gio-portal-dark-mode')).toEqual('true');
        expect(document.documentElement.classList.contains('dark-mode')).toBe(true);
        expect(document.documentElement.style.getPropertyValue('--gio-app-background-color')).toEqual(
          themeResponse.definition.dark?.color?.background?.page,
        );

        service.toggleDarkMode();
        expect(service.darkMode()).toBe(false);
        expect(localStorage.getItem('gio-portal-dark-mode')).toEqual('false');
        expect(document.documentElement.classList.contains('dark-mode')).toBe(false);
        expect(document.documentElement.style.getPropertyValue('--gio-app-background-color')).toEqual(
          themeResponse.definition.color?.background?.page,
        );

        done();
      },
      error: _ => fail(),
    });

    httpTestingController.expectOne(`${TESTING_BASE_URL}/theme?type=PORTAL_NEXT`).flush(themeResponse);
  });

  it('should swap custom CSS when toggling mode', done => {
    service.loadTheme().subscribe({
      next: _ => {
        let style = document.getElementById('gio-theme-custom-css') as HTMLStyleElement;
        expect(style?.innerText).toEqual(themeResponse.definition.customCss);

        service.toggleDarkMode();
        style = document.getElementById('gio-theme-custom-css') as HTMLStyleElement;
        expect(style?.innerText).toEqual(themeResponse.definition.dark?.customCss);

        done();
      },
      error: _ => fail(),
    });

    httpTestingController.expectOne(`${TESTING_BASE_URL}/theme?type=PORTAL_NEXT`).flush(themeResponse);
  });
});
