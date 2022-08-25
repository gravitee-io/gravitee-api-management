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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiListModule } from './api-list.module';
import { ApiListComponent } from './api-list.component';

import { GioUiRouterTestingModule } from '../../../shared/testing/gio-uirouter-testing-module';
import { CurrentUserService, UIRouterState } from '../../../ajs-upgraded-providers';
import { User as DeprecatedUser } from '../../../entities/user';

describe('ApisListComponent', () => {
  const fakeUiRouter = { go: jest.fn() };

  let fixture: ComponentFixture<ApiListComponent>;

  beforeEach(async () => {
    const currentUser = new DeprecatedUser();
    currentUser.userPermissions = ['environment-api-c'];

    await TestBed.configureTestingModule({
      imports: [ApiListModule, MatIconTestingModule, GioUiRouterTestingModule],
      providers: [
        { provide: UIRouterState, useValue: fakeUiRouter },
        { provide: CurrentUserService, useValue: { currentUser } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ApiListComponent);
  });

  describe('onAddApiClick', () => {
    let loader: HarnessLoader;

    beforeEach(async () => {
      fixture.detectChanges();
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    it('should navigate to new apis page on click to add button', async () => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="add-api"]' })).then((button) => button.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.new');
    });
  });
});
