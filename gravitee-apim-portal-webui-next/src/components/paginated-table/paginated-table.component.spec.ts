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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { PaginatedTableComponent, TableAction, TableColumn } from './paginated-table.component';
import { PaginatedTableHarness } from './paginated-table.harness';

type TestRow = {
  id: string;
  name: string;
  createdAt: string;
};

@Component({
  standalone: true,
  imports: [PaginatedTableComponent],
  template: `
    <app-paginated-table
      [columns]="columns"
      [rows]="rows"
      [totalElements]="totalElements"
      [currentPage]="currentPage"
      [pageSize]="pageSize"
      [navigable]="navigable"
      [actions]="actions"
      (actionClick)="onActionClick($event)"
    />
  `,
})
class TestHostComponent {
  columns: TableColumn[] = [
    { id: 'name', label: 'Name' },
    { id: 'createdAt', label: 'Created', type: 'date' },
  ];
  rows: TestRow[] = [{ id: 'row-1', name: 'First row', createdAt: '2026-01-01T00:00:00Z' }];
  totalElements = 1;
  currentPage = 1;
  pageSize = 10;
  navigable = true;
  actions: TableAction<TestRow>[] = [];
  receivedAction: { actionId: string; row: TestRow } | null = null;

  onActionClick(event: { actionId: string; row: TestRow }): void {
    this.receivedAction = event;
  }
}

describe('PaginatedTableComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let host: TestHostComponent;
  let harness: PaginatedTableHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideNoopAnimations(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    host = fixture.componentInstance;
  });

  it('should render navigable rows with expand column by default', async () => {
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PaginatedTableHarness);

    expect((await harness.getNavigableRows()).length).toBeGreaterThan(0);
    expect((await harness.getExpandColumns()).length).toBeGreaterThan(0);
    expect((await harness.getActionButtons()).length).toBe(0);
  });

  it('should render action buttons and disable navigation when configured', async () => {
    host.navigable = false;
    host.actions = [
      {
        id: 'delete',
        icon: 'delete',
        ariaLabel: 'Delete row',
        color: 'warn',
      },
    ];

    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PaginatedTableHarness);

    expect((await harness.getNavigableRows()).length).toBe(0);
    expect((await harness.getExpandColumns()).length).toBe(0);

    const deleteButton = await harness.getActionButton('delete');
    expect(deleteButton).toBeTruthy();

    await deleteButton!.click();

    expect(host.receivedAction).toEqual({
      actionId: 'delete',
      row: host.rows[0],
    });
  });
});
