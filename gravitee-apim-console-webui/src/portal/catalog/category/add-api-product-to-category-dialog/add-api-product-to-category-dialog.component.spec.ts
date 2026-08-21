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

import {
  AddApiProductToCategoryDialogComponent,
  AddApiProductToCategoryDialogData,
  ApiProductCategoryCandidate,
} from './add-api-product-to-category-dialog.component';
import { AddApiProductToCategoryDialogHarness } from './add-api-product-to-category-dialog.harness';

import { fakePortalNavigationApiProduct } from '../../../../entities/management-api-v2';

describe('AddApiProductToCategoryDialogComponent', () => {
  let fixture: ComponentFixture<AddApiProductToCategoryDialogComponent>;
  let harness: AddApiProductToCategoryDialogHarness;
  let dialogRefClose: jest.Mock;
  let dialogData: AddApiProductToCategoryDialogData;

  const PRODUCT_1: ApiProductCategoryCandidate = {
    navigationItem: fakePortalNavigationApiProduct({ id: 'nav-product-1', apiProductId: 'product-1' }),
    name: 'Commerce Product',
    version: '1.0',
  };
  const PRODUCT_2: ApiProductCategoryCandidate = {
    navigationItem: fakePortalNavigationApiProduct({ id: 'nav-product-2', apiProductId: 'product-2' }),
    name: 'Banking Bundle',
    version: '2.0',
  };

  beforeEach(async () => {
    dialogRefClose = jest.fn();
    dialogData = { title: 'Add API Product to Category', candidates: [PRODUCT_1, PRODUCT_2] };

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, AddApiProductToCategoryDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
        { provide: MAT_DIALOG_DATA, useFactory: () => dialogData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddApiProductToCategoryDialogComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, AddApiProductToCategoryDialogHarness);
  });

  it('should list candidate API Products with their versions', async () => {
    expect(await harness.getOptionLabels()).toEqual(['Commerce Product (1.0)', 'Banking Bundle (2.0)']);
  });

  it('should explicitly associate the search input with its label', () => {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('#api-product-select-input');
    const label: HTMLElement = fixture.nativeElement.querySelector('#api-product-search-label');

    expect(input.getAttribute('aria-labelledby')).toEqual(label.id);
  });

  it('should filter candidates by API Product name', async () => {
    expect(await harness.getOptionLabels('Banking')).toEqual(['Banking Bundle (2.0)']);
  });

  it('should close with the selected API Product candidate', async () => {
    await harness.fillFormAndSubmit('Banking Bundle');

    expect(dialogRefClose).toHaveBeenCalledWith(PRODUCT_2);
  });

  it('should close with undefined on cancel', async () => {
    await harness.cancel();

    expect(dialogRefClose).toHaveBeenCalledWith();
  });
});
