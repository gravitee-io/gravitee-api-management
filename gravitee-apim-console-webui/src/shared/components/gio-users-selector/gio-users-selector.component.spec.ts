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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatAutocompleteHarness } from '@angular/material/autocomplete/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { GioUsersSelectorModule } from './gio-users-selector.module';
import { GioUsersSelectorComponent } from './gio-users-selector.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../testing';
import { fakeSearchableUser } from '../../../entities/user/searchableUser.fixture';
import { SearchableUser } from '../../../entities/user/searchableUser';

describe('GioUsersSelectorComponent', () => {
  let fixture: ComponentFixture<GioUsersSelectorComponent>;
  let component: GioUsersSelectorComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const userFilterPredicate: jest.Mock<boolean> = jest.fn();
  const matDialogRefMock: Partial<MatDialogRef<GioUsersSelectorComponent>> = {
    close: jest.fn(),
  };
  const flashSearchableUser = fakeSearchableUser({ displayName: 'Flash', id: 'flash' });
  const flash2SearchableUser = fakeSearchableUser({ displayName: 'Flash2', id: 'flash2' });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, GioUsersSelectorModule],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            userFilterPredicate,
          },
        },
        { provide: MatDialogRef, useValue: matDialogRefMock },
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GioUsersSelectorComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    userFilterPredicate.mockImplementation(() => true);
  });

  afterEach(() => {
    userFilterPredicate.mockReset();
    httpTestingController.verify();
  });

  describe('search', () => {
    it('should call the API and display the responses as options', async () => {
      const searchAutocomplete = await loader.getHarness(MatAutocompleteHarness);
      await searchAutocomplete.enterText('a');

      const userToSelect = flashSearchableUser;

      respondToUserSearchRequest('a', [fakeSearchableUser({ displayName: 'Aquaman', id: 'aquaman' }), userToSelect]);

      expect((await searchAutocomplete.getOptions({ text: 'Aquaman' })).length).toBe(1);
      expect((await searchAutocomplete.getOptions({ text: 'Flash' })).length).toBe(1);
      await searchAutocomplete.selectOption({ text: 'Flash' });

      expect(component.selectedUsers).toEqual([
        { ...userToSelect, userPicture: 'https://url.test:3000/management/organizations/DEFAULT/users/flash/avatar' },
      ]);
    });

    it('should filter the already selected options', async () => {
      const searchAutocomplete = await loader.getHarness(MatAutocompleteHarness);
      await searchAutocomplete.enterText('F');

      respondToUserSearchRequest('F', [flashSearchableUser, flash2SearchableUser]);

      expect((await searchAutocomplete.getOptions({ text: 'Flash' })).length).toBe(1);
      expect((await searchAutocomplete.getOptions({ text: 'Flash2' })).length).toBe(1);
      await searchAutocomplete.selectOption({ text: 'Flash2' });

      await searchAutocomplete.enterText('Fl');

      respondToUserSearchRequest('Fl', [flashSearchableUser, flash2SearchableUser]);

      const options = await searchAutocomplete.getOptions();
      expect(options.length).toBe(1);
      expect(await options[0].getText()).toBe('Flash');
    });

    it('should filter according the input predicate already selected options', async () => {
      userFilterPredicate.mockImplementation(user => user.id !== 'flash');

      const searchAutocomplete = await loader.getHarness(MatAutocompleteHarness);
      await searchAutocomplete.enterText('F');

      respondToUserSearchRequest('F', [flashSearchableUser, flash2SearchableUser]);

      const options = await searchAutocomplete.getOptions();
      expect(options.length).toBe(1);
      expect(await options[0].getText()).toBe('Flash2');
    });
  });

  it('should remove a selected user', async () => {
    fixture.detectChanges();

    const searchAutocomplete = await loader.getHarness(MatAutocompleteHarness);
    await searchAutocomplete.enterText('F');

    respondToUserSearchRequest('F', [flashSearchableUser]);

    await searchAutocomplete.selectOption({ text: 'Flash' });

    expect(component.selectedUsers).toEqual([
      {
        ...flashSearchableUser,
        userPicture: 'https://url.test:3000/management/organizations/DEFAULT/users/flash/avatar',
      },
    ]);

    const removeUserButton = await loader.getHarness(
      MatButtonHarness.with({ selector: '[aria-label="Button to remove user from selection"]' }),
    );
    await removeUserButton.click();

    expect(component.selectedUsers).toEqual([]);
  });

  function respondToUserSearchRequest(searchTerm: string, searchableUsers: SearchableUser[]) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/search/users?q=${searchTerm}`,
      })
      .flush(searchableUsers);
  }
});
