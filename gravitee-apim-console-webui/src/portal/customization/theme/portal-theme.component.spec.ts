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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { HttpTestingController } from '@angular/common/http/testing';

import { PortalThemeComponent } from './portal-theme.component';
import { PortalThemeHarness } from './portal-theme.harness';

import { PortalSettingsModule } from '../../portal-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { ThemePortalNext } from '../../../entities/management-api-v2';

describe('PortalThemeComponent', () => {
  let component: PortalThemeComponent;
  let fixture: ComponentFixture<PortalThemeComponent>;
  let componentHarness: PortalThemeHarness;
  let httpTestingController: HttpTestingController;
  let defaultTheme: ThemePortalNext;
  let currentTheme: ThemePortalNext;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, PortalSettingsModule, HttpClientModule, GioTestingModule],
    }).compileComponents();
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(PortalThemeComponent);
    component = fixture.componentInstance;
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PortalThemeHarness);
    fixture.autoDetectChanges();

    defaultTheme = {
      id: 'default',
      logo: undefined,
      favicon: undefined,
      createdAt: new Date(),
      updatedAt: new Date(),
      type: 'PORTAL_NEXT',
      definition: {
        color: {
          primary: '#613CB0',
          secondary: '#958BA9',
          tertiary: '#B7818F',
          error: '#EC6152',
          pageBackground: '#F7F8FD',
          cardBackground: '#FFFFFF',
        },
        font: {
          fontFamily: '"Roboto", sans-serif',
        },
      },
      name: 'Default Portal Next Theme',
      enabled: false,
    };
    expectDefaultTheme(defaultTheme);

    currentTheme = {
      id: 'current',
      logo: 'logo',
      favicon: 'favicon',
      createdAt: new Date(),
      updatedAt: new Date(),
      type: 'PORTAL_NEXT',
      definition: {
        color: {
          primary: '#704BCF',
          secondary: '#A49ABA',
          tertiary: '#C6909E',
          error: '#FB7061',
          pageBackground: '#06070C',
          cardBackground: '#0E0E0E',
        },
        font: {
          fontFamily: '"Roboto", sans-serif',
        },
        customCss: '',
      },
      name: 'Current Portal Next Theme',
      enabled: false,
    };
    expectCurrentTheme(currentTheme);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should fill form and submit', async () => {
    expect(component).toBeTruthy();

    await componentHarness.setPrimaryColor('primaryColor');
    await componentHarness.reset();
    expect(await componentHarness.getPrimaryColor()).toStrictEqual(currentTheme.definition.color.primary);

    await componentHarness.setPrimaryColor('primaryColor');
    await componentHarness.submit();

    const updateThemeReq = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/themes/${currentTheme.id}`,
    });

    expect(updateThemeReq.request.body).toEqual({
      definition: {
        color: {
          cardBackground: '#0E0E0E',
          error: '#FB7061',
          pageBackground: '#06070C',
          primary: 'primaryColor',
          secondary: '#A49ABA',
          tertiary: '#C6909E',
        },
        customCss: '',
        font: { fontFamily: '"Roboto", sans-serif' },
      },
      enabled: true,
      favicon: 'favicon',
      id: 'current',
      logo: 'logo',
      name: 'Current Portal Next Theme',
      optionalLogo: undefined,
      type: 'PORTAL_NEXT',
    });

    expect(await componentHarness.getPrimaryColor()).toStrictEqual('primaryColor');
  });

  it('should not be able to save', async () => {
    expect(await componentHarness.isSubmitInvalid()).toBeTruthy();
    await componentHarness.setPrimaryColor('primaryColor');
    expect(await componentHarness.isSubmitInvalid()).toBeFalsy();
    await componentHarness.setPrimaryColor(currentTheme.definition.color.primary);
    expect(await componentHarness.isSubmitInvalid()).toBeTruthy();
  });

  function expectDefaultTheme(theme: ThemePortalNext) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/themes/_default?type=PORTAL_NEXT`,
    });
    req.flush(theme);
  }

  function expectCurrentTheme(theme: ThemePortalNext) {
    const req = httpTestingController.expectOne({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/themes/_current?type=PORTAL_NEXT`,
    });
    req.flush(theme);
  }
});
