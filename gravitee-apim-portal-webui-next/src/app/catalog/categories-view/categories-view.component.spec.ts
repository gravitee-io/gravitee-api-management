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
import { MatCardHarness } from '@angular/material/card/testing';

import { CategoriesViewComponent } from './categories-view.component';
import { CategoryCardHarness } from '../../../components/category-card/category-card.harness';
import { Category } from '../../../entities/categories/categories';
import { fakeCategory } from '../../../entities/categories/categories.fixture';
import { AppTestingModule } from '../../../testing/app-testing.module';

describe('CategoriesViewComponent', () => {
  let fixture: ComponentFixture<CategoriesViewComponent>;
  let harnessLoader: HarnessLoader;

  const init = async (categories: Category[]) => {
    await TestBed.configureTestingModule({
      imports: [CategoriesViewComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesViewComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.componentRef.setInput('categories', categories);
    fixture.detectChanges();
  };

  describe('No categories', () => {
    beforeEach(async () => {
      await init([]);
    });
    it('should show message when no categories', async () => {
      const emptyCategoriesMessage = await harnessLoader.getHarness(MatCardHarness.with({ selector: '#no-categories' }));
      expect(await emptyCategoriesMessage.getText()).toContain('Sorry, there are no categories listed yet');
    });
    it('should show button to view all APIs', async () => {
      expect(await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'View all APIs' }))).toBeTruthy();
    });
  });

  describe('With categories', () => {
    beforeEach(async () => {
      await init([fakeCategory()]);
    });
    it('should show categories', async () => {
      const categoryCards = await harnessLoader.getAllHarnesses(CategoryCardHarness);
      expect(categoryCards).toHaveLength(1);
    });
  });
});
