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
/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApiGeneralInfoIncludedInDialogComponent } from './api-general-info-included-in-dialog.component';
import { ApiGeneralInfoIncludedInDialogHarness } from './api-general-info-included-in-dialog.harness';

import { ApiProduct } from '../../../../entities/management-api-v2/api-product/apiProduct';
import { GioTestingModule } from '../../../../shared/testing';

describe('ApiGeneralInfoIncludedInDialogComponent', () => {
  const productA: ApiProduct = {
    id: 'product-a',
    name: 'Product Alpha',
    version: '1.0',
    description: 'First product',
  };
  const productB: ApiProduct = {
    id: 'product-b',
    name: 'Product Beta',
    version: '1.0',
    description: 'Second product',
  };
  const productC: ApiProduct = {
    id: 'product-c',
    name: 'Gamma API',
    version: '2.0',
    description: 'Gamma description',
  };

  let harness: ApiGeneralInfoIncludedInDialogHarness;

  const initComponent = async (apiProducts: ApiProduct[]) => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ApiGeneralInfoIncludedInDialogComponent],
      providers: [{ provide: MAT_DIALOG_DATA, useValue: { apiProducts } }],
    });
    const fixture = TestBed.createComponent(ApiGeneralInfoIncludedInDialogComponent);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiGeneralInfoIncludedInDialogHarness);
  };

  describe('with API products', () => {
    beforeEach(async () => {
      await initComponent([productA, productB, productC]);
    });

    it('should display the title with product count', async () => {
      expect(await harness.getTitleText()).toBe('Included in 3 API Products');
    });

    it('should display all product names', async () => {
      const names = await harness.getProductNames();
      expect(names).toEqual(['Product Alpha', 'Product Beta', 'Gamma API']);
    });

    it('should filter products by search term (name)', async () => {
      await harness.setSearchTerm('Alpha');
      const names = await harness.getProductNames();
      expect(names).toEqual(['Product Alpha']);
    });

    it('should filter products by search term (description)', async () => {
      await harness.setSearchTerm('Gamma description');
      const names = await harness.getProductNames();
      expect(names).toEqual(['Gamma API']);
    });

    it('should show no products when search has no matches', async () => {
      await harness.setSearchTerm('nonexistent');
      const names = await harness.getProductNames();
      expect(names).toEqual([]);
    });

    it('should show empty message when search has no matches', async () => {
      await harness.setSearchTerm('nonexistent');
      const emptyText = await harness.getEmptyMessageText();
      expect(emptyText).toBe('No API Products matching your search');
    });

    it('should get product by unique id', async () => {
      const productHarness = await harness.getProductById('product-b');
      expect(productHarness).not.toBeNull();
      expect(await productHarness!.getText()).toBe('Product Beta');
    });

    it('should return null for non-existent product id', async () => {
      const productDiv = await harness.getProductById('non-existent');
      expect(productDiv).toBeNull();
    });
  });

  describe('with empty API products', () => {
    beforeEach(async () => {
      await initComponent([]);
    });

    it('should display the title with zero count', async () => {
      expect(await harness.getTitleText()).toBe('Included in 0 API Products');
    });

    it('should show empty state message', async () => {
      const emptyText = await harness.getEmptyMessageText();
      expect(emptyText).toBe('No API Products matching your search');
    });

    it('should show no product names', async () => {
      const names = await harness.getProductNames();
      expect(names).toEqual([]);
    });
  });

  describe('search input', () => {
    beforeEach(async () => {
      await initComponent([productA]);
    });

    it('should allow typing in search field', async () => {
      await harness.setSearchTerm('test');
      expect(await harness.getSearchValue()).toBe('test');
    });

    it('should clear filter when search is cleared', async () => {
      await harness.setSearchTerm('Alpha');
      expect(await harness.getProductNames()).toEqual(['Product Alpha']);

      await harness.setSearchTerm('');
      expect(await harness.getProductNames()).toEqual(['Product Alpha']);
    });
  });
});
