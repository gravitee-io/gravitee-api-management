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

import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { action } from 'storybook/actions';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';

import { GioTableWrapperComponent, GioTableWrapperFilters } from './gio-table-wrapper.component';
import { GioTableWrapperModule } from './gio-table-wrapper.module';
import { gioTableFilterCollection } from './gio-table-wrapper.util';

export interface PeriodicElement {
  name: string;
  position: number;
  weight: number;
  symbol: string;
}

const ELEMENT_DATA: PeriodicElement[] = [
  { position: 1, name: 'Hydrogen', weight: 1.0079, symbol: 'H' },
  { position: 2, name: 'Helium', weight: 4.0026, symbol: 'He' },
  { position: 3, name: 'Lithium', weight: 6.941, symbol: 'Li' },
  { position: 4, name: 'Beryllium', weight: 9.0122, symbol: 'Be' },
  { position: 5, name: 'Boron', weight: 10.811, symbol: 'B' },
  { position: 6, name: 'Carbon', weight: 12.0107, symbol: 'C' },
  { position: 7, name: 'Nitrogen', weight: 14.0067, symbol: 'N' },
  { position: 8, name: 'Oxygen', weight: 15.9994, symbol: 'O' },
  { position: 9, name: 'Fluorine', weight: 18.9984, symbol: 'F' },
  { position: 10, name: 'Neon', weight: 20.1797, symbol: 'Ne' },
];

export default {
  title: 'Shared / Table wrapper',
  component: GioTableWrapperComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, GioTableWrapperModule, MatTableModule, MatSortModule, MatCardModule],
    }),
  ],
} as Meta;

export const Default: StoryObj = {
  render: args => ({
    template: `
    <div *ngIf="!insideMatCard">
        <ng-container *ngTemplateOutlet="contentTemplate;"></ng-container>
    </div>

    <mat-card *ngIf="insideMatCard">
        <ng-container *ngTemplateOutlet="contentTemplate;"></ng-container>
    </mat-card>

    <ng-template #contentTemplate>
      <h2>The title</h2>
      <p>Some content bla bla bla ...</p>
      <gio-table-wrapper [length]="filterDataSource(_filters).unpaginatedLength" [filters]="filters" (filtersChange)="filtersChange($event); _filters = $event" >
        <table
          mat-table
          [dataSource]="filterDataSource(_filters).filteredCollection"
          matSort
        >
          <!-- Position Column -->
          <ng-container matColumnDef="position">
            <th mat-header-cell *matHeaderCellDef mat-sort-header> No. </th>
            <td mat-cell *matCellDef="let element"> {{element.position}} </td>
          </ng-container>

          <!-- Name Column -->
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef mat-sort-header> Name </th>
            <td mat-cell *matCellDef="let element"> {{element.name}} </td>
          </ng-container>

          <!-- Weight Column -->
          <ng-container matColumnDef="weight">
            <th mat-header-cell *matHeaderCellDef mat-sort-header> Weight </th>
            <td mat-cell *matCellDef="let element"> {{element.weight}} </td>
          </ng-container>

          <!-- Symbol Column -->
          <ng-container matColumnDef="symbol">
            <th mat-header-cell *matHeaderCellDef mat-sort-header> Symbol </th>
            <td mat-cell *matCellDef="let element"> {{element.symbol}} </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>

          <tr class="mat-mdc-row mdc-data-table__row" *matNoDataRow>
            <td class="mat-mdc-cell mdc-data-table__cell" [attr.colspan]="displayedColumns.length">No Data</td>
          </tr>
        </table>

      </gio-table-wrapper>
    </ng-template>
    `,
    props: {
      insideMatCard: args.insideMatCard,
      length: args.length,
      filters: {
        ...(args.filterSearchTerm ? { searchTerm: args.filterSearchTerm } : {}),
        ...(args.filterPagination ? { pagination: args.filterPagination } : {}),
        ...(args.filterSort ? { sort: args.filterSort } : {}),
      },
      displayedColumns: ['position', 'name', 'weight', 'symbol'],
      // dumb function to simulate filters
      filterDataSource(filter: GioTableWrapperFilters) {
        return gioTableFilterCollection(ELEMENT_DATA, filter);
      },
      filtersChange: action('filtersChange'),
    },
  }),
  argTypes: {
    insideMatCard: {
      control: {
        type: 'boolean',
      },
      defaultValue: true,
    },
    filterSearchTerm: {
      control: {
        type: 'text',
        default: '',
      },
    },
    filterPagination: {
      control: {
        type: 'object',
      },
      defaultValue: {
        index: 1,
        size: 10,
      },
    },
    length: {
      control: {
        type: 'number',
      },
      defaultValue: ELEMENT_DATA.length,
    },
    filterSort: {
      control: {
        type: 'object',
      },
      defaultValue: {
        active: 'position',
        direction: 'asc',
      },
    },
  },
};
