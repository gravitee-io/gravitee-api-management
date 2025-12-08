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
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { AppComponent } from './app.component';
import { PortalNavigationItem } from '../entities/portal-navigation/portal-navigation-item';
import { PortalNavigationItemsService } from '../services/portal-navigation-items.service';
import { AppTestingModule } from '../testing/app-testing.module';

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
  });

  describe('custom links', () => {
    beforeEach(async () => {
      const mockItems: PortalNavigationItem[] = [
        {
          id: 'l1',
          organizationId: 'org1',
          environmentId: 'env1',
          title: 'link-name-1',
          type: 'LINK',
          area: 'TOP_NAVBAR',
          order: 0,
          url: '/link1',
        },
        {
          id: 'l2',
          organizationId: 'org1',
          environmentId: 'env1',
          title: 'link-name-2',
          type: 'LINK',
          area: 'TOP_NAVBAR',
          order: 1,
          url: '/link2',
        },
      ];

      await TestBed.configureTestingModule({
        imports: [AppComponent, AppTestingModule],
        providers: [
          provideHttpClientTesting(),
          {
            provide: PortalNavigationItemsService,
            useValue: { topNavbar: signal(mockItems) },
          },
        ],
      }).compileComponents();
      fixture = TestBed.createComponent(AppComponent);
      fixture.detectChanges();
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    });
    it('should show custom links', async () => {
      const link1Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /link-name-1/i }));
      expect(link1Anchor).toBeTruthy();
      const link2Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: /link-name-2/i }));
      expect(link2Anchor).toBeTruthy();
    });
  });
});
