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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DataTableComponent } from './data-table.component';
import { DataTableHarness } from './data-table.harness';

@Component({
  selector: 'gke-test-host',
  standalone: true,
  imports: [DataTableComponent],
  template: `
    <gke-data-table
      [filterable]="filterable()"
      [filterPlaceholder]="filterPlaceholder()"
      [loading]="loading()"
      [empty]="empty()"
      [emptyMessage]="emptyMessage()"
      [paginated]="paginated()"
      [totalElements]="totalElements()"
      [page]="page()"
      [pageSize]="pageSize()"
      (filterChange)="lastFilter = $event"
      (pageChange)="lastPage = $event"
    >
      <p class="projected-content">Table goes here</p>
    </gke-data-table>
  `,
})
class TestHostComponent {
  filterable = input(false);
  filterPlaceholder = input('Filter...');
  loading = input(false);
  empty = input(false);
  emptyMessage = input('No data available');
  paginated = input(false);
  totalElements = input(0);
  page = input(0);
  pageSize = input(25);

  lastFilter = '';
  lastPage: { page: number; pageSize: number } | null = null;
}

describe('DataTableComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  it('should project content via ng-content', () => {
    const el = fixture.nativeElement.querySelector('.projected-content');
    expect(el.textContent).toContain('Table goes here');
  });

  it('should not show filter by default', async () => {
    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.hasFilter()).toBe(false);
  });

  it('should show filter when filterable is true', async () => {
    fixture.componentRef.setInput('filterable', true);
    fixture.detectChanges();

    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.hasFilter()).toBe(true);
  });

  it('should emit filterChange on input', async () => {
    fixture.componentRef.setInput('filterable', true);
    fixture.detectChanges();

    const harness = await loader.getHarness(DataTableHarness);
    await harness.setFilter('test');

    expect(fixture.componentInstance.lastFilter).toBe('test');
  });

  it('should not show progress bar when not loading', async () => {
    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.isLoading()).toBe(false);
  });

  it('should show progress bar when loading', async () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.isLoading()).toBe(true);
  });

  it('should show empty message when empty and not loading', async () => {
    fixture.componentRef.setInput('empty', true);
    fixture.detectChanges();

    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.getEmptyText()).toBe('No data available');
  });

  it('should not show empty message when loading', async () => {
    fixture.componentRef.setInput('empty', true);
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.getEmptyText()).toBeNull();
  });

  it('should show custom empty message', async () => {
    fixture.componentRef.setInput('empty', true);
    fixture.componentRef.setInput('emptyMessage', 'Nothing here');
    fixture.detectChanges();

    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.getEmptyText()).toBe('Nothing here');
  });

  it('should not show paginator by default', async () => {
    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.hasPaginator()).toBe(false);
  });

  it('should show paginator when paginated is true', async () => {
    fixture.componentRef.setInput('paginated', true);
    fixture.componentRef.setInput('totalElements', 100);
    fixture.detectChanges();

    const harness = await loader.getHarness(DataTableHarness);
    expect(await harness.hasPaginator()).toBe(true);
  });
});
