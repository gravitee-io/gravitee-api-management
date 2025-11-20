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
import { MatButtonHarness } from '@angular/material/button/testing';

import { AppComponent } from './app.component';
import { PortalMenuLinksService } from '../services/portal-menu-links.service';
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
    @Injectable()
    class PortalMenuLinksServiceStub {
      links = () => [
        {
          id: 'link-id-1',
          type: 'external',
          name: 'link-name-1',
          target: 'link-target-1',
          order: 1,
        },
        {
          id: 'link-id-2',
          type: 'external',
          name: 'link-name-2',
          target: 'link-target-2',
          order: 2,
        },
      ];
    }

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [AppComponent, AppTestingModule],
        providers: [
          {
            provide: PortalMenuLinksService,
            useClass: PortalMenuLinksServiceStub,
          },
        ],
      }).compileComponents();
      fixture = TestBed.createComponent(AppComponent);
      harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    });
    it('should show custom links', async () => {
      const link1Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'link-name-1' }));
      expect(link1Anchor).toBeTruthy();
      const link2Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'link-name-2' }));
      expect(link2Anchor).toBeTruthy();
    });
  });
});
