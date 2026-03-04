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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { PaginatedTableComponent, TableActionEvent, TableColumn } from './paginated-table.component';

interface TestRow {
  id: string;
  name: string;
  created_at: string;
}

describe('PaginatedTableComponent', () => {
  let fixture: ComponentFixture<PaginatedTableComponent<TestRow>>;
  let component: PaginatedTableComponent<TestRow>;

  const textColumns: TableColumn[] = [
    { id: 'name', label: 'Name' },
    { id: 'created_at', label: 'Created', type: 'date' },
  ];

  const actionsColumn: TableColumn = {
    id: 'actions',
    label: 'Actions',
    type: 'actions',
    actions: [
      { id: 'edit', icon: 'edit', label: 'Edit' },
      { id: 'delete', icon: 'delete', label: 'Delete' },
    ],
  };

  const rows: TestRow[] = [
    { id: 'row-1', name: 'Alice', created_at: '2025-01-15T10:00:00Z' },
    { id: 'row-2', name: 'Bob', created_at: '2025-02-20T12:00:00Z' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginatedTableComponent],
      providers: [provideNoopAnimations(), provideRouter([])],
    }).compileComponents();
  });

  function setup(columns: TableColumn[] = textColumns): void {
    fixture = TestBed.createComponent(PaginatedTableComponent<TestRow>);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('columns', columns);
    fixture.componentRef.setInput('rows', rows);
    fixture.componentRef.setInput('totalElements', rows.length);
    fixture.componentRef.setInput('currentPage', 1);
    fixture.componentRef.setInput('pageSize', 10);
    fixture.detectChanges();
  }

  describe('text and date columns', () => {
    beforeEach(() => setup());

    it('should render table with rows', () => {
      const el: HTMLElement = fixture.nativeElement;
      const tableRows = el.querySelectorAll('tr.paginated-table__row');
      expect(tableRows.length).toBe(2);
    });

    it('should render text column values', () => {
      const el: HTMLElement = fixture.nativeElement;
      const cells = el.querySelectorAll('td.mat-mdc-cell');
      const textContent = Array.from(cells).map(c => c.textContent?.trim());
      expect(textContent).toContain('Alice');
      expect(textContent).toContain('Bob');
    });

    it('should render date column with formatted value', () => {
      const el: HTMLElement = fixture.nativeElement;
      const cells = el.querySelectorAll('td.mat-mdc-cell');
      const textContent = Array.from(cells).map(c => c.textContent?.trim());
      expect(textContent).toContain('2025-01-15');
    });

    it('should not render action buttons', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('[data-testid="action-edit"]')).toBeFalsy();
    });
  });

  describe('actions column', () => {
    beforeEach(() => setup([...textColumns, actionsColumn]));

    it('should render action buttons for each row', () => {
      const el: HTMLElement = fixture.nativeElement;
      const editButtons = el.querySelectorAll('[data-testid="action-edit"]');
      const deleteButtons = el.querySelectorAll('[data-testid="action-delete"]');
      expect(editButtons.length).toBe(2);
      expect(deleteButtons.length).toBe(2);
    });

    it('should have accessible labels on action buttons', () => {
      const el: HTMLElement = fixture.nativeElement;
      const editBtn = el.querySelector('[data-testid="action-edit"]');
      expect(editBtn?.getAttribute('aria-label')).toBe('Edit');
    });

    it('should emit actionClick on button click', () => {
      const spy = jest.fn();
      component.actionClick.subscribe(spy);

      const el: HTMLElement = fixture.nativeElement;
      const editBtn = el.querySelector<HTMLButtonElement>('[data-testid="action-edit"]')!;
      editBtn.click();

      expect(spy).toHaveBeenCalledTimes(1);
      const event: TableActionEvent<TestRow> = spy.mock.calls[0][0];
      expect(event.actionId).toBe('edit');
      expect(event.row).toMatchObject({ id: 'row-1', name: 'Alice' });
    });

    it('should stop propagation on action click', () => {
      const el: HTMLElement = fixture.nativeElement;
      const editBtn = el.querySelector<HTMLButtonElement>('[data-testid="action-edit"]')!;

      const clickEvent = new MouseEvent('click', { bubbles: true });
      const stopSpy = jest.spyOn(clickEvent, 'stopPropagation');

      editBtn.dispatchEvent(clickEvent);
      expect(stopSpy).toHaveBeenCalled();
    });

    it('should not affect text/date column rendering', () => {
      const el: HTMLElement = fixture.nativeElement;
      const cells = el.querySelectorAll('td.mat-mdc-cell');
      const textContent = Array.from(cells).map(c => c.textContent?.trim());
      expect(textContent).toContain('Alice');
      expect(textContent).toContain('2025-01-15');
    });
  });

  describe('pagination', () => {
    beforeEach(() => setup());

    it('should emit pageChange', () => {
      const spy = jest.fn();
      component.pageChange.subscribe(spy);
      component.onPageChange(2);
      expect(spy).toHaveBeenCalledWith(2);
    });
  });

  describe('rowLink and showExpandColumn disabled', () => {
    beforeEach(() => {
      setup(textColumns);
      fixture.componentRef.setInput('rowLink', false);
      fixture.componentRef.setInput('showExpandColumn', false);
      fixture.detectChanges();
    });

    it('should not render expand column', () => {
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('.paginated-table__column-expand')).toBeFalsy();
    });

    it('should not add clickable class to row', () => {
      const el: HTMLElement = fixture.nativeElement;
      const row = el.querySelector('tr.paginated-table__row');
      expect(row?.classList.contains('paginated-table__row--clickable')).toBe(false);
    });
  });
});
