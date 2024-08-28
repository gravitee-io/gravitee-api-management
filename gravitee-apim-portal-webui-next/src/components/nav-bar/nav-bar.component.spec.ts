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
import { ComponentRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { NavBarComponent } from './nav-bar.component';
import { fakeUser } from '../../entities/user/user.fixtures';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('NavBarComponent', () => {
  let fixture: ComponentFixture<NavBarComponent>;
  let harnessLoader: HarnessLoader;
  let componentRef: ComponentRef<NavBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavBarComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(NavBarComponent);
    componentRef = fixture.componentRef;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should show login button if user not connected', async () => {
    let logInButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Log in' }));
    expect(logInButton).toBeTruthy();
    componentRef.setInput('currentUser', fakeUser());
    logInButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Log in' }));
    expect(logInButton).toBeFalsy();
  });

  it('should show custom links', async () => {
    const customLinks = [
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

    componentRef.setInput('customLinks', customLinks);
    const link1Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'link-name-1' }));
    expect(link1Anchor).toBeTruthy();
    const link2Anchor = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'link-name-2' }));
    expect(link2Anchor).toBeTruthy();
  });
});
