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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppComponent } from './app.component';
import { CompanyTitleHarness } from '../components/company-title/company-title.harness';
import { ConfigurationPortalNext } from '../entities/configuration/configuration-portal-next';
import { ConfigService } from '../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let harnessLoader: HarnessLoader;

  describe('default configuration', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [AppComponent, AppTestingModule],
        providers: [provideHttpClientTesting()],
      }).compileComponents();
      fixture = TestBed.createComponent(AppComponent);
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    });

    it('should create the app', () => {
      const app = fixture.componentInstance;
      expect(app).toBeTruthy();
    });

    it(`should have the 'Developer Portal' title`, async () => {
      const companyTitleComponent = await harnessLoader.getHarness(CompanyTitleHarness);
      expect(companyTitleComponent).toBeTruthy();

      expect(await companyTitleComponent.getTitle()).toEqual('Developer Portal');
    });
  });

  describe('custom configuration', () => {
    @Injectable()
    class CustomConfigurationServiceStub {
      get baseURL(): string {
        return TESTING_BASE_URL;
      }
      get portalNext(): ConfigurationPortalNext {
        return { siteTitle: 'My custom title' };
      }
    }

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [AppComponent, AppTestingModule],
        providers: [
          {
            provide: ConfigService,
            useClass: CustomConfigurationServiceStub,
          },
        ],
      }).compileComponents();
      fixture = TestBed.createComponent(AppComponent);
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    });
    it('should show configured title', async () => {
      const companyTitleComponent = await harnessLoader.getHarness(CompanyTitleHarness);
      expect(companyTitleComponent).toBeTruthy();

      expect(await companyTitleComponent.getTitle()).toEqual('My custom title');
    });
  });
});
