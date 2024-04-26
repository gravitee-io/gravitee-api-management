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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { NavBarComponent } from './nav-bar.component';
import { fakeUser } from '../../entities/user/user.fixtures';
import { CurrentUserService } from '../../services/current-user.service';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('NavBarComponent', () => {
  let fixture: ComponentFixture<NavBarComponent>;
  let harnessLoader: HarnessLoader;
  let currentUserService: CurrentUserService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavBarComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(NavBarComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    currentUserService = TestBed.inject(CurrentUserService);
    fixture.detectChanges();
  });

  it('should show login button if user not connected', async () => {
    let logInButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Log in' }));
    expect(logInButton).toBeTruthy();
    currentUserService.user.set(fakeUser());
    logInButton = await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Log in' }));
    expect(logInButton).toBeFalsy();
  });
});
