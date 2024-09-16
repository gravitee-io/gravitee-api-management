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
import { MatIcon } from '@angular/material/icon';

import { BothPortalsBadgeHarness } from './both-portals-badge.harness';
import { BothPortalsBadgeComponent } from './both-portals-badge.component';

describe('BothPortalsBadgeComponent', () => {
  let fixture: ComponentFixture<BothPortalsBadgeComponent>;
  let componentHarness: BothPortalsBadgeHarness;

  const appliesToBothPortals = 'Applies to both portals';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatIcon],
    }).compileComponents();
    fixture = TestBed.createComponent(BothPortalsBadgeComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BothPortalsBadgeHarness);
    fixture.detectChanges();
  });

  it('elements should be present', async () => {
    expect(await componentHarness.getBadgeWarningText()).toEqual(appliesToBothPortals);
    expect(await componentHarness.getBadgeIconName()).toBeTruthy();
  });
});
