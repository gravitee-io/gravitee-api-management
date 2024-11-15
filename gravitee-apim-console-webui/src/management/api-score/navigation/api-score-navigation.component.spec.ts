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
import { BrowserAnimationsModule, NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiScoreNavigationHarness } from './api-score-navigation.harness';
import { ApiScoreNavigationComponent } from './api-score-navigation.component';

import { GioTestingModule } from '../../../shared/testing';

describe('ApiScoreNavigationComponent', () => {
  let fixture: ComponentFixture<ApiScoreNavigationComponent>;
  let componentHarness: ApiScoreNavigationHarness;
  let httpTestingController: HttpTestingController;

  const init = async () => {
    await TestBed.configureTestingModule({
      imports: [ApiScoreNavigationComponent, GioTestingModule, BrowserAnimationsModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiScoreNavigationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiScoreNavigationHarness);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('navigation items', () => {
    beforeEach(async () => {
      await init();
    });

    it('should be displayed', async () => {
      const navItems = await componentHarness.navItemsLocator();
      expect(navItems.length).toEqual(2);
    });

    it('should have correct names', async () => {
      const navItems = await componentHarness.navItemsLocator();
      expect(await navItems[0].text()).toBe('Overview');
      expect(await navItems[1].text()).toBe('Rulesets');
    });
  });
});
