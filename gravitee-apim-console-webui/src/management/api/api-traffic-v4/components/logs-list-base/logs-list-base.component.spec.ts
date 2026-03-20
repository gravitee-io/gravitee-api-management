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
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { LogsListBaseComponent, LogsListColumnDef } from './logs-list-base.component';

import { Constants } from '../../../../../entities/Constants';
import { CONSTANTS_TESTING } from '../../../../../shared/testing';

describe('LogsListBaseComponent', () => {
  let fixture: ComponentFixture<LogsListBaseComponent>;
  let component: LogsListBaseComponent;

  const STORAGE_KEY = 'env-logs';
  const ENV_ID = CONSTANTS_TESTING.org.currentEnv.id;
  const STORAGE_ID = `${ENV_ID}-${STORAGE_KEY}-logs-list-visible-columns`;

  const defaultColumns: LogsListColumnDef[] = [
    { id: 'timestamp', label: 'Timestamp' },
    { id: 'method', label: 'Method' },
    { id: 'status', label: 'Status' },
    { id: 'api', label: 'API' },
  ];

  const defaultPagination = { page: 1, perPage: 10, totalCount: 3 };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [LogsListBaseComponent, NoopAnimationsModule, MatIconTestingModule],
      providers: [provideHttpClient(), { provide: Constants, useValue: CONSTANTS_TESTING }],
    }).compileComponents();

    fixture = TestBed.createComponent(LogsListBaseComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('without storageKey (no column picker)', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('logs', []);
      fixture.componentRef.setInput('pagination', defaultPagination);
      fixture.componentRef.setInput('columns', defaultColumns);
      fixture.detectChanges();
    });

    it('should_have_column_picker_disabled', () => {
      expect(component.hasColumnPicker()).toBe(false);
    });

    it('should_return_all_column_ids_without_actions', () => {
      expect(component.displayedColumns()).toEqual(['timestamp', 'method', 'status', 'api']);
    });
  });

  describe('with storageKey (column picker enabled)', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('logs', []);
      fixture.componentRef.setInput('pagination', defaultPagination);
      fixture.componentRef.setInput('columns', defaultColumns);
      fixture.componentRef.setInput('storageKey', STORAGE_KEY);
      fixture.detectChanges();
    });

    it('should_have_column_picker_enabled', () => {
      expect(component.hasColumnPicker()).toBe(true);
    });

    it('should_display_all_columns_plus_actions_when_no_stored_data', () => {
      expect(component.displayedColumns()).toEqual(['timestamp', 'method', 'status', 'api', 'actions']);
    });

    it('should_initialize_all_option_checkboxes_to_true', () => {
      expect(component.displayedColumnsOption).toEqual({
        timestamp: true,
        method: true,
        status: true,
        api: true,
      });
    });
  });

  describe('updateVisibleColumns', () => {
    beforeEach(() => {
      fixture.componentRef.setInput('logs', []);
      fixture.componentRef.setInput('pagination', defaultPagination);
      fixture.componentRef.setInput('columns', defaultColumns);
      fixture.componentRef.setInput('storageKey', STORAGE_KEY);
      fixture.detectChanges();
    });

    it('should_update_displayed_columns_and_write_to_local_storage', () => {
      component.displayedColumnsOption['method'] = false;
      component.displayedColumnsOption['status'] = false;
      component.updateVisibleColumns();

      expect(component.displayedColumns()).toEqual(['timestamp', 'api', 'actions']);

      const stored = JSON.parse(localStorage.getItem(STORAGE_ID)!);
      expect(stored).toEqual({
        timestamp: true,
        method: false,
        status: false,
        api: true,
      });
    });

    it('should_prevent_deselecting_all_columns', () => {
      component.displayedColumnsOption['timestamp'] = false;
      component.displayedColumnsOption['method'] = false;
      component.displayedColumnsOption['status'] = false;
      component.displayedColumnsOption['api'] = false;
      component.updateVisibleColumns();

      // Should keep the previous selection and not write to localStorage
      expect(component.displayedColumns()).toEqual(['timestamp', 'method', 'status', 'api', 'actions']);
      expect(localStorage.getItem(STORAGE_ID)).toBeNull();
    });

    it('should_discard_unapplied_toggles_on_reset', () => {
      // Simulate user toggling checkboxes without clicking Apply
      component.displayedColumnsOption['method'] = false;
      component.displayedColumnsOption['status'] = false;

      // Simulate menu reopen (calls resetColumnOptions)
      component.resetColumnOptions();

      // Should restore to the applied state (all columns visible)
      expect(component.displayedColumnsOption).toEqual({
        timestamp: true,
        method: true,
        status: true,
        api: true,
      });
    });

    it('should_discard_unapplied_toggles_after_partial_apply', () => {
      // Apply with method hidden
      component.displayedColumnsOption['method'] = false;
      component.updateVisibleColumns();
      expect(component.displayedColumns()).toEqual(['timestamp', 'status', 'api', 'actions']);

      // Now toggle more without applying
      component.displayedColumnsOption['status'] = false;

      // Reset — should go back to the applied state (method hidden, everything else visible)
      component.resetColumnOptions();
      expect(component.displayedColumnsOption).toEqual({
        timestamp: true,
        method: false,
        status: true,
        api: true,
      });
    });
  });

  describe('localStorage restoration', () => {
    it('should_restore_columns_from_local_storage', () => {
      localStorage.setItem(STORAGE_ID, JSON.stringify({ timestamp: true, method: false, status: true, api: false }));

      fixture.componentRef.setInput('logs', []);
      fixture.componentRef.setInput('pagination', defaultPagination);
      fixture.componentRef.setInput('columns', defaultColumns);
      fixture.componentRef.setInput('storageKey', STORAGE_KEY);
      fixture.detectChanges();

      expect(component.displayedColumns()).toEqual(['timestamp', 'status', 'actions']);
      expect(component.displayedColumnsOption).toEqual({ timestamp: true, method: false, status: true, api: false });
    });

    it('should_recover_from_corrupted_local_storage_data', () => {
      localStorage.setItem(STORAGE_ID, 'not-valid-json!!!');

      fixture.componentRef.setInput('logs', []);
      fixture.componentRef.setInput('pagination', defaultPagination);
      fixture.componentRef.setInput('columns', defaultColumns);
      fixture.componentRef.setInput('storageKey', STORAGE_KEY);
      fixture.detectChanges();

      // Should fall back to all columns and clear the bad entry
      expect(component.displayedColumns()).toEqual(['timestamp', 'method', 'status', 'api', 'actions']);
      expect(localStorage.getItem(STORAGE_ID)).toBeNull();
    });
  });

  describe('column reconciliation', () => {
    it('should_add_new_columns_as_visible_when_not_in_stored_data', () => {
      // Stored data only has 2 of the 4 current columns
      localStorage.setItem(STORAGE_ID, JSON.stringify({ timestamp: true, method: false }));

      fixture.componentRef.setInput('logs', []);
      fixture.componentRef.setInput('pagination', defaultPagination);
      fixture.componentRef.setInput('columns', defaultColumns);
      fixture.componentRef.setInput('storageKey', STORAGE_KEY);
      fixture.detectChanges();

      // 'status' and 'api' are new — should default to visible (true)
      expect(component.displayedColumnsOption).toEqual({ timestamp: true, method: false, status: true, api: true });
      expect(component.displayedColumns()).toEqual(['timestamp', 'status', 'api', 'actions']);
    });

    it('should_drop_stale_keys_not_in_current_columns', () => {
      // Stored data has a column that no longer exists
      localStorage.setItem(STORAGE_ID, JSON.stringify({ timestamp: true, method: true, removedColumn: true }));

      fixture.componentRef.setInput('logs', []);
      fixture.componentRef.setInput('pagination', defaultPagination);
      fixture.componentRef.setInput('columns', defaultColumns);
      fixture.componentRef.setInput('storageKey', STORAGE_KEY);
      fixture.detectChanges();

      expect(component.displayedColumnsOption).not.toHaveProperty('removedColumn');
      expect(component.displayedColumns()).not.toContain('removedColumn');
      // New columns (status, api) should be visible
      expect(component.displayedColumns()).toEqual(['timestamp', 'method', 'status', 'api', 'actions']);
    });
  });
});
