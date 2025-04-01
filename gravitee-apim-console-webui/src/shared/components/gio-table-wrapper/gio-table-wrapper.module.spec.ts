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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSortHarness } from '@angular/material/sort/testing';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { GioTableWrapperModule } from './gio-table-wrapper.module';
import { GioTableWrapperFilters } from './gio-table-wrapper.component';
import { GioTableWrapperHarness } from './gio-table-wrapper.harness';

describe('GioTableWrapperComponent', () => {
  describe('simple usage', () => {
    @Component({
      template: `
        <gio-table-wrapper [length]="length" [filters]="filters" (filtersChange)="filtersChange($event)">
          <table mat-table [dataSource]="dataSource">
            <!-- Name Column -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Name</th>
              <td mat-cell *matCellDef="let element">{{ element.name }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>

            <tr class="mat-mdc-row mdc-data-table__row" *matNoDataRow>
              <td class="mat-mdc-cell mdc-data-table__cell" [attr.colspan]="displayedColumns.length">No Data</td>
            </tr>
          </table>
        </gio-table-wrapper>
      `,
      standalone: false,
    })
    class TestComponent {
      length = 0;
      dataSource = [{ name: '🦊' }, { name: '🐙' }, { name: '🐶' }];
      displayedColumns = ['name'];
      filters: GioTableWrapperFilters;
      filtersChange = jest.fn();
    }

    let component: TestComponent;
    let fixture: ComponentFixture<TestComponent>;
    let loader: HarnessLoader;

    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponent],
        imports: [NoopAnimationsModule, GioTableWrapperModule, MatTableModule, MatSortModule, MatIconTestingModule],
      });
      fixture = TestBed.createComponent(TestComponent);
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    it('should emit default initial filtersChange', async () => {
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

      expect(component.filtersChange).toHaveBeenCalledTimes(1);
      expect(component.filtersChange).toHaveBeenCalledWith({
        pagination: {
          index: 1,
          size: 10,
        },
        searchTerm: '',
      });
      expect(await tableWrapper.getSearchValue()).toBe('');
    });

    it('should emit default initial filtersChange with initial filters value', async () => {
      component.filters = {
        pagination: {
          index: 2,
          size: 25,
        },
        searchTerm: 'fox',
      };
      component.length = 100;
      fixture.detectChanges();
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

      expect(component.filtersChange).toHaveBeenCalledTimes(1);
      expect(component.filtersChange).toHaveBeenCalledWith({
        pagination: {
          index: 2,
          size: 25,
        },
        searchTerm: 'fox',
      });
      expect(await tableWrapper.getSearchValue()).toEqual('fox');
      const paginator = await tableWrapper.getPaginator();
      expect(await paginator.getRangeLabel()).toEqual('26 – 50 of 100');
    });

    it('should emit when search term change', async () => {
      fixture.detectChanges();
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

      // initial filtersChange
      expect(component.filtersChange).toHaveBeenCalledTimes(1);
      await tableWrapper.setSearchValue('Fox');

      expect(component.filtersChange).toHaveBeenCalledTimes(3);
      expect(component.filtersChange).toHaveBeenNthCalledWith(2, {
        pagination: {
          index: 1,
          size: 10,
        },
        searchTerm: '',
      });

      expect(component.filtersChange).toHaveBeenNthCalledWith(3, {
        pagination: {
          index: 1,
          size: 10,
        },
        searchTerm: 'Fox',
      });
    });

    it('should emit when pagination change', async () => {
      component.filters = {
        pagination: {
          index: 1,
          size: 25,
        },
        searchTerm: '',
      };
      component.length = 100;
      fixture.detectChanges();
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

      // initial filtersChange
      expect(component.filtersChange).toHaveBeenCalledTimes(1);
      const paginator = await tableWrapper.getPaginator();

      await paginator.goToNextPage();
      expect(component.filtersChange).toHaveBeenCalledTimes(2);
      expect(component.filtersChange).toHaveBeenNthCalledWith(2, {
        pagination: {
          index: 2,
          size: 25,
        },
        searchTerm: '',
      });

      await paginator.setPageSize(5);
      expect(component.filtersChange).toHaveBeenCalledTimes(3);
      expect(component.filtersChange).toHaveBeenNthCalledWith(3, {
        pagination: {
          index: 6,
          size: 5,
        },
        searchTerm: '',
      });
    });

    it('should hide pagination when length < pagination size ', async () => {
      component.length = 0;
      component.filters = {
        pagination: {
          index: 1,
          size: 25,
        },
        searchTerm: '',
      };
      fixture.detectChanges();
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

      expect(await (await (await tableWrapper.getPaginator('footer')).host()).hasClass('hidden')).toBe(true);
    });

    it('should emit and reset pagination when search term change', async () => {
      component.filters = {
        pagination: {
          index: 4,
          size: 10,
        },
        searchTerm: 'fox',
      };
      component.length = 100;
      fixture.detectChanges();
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);
      const paginator = await tableWrapper.getPaginator();

      // initial filtersChange
      expect(component.filtersChange).toHaveBeenCalledTimes(1);
      await tableWrapper.setSearchValue('Fox');

      expect(component.filtersChange).toHaveBeenNthCalledWith(3, {
        pagination: {
          index: 1,
          size: 10,
        },
        searchTerm: 'Fox',
      });
      expect(await paginator.getRangeLabel()).toEqual('1 – 10 of 100');
    });
  });

  describe('with sort usage', () => {
    @Component({
      template: `
        <gio-table-wrapper [length]="length" [filters]="filters" (filtersChange)="filtersChange($event)">
          <table mat-table [dataSource]="dataSource" matSort>
            <!-- Name Column -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
              <td mat-cell *matCellDef="let element">{{ element.name }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>

            <tr class="mat-mdc-row mdc-data-table__row" *matNoDataRow>
              <td class="mat-mdc-cell mdc-data-table__cell" [attr.colspan]="displayedColumns.length">No Data</td>
            </tr>
          </table>
        </gio-table-wrapper>
      `,
      standalone: false,
    })
    class TestComponentWithSort {
      length = 100;
      dataSource = [{ name: '🦊' }, { name: '🐙' }, { name: '🐶' }];
      displayedColumns = ['name'];
      filters: GioTableWrapperFilters;
      filtersChange = jest.fn();
    }

    let component: TestComponentWithSort;
    let fixture: ComponentFixture<TestComponentWithSort>;
    let loader: HarnessLoader;

    beforeEach(() => {
      TestBed.configureTestingModule({
        declarations: [TestComponentWithSort],
        imports: [NoopAnimationsModule, GioTableWrapperModule, MatTableModule, MatSortModule, MatSortModule, MatIconTestingModule],
      });
      fixture = TestBed.createComponent(TestComponentWithSort);
      component = fixture.componentInstance;
      loader = TestbedHarnessEnvironment.loader(fixture);
    });

    afterEach(() => {
      jest.clearAllMocks();
    });

    it('should emit default initial filtersChange', async () => {
      const matSort = await loader.getHarness(MatSortHarness);
      const tableWrapper = await loader.getHarness(GioTableWrapperHarness);

      component.filters = {
        pagination: {
          index: 2,
          size: 25,
        },
        searchTerm: 'fox',
        sort: {
          active: 'name',
          direction: 'asc',
        },
      };
      fixture.detectChanges();

      expect(await (await matSort.getActiveHeader()).getLabel()).toEqual('Name');

      expect(component.filtersChange).toHaveBeenNthCalledWith(2, {
        pagination: { index: 2, size: 25 },
        searchTerm: 'fox',
        sort: {
          active: 'name',
          direction: 'asc',
        },
      });
      expect(await tableWrapper.getSearchValue()).toEqual('fox');
      const paginator = await tableWrapper.getPaginator();
      expect(await paginator.getRangeLabel()).toEqual('26 – 50 of 100');

      expect(component.filtersChange).toHaveBeenCalledTimes(2);
    });

    it('should emit when sort change', async () => {
      fixture.detectChanges();
      const matSort = await loader.getHarness(MatSortHarness);
      const sortHeader = await matSort.getSortHeaders();
      await sortHeader[0].click();

      // initial filtersChange + sort change
      expect(component.filtersChange).toHaveBeenCalledTimes(2);

      expect(component.filtersChange).toHaveBeenNthCalledWith(2, {
        pagination: {
          index: 1,
          size: 10,
        },
        searchTerm: '',
        sort: {
          active: 'name',
          direction: 'asc',
        },
      });

      await sortHeader[0].click();
      expect(component.filtersChange).toHaveBeenNthCalledWith(3, {
        pagination: {
          index: 1,
          size: 10,
        },
        searchTerm: '',
        sort: {
          active: 'name',
          direction: 'desc',
        },
      });
    });
  });
});
