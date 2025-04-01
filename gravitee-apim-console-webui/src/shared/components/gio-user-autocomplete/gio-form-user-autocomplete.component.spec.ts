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
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { GioFormUserAutocompleteModule } from './gio-form-user-autocomplete.module';
import { GioFormUserAutocompleteHarness } from './gio-form-user-autocomplete.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../testing';
import { fakeSearchableUser } from '../../../entities/user/searchableUser.fixture';
import { SearchableUser } from '../../../entities/user/searchableUser';

@Component({
  template: ` <gio-form-user-autocomplete [formControl]="formControl"></gio-form-user-autocomplete> `,
  standalone: false,
})
class TestComponent {
  formControl = new FormControl();
}

describe('GioFormUserAutocompleteComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let component: TestComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const userFilterPredicate: jest.Mock<boolean> = jest.fn();

  const flashSearchableUser = fakeSearchableUser({ displayName: 'Flash', id: 'flash' });
  const flash2SearchableUser = fakeSearchableUser({ displayName: 'Flash2', id: 'flash2' });

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TestComponent],
      imports: [NoopAnimationsModule, ReactiveFormsModule, GioTestingModule, GioFormUserAutocompleteModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    userFilterPredicate.mockImplementation(() => true);
  });

  afterEach(() => {
    jest.resetAllMocks();
    httpTestingController.verify();
  });

  it('should select user', async () => {
    expect(component.formControl.touched).toEqual(false);
    expect(component.formControl.dirty).toEqual(false);
    expect(component.formControl.errors).toEqual(null);

    const userInput = await loader.getHarness(GioFormUserAutocompleteHarness);
    await userInput.setSearchText('a');
    expect(component.formControl.touched).toEqual(true);
    expect(component.formControl.dirty).toEqual(true);
    expect(component.formControl.errors).toEqual({ invalidUser: true });

    const userToSelect = flashSearchableUser;

    respondToUserSearchRequest('a', [fakeSearchableUser({ displayName: 'Aquaman', id: 'aquaman' }), userToSelect]);

    expect((await userInput.getOptions({ text: 'Aquaman' })).length).toBe(1);
    expect((await userInput.getOptions({ text: 'Flash' })).length).toBe(1);
    await userInput.selectOption({ text: 'Flash' });
    respondToUserSearchRequest('Flash', [flashSearchableUser]);

    expect(component.formControl.value.id).toEqual(userToSelect.id);
    expect(component.formControl.touched).toEqual(true);
    expect(component.formControl.dirty).toEqual(true);
    expect(component.formControl.errors).toEqual(null);
  });

  it('should change selected user', async () => {
    component.formControl.setValue(flashSearchableUser);

    const userInput = await loader.getHarness(GioFormUserAutocompleteHarness);
    expectGetUserRequest(flashSearchableUser.id, flashSearchableUser);
    expect(await userInput.getSearchValue()).toEqual(flashSearchableUser.displayName);
    expect(component.formControl.touched).toEqual(false);
    expect(component.formControl.dirty).toEqual(false);
    expect(component.formControl.errors).toEqual(null);

    await userInput.setSearchText('F');
    respondToUserSearchRequest('F', [flashSearchableUser, flash2SearchableUser]);

    expect(component.formControl.touched).toEqual(true);
    expect(component.formControl.dirty).toEqual(true);
    expect(component.formControl.errors).toEqual({ invalidUser: true });
    expect(component.formControl.value).toEqual(null);

    expect((await userInput.getOptions({ text: 'Flash' })).length).toBe(1);
    expect((await userInput.getOptions({ text: 'Flash2' })).length).toBe(1);

    await userInput.selectOption({ text: 'Flash2' });
    respondToUserSearchRequest('Flash2', [flash2SearchableUser]);

    expect(component.formControl.touched).toEqual(true);
    expect(component.formControl.dirty).toEqual(true);
    expect(component.formControl.errors).toEqual(null);
    expect(component.formControl.value.id).toEqual(flash2SearchableUser.id);
  });

  it('should remove a selected user', async () => {
    component.formControl.setValue(flashSearchableUser);
    fixture.detectChanges();

    expectGetUserRequest(flashSearchableUser.id, flashSearchableUser);

    const userInput = await loader.getHarness(GioFormUserAutocompleteHarness);
    await userInput.clear();

    expect(component.formControl.value).toEqual(null);
    expect(component.formControl.touched).toEqual(true);
    expect(component.formControl.dirty).toEqual(true);
    expect(component.formControl.errors).toEqual(null);
  });

  function respondToUserSearchRequest(searchTerm: string, searchableUsers: SearchableUser[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/search/users?q=${searchTerm}`,
      })
      .flush(searchableUsers);
  }

  function expectGetUserRequest(userId: string, user: SearchableUser) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/users/${userId}`,
      })
      .flush(user);
  }
});
