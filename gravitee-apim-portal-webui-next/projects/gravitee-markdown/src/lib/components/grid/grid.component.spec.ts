/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CellComponent } from './cell/cell.component';
import { GridComponent } from './grid.component';
import { GridComponentHarness } from './grid.component.harness';

interface GridTestInput {
  columns: number;
  items: string[];
}

@Component({
  selector: 'gmd-grid-test',
  standalone: true,
  imports: [CommonModule, GridComponent, CellComponent],
  template: `
    <gmd-grid [columns]="columns">
      @for (item of items; track item) {
        <gmd-cell>{{ item }}</gmd-cell>
      }
    </gmd-grid>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
})
class GridTestComponent {
  @Input() columns = 1;
  @Input() items: string[] = [];
}

describe('GridComponent', () => {
  let component: GridTestComponent;
  let fixture: ComponentFixture<GridTestComponent>;
  let harness: GridComponentHarness;

  async function init(input: GridTestInput): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [GridTestComponent, GridComponent, CellComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(GridTestComponent);
    component = fixture.componentInstance;

    // Set the input values
    component.columns = input.columns;
    component.items = input.items;

    fixture.detectChanges();

    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, GridComponentHarness);
  }

  it('should create', async () => {
    await init({ columns: 1, items: [] });
    expect(component).toBeTruthy();
  });

  it('should display cells when provided', async () => {
    const testItems = ['First cell content', 'Second cell content'];
    await init({ columns: 2, items: testItems });

    const cellCount = await harness.getCellCount();
    expect(cellCount).toBe(2);

    const firstCellText = await harness.getCellText(0);
    expect(firstCellText).toContain('First cell content');

    const secondCellText = await harness.getCellText(1);
    expect(secondCellText).toContain('Second cell content');
  });

  it('should have correct column configuration', async () => {
    await init({ columns: 3, items: ['Item 1', 'Item 2', 'Item 3'] });

    const hasThreeColumns = await harness.hasColumns(3);
    expect(hasThreeColumns).toBe(true);
  });

  it('should render grid container with proper styling', async () => {
    await init({ columns: 2, items: ['Test item'] });

    const container = await harness.getGridContainer();
    expect(container).toBeTruthy();

    const displaysGridStyling = await container.hasClass('grid-cols-2');
    expect(displaysGridStyling).toBeTruthy();
  });

  it('should handle multiple items with different column counts', async () => {
    const testItems = ['Item 1', 'Item 2', 'Item 3', 'Item 4'];
    await init({ columns: 4, items: testItems });

    const cellCount = await harness.getCellCount();
    expect(cellCount).toBe(4);

    for (let i = 0; i < testItems.length; i++) {
      const cellText = await harness.getCellText(i);
      expect(cellText).toContain(testItems[i]);
    }
  });

  it('should handle empty items array', async () => {
    await init({ columns: 2, items: [] });

    const cellCount = await harness.getCellCount();
    expect(cellCount).toBe(0);
  });

  it('should limit column input to 6 range', async () => {
    await init({ columns: 10, items: ['Item 1', 'Item 2'] });
    expect(await harness.hasColumns(6)).toBe(true);
  });
});
