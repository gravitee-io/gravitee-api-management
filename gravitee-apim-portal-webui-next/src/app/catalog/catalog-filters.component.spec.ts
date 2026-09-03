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
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { CatalogFilterField, CatalogFilterSelection } from './catalog-filters';
import { CatalogFiltersComponent } from './catalog-filters.component';

describe('CatalogFiltersComponent', () => {
  let fixture: ComponentFixture<CatalogFiltersComponent>;
  let loader: HarnessLoader;

  const fields: CatalogFilterField[] = [
    {
      key: 'access',
      label: 'Access',
      values: [
        { value: 'NO_KEY', label: 'No key needed', count: 2 },
        { value: 'CREDENTIALS', label: 'Credentials needed', count: 1 },
      ],
    },
    { key: 'tag', label: 'Tags', values: [{ value: 'incident', label: 'incident', count: 3 }] },
  ];

  const init = async (selection: CatalogFilterSelection = {}) => {
    await TestBed.configureTestingModule({ imports: [CatalogFiltersComponent, NoopAnimationsModule] }).compileComponents();
    fixture = TestBed.createComponent(CatalogFiltersComponent);
    fixture.componentRef.setInput('fields', fields);
    fixture.componentRef.setInput('selection', selection);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  };

  const sectionTitles = () =>
    Array.from(fixture.nativeElement.querySelectorAll('.catalog-filters__name')).map(node => (node as HTMLElement).textContent?.trim());

  afterEach(() => fixture.destroy());

  it('should list every field it is given, with a count on each value', async () => {
    await init();

    expect(sectionTitles()).toEqual(['Access', 'Tags']);
    expect(Array.from(fixture.nativeElement.querySelectorAll('.catalog-filters__count')).map(n => (n as HTMLElement).textContent)).toEqual([
      '2',
      '1',
      '3',
    ]);
  });

  it('should report a value the reader ticks, and report it gone when they untick it', async () => {
    await init();
    const emitted: CatalogFilterSelection[] = [];
    fixture.componentInstance.selection.subscribe(value => emitted.push(value));

    const checkbox = await loader.getHarness(MatCheckboxHarness.with({ label: 'No key needed' }));
    await checkbox.check();
    fixture.componentRef.setInput('selection', emitted[emitted.length - 1]);
    fixture.detectChanges();

    expect(emitted[emitted.length - 1]).toEqual({ access: ['NO_KEY'] });

    await (await loader.getHarness(MatCheckboxHarness.with({ label: 'No key needed' }))).uncheck();

    expect(emitted[emitted.length - 1]).toEqual({ access: [] });
  });

  it('should collapse a section and bring it back', async () => {
    await init();
    const header = fixture.nativeElement.querySelector('[data-testid="filter-access"]') as HTMLButtonElement;

    header.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="section-access"] .catalog-filters__values')).toBeNull();
    expect(header.getAttribute('aria-expanded')).toBe('false');

    header.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="section-access"] .catalog-filters__values')).not.toBeNull();
  });

  it('should show a value the reader picked even when nothing on screen carries it', async () => {
    await init({ access: ['NO_KEY'] });

    const checkbox = await loader.getHarness(MatCheckboxHarness.with({ label: 'No key needed' }));
    expect(await checkbox.isChecked()).toBe(true);
  });
});
