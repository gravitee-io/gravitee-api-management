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

import { Breadcrumb, BreadcrumbsComponent } from './breadcrumbs.component';
import { BreadcrumbsComponentHarness } from './breadcrumbs.component.harness';

describe('BreadcrumbsComponent', () => {
  let fixture: ComponentFixture<BreadcrumbsComponent>;
  let harness: BreadcrumbsComponentHarness;

  const init = async (breadcrumbs: Breadcrumb[]) => {
    await TestBed.configureTestingModule({
      imports: [BreadcrumbsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BreadcrumbsComponent);
    fixture.componentRef.setInput('breadcrumbs', breadcrumbs);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, BreadcrumbsComponentHarness);
  };

  it('should not display any items', async () => {
    await init([]);

    const breadcrumbs = await harness.getBreadcrumbItems();
    expect(breadcrumbs.length).toEqual(0);

    const separators = await harness.getBreadcrumbSeparators();
    expect(separators.length).toEqual(0);
  });

  it('should display items with separators', async () => {
    await init([
      {
        id: 'item1',
        label: 'Item 1',
      },
      {
        id: 'item2',
        label: 'Item 2',
      },
      {
        id: 'item3',
        label: 'Item 3',
      },
    ]);

    const breadcrumbs = await harness.getBreadcrumbItems();
    expect(breadcrumbs.length).toEqual(3);
    expect(await Promise.all(breadcrumbs.map(x => x.text()))).toEqual(['Item 1', 'Item 2', 'Item 3']);

    const separators = await harness.getBreadcrumbSeparators();
    expect(separators.length).toEqual(2);
  });
});
