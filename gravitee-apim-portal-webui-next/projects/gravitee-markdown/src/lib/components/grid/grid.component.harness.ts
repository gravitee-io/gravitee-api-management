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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';

import { CellComponentHarness } from './cell/cell.component.harness';

export class GridComponentHarness extends ComponentHarness {
  static hostSelector = 'gmd-grid';

  async getGridContainer(): Promise<TestElement> {
    return this.locatorFor('.grid-container')();
  }

  async getCells(): Promise<CellComponentHarness[]> {
    return this.locatorForAll(CellComponentHarness)();
  }

  async getCellCount(): Promise<number> {
    const cells = await this.getCells();
    return cells.length;
  }

  async getCellText(index: number): Promise<string> {
    const cells = await this.getCells();
    if (index >= cells.length) {
      throw new Error(`Cell index ${index} is out of bounds. Available cells: ${cells.length}`);
    }
    return (await cells[index].getContent()) ?? '';
  }

  async getGridClasses(): Promise<string> {
    const container = await this.getGridContainer();
    return (await container.getAttribute('class')) ?? '';
  }

  async hasColumns(columnCount: number): Promise<boolean> {
    const classes = await this.getGridClasses();
    return classes.includes(`grid-cols-${columnCount}`);
  }
}
