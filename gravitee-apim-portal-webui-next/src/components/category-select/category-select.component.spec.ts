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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { CategorySelectComponent } from './category-select.component';
import { CategorySelectHarness } from './category-select.component.harness';
import { fakePortalCategory } from '../../entities/categories/portal-category.fixture';

describe('CategorySelectComponent', () => {
  let fixture: ComponentFixture<CategorySelectComponent>;
  let harness: CategorySelectHarness;

  const categories = [
    fakePortalCategory({ id: 'cat-1', title: 'Category One' }),
    fakePortalCategory({ id: 'cat-2', title: 'Category Two' }),
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategorySelectComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(CategorySelectComponent);
    fixture.componentRef.setInput('categories', categories);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, CategorySelectHarness);
  });

  it('should default to "All"', async () => {
    expect(await harness.getSelectedText()).toEqual('All');
  });

  it('should reflect the bound value', async () => {
    fixture.componentRef.setInput('categories', categories);
    fixture.componentInstance.value.set('cat-2');
    fixture.detectChanges();

    expect(await harness.getSelectedText()).toEqual('Category Two');
  });

  it('should update the value model when an option is selected', async () => {
    await harness.selectCategory('Category One');
    fixture.detectChanges();

    expect(fixture.componentInstance.value()).toEqual('cat-1');
  });

  it('should clear the value model when "All categories" is selected', async () => {
    fixture.componentInstance.value.set('cat-2');
    fixture.detectChanges();

    await harness.selectCategory('All categories');
    fixture.detectChanges();

    expect(fixture.componentInstance.value()).toBeNull();
    expect(await harness.getSelectedText()).toEqual('All');
  });
});
