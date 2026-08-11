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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AddApiToCategoryDialogComponent, AddApiToCategoryDialogData } from './add-api-to-category-dialog.component';
import { AddApiToCategoryDialogHarness } from './add-api-to-category-dialog.harness';

import { fakePortalNavigationApi } from '../../../../entities/management-api-v2';

describe('AddApiToCategoryDialogComponent', () => {
  let fixture: ComponentFixture<AddApiToCategoryDialogComponent>;
  let harness: AddApiToCategoryDialogHarness;
  let dialogRefClose: jest.Mock;
  let dialogData: AddApiToCategoryDialogData;

  const API_1 = fakePortalNavigationApi({ id: 'nav-api-1', apiId: 'api-1', title: 'API One' });
  const API_2 = fakePortalNavigationApi({ id: 'nav-api-2', apiId: 'api-2', title: 'API Two' });

  beforeEach(async () => {
    dialogRefClose = jest.fn();
    dialogData = { title: 'Add API to Category', candidates: [API_1, API_2] };

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, AddApiToCategoryDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        { provide: MAT_DIALOG_DATA, useFactory: () => dialogData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddApiToCategoryDialogComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AddApiToCategoryDialogHarness);
  });

  it('should list all candidate APIs', async () => {
    expect(await harness.getOptionLabels()).toEqual(['API One', 'API Two']);
  });

  it('should filter candidates as the user types', async () => {
    expect(await harness.getOptionLabels('Two')).toEqual(['API Two']);
  });

  it('should close with the selected navigation item', async () => {
    await harness.fillFormAndSubmit('API Two');

    expect(dialogRefClose).toHaveBeenCalledWith(API_2);
  });

  it('should close with undefined on cancel', async () => {
    await harness.cancel();

    expect(dialogRefClose).toHaveBeenCalledWith();
  });
});
