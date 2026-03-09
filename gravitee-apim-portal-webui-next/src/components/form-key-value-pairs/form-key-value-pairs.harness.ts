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

import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class FormKeyValuePairsHarness extends ComponentHarness {
  public static readonly hostSelector = 'app-form-key-value-pairs';

  /**
   * Get the number of rows in the form
   */
  async getRowCount(): Promise<number> {
    const rows = await this.locatorForAll('tbody tr')();
    return rows.length;
  }

  /**
   * Get the key input field for a specific row
   */
  async getKeyField(rowIndex: number): Promise<MatInputHarness> {
    return this.locatorFor(MatInputHarness.with({ selector: `#key-value-key-${rowIndex}` }))();
  }

  /**
   * Get the value input field for a specific row
   */
  async getValueField(rowIndex: number): Promise<MatInputHarness> {
    return this.locatorFor(MatInputHarness.with({ selector: `#key-value-value-${rowIndex}` }))();
  }

  /**
   * Get the current key value for a specific row
   */
  async getKeyValue(rowIndex: number): Promise<string> {
    const keyField = await this.getKeyField(rowIndex);
    return keyField.getValue();
  }

  /**
   * Get the current value for a specific row
   */
  async getValue(rowIndex: number): Promise<string> {
    const valueField = await this.getValueField(rowIndex);
    return valueField.getValue();
  }

  /**
   * Get the key-value pair for a specific row
   */
  async getKeyValuePair(rowIndex: number): Promise<{ key: string; value: string }> {
    const [key, value] = await Promise.all([this.getKeyValue(rowIndex), this.getValue(rowIndex)]);
    return { key, value };
  }

  /**
   * Set the key value for a specific row
   */
  async setKeyValue(rowIndex: number, key: string): Promise<void> {
    const keyField = await this.getKeyField(rowIndex);
    await keyField.setValue(key);
    await keyField.blur();
  }

  /**
   * Set the value for a specific row
   */
  async setValue(rowIndex: number, value: string): Promise<void> {
    const valueField = await this.getValueField(rowIndex);
    await valueField.setValue(value);
    await valueField.blur();
  }

  /**
   * Set both key and value for a specific row
   */
  async setKeyValuePair(rowIndex: number, key: string, value: string): Promise<void> {
    // Set sequentially to match user behavior and ensure proper change detection
    await this.setKeyValue(rowIndex, key);
    await this.setValue(rowIndex, value);
  }

  /**
   * Get the delete button for a specific row (if it exists)
   */
  async getDeleteButton(rowIndex: number): Promise<MatButtonHarness | null> {
    const selector = `tbody tr:nth-child(${rowIndex + 1}) button[aria-label="Delete"]`;
    return this.locatorForOptional(MatButtonHarness.with({ selector }))();
  }

  /**
   * Check if the delete button is visible for a specific row
   */
  async isDeleteButtonVisible(rowIndex: number): Promise<boolean> {
    const deleteButton = await this.getDeleteButton(rowIndex);
    return deleteButton !== null;
  }

  /**
   * Click the delete button for a specific row
   */
  async clickDelete(rowIndex: number): Promise<void> {
    const deleteButton = await this.getDeleteButton(rowIndex);
    if (!deleteButton) {
      throw new Error(`Delete button not found for row ${rowIndex}`);
    }
    await deleteButton.click();
  }

  /**
   * Check if the form is disabled
   */
  async isFormDisabled(): Promise<boolean> {
    const keyField = await this.getKeyField(0);
    return keyField.isDisabled();
  }

  /**
   * Get all key-value pairs as a Record<string, string>
   * This filters out empty pairs (where both key and value are empty)
   */
  async getAllKeyValuePairs(): Promise<Record<string, string>> {
    const rowCount = await this.getRowCount();
    const pairs: Record<string, string> = {};

    for (let i = 0; i < rowCount; i++) {
      const { key, value } = await this.getKeyValuePair(i);
      const trimmedKey = key.trim();
      const trimmedValue = value.trim();

      if (trimmedKey) {
        pairs[trimmedKey] = trimmedValue;
      }
    }

    return pairs;
  }

  /**
   * Wait for the row count to reach a specific value
   */
  async waitForRowCount(expectedCount: number, timeout = 5000): Promise<void> {
    const startTime = Date.now();
    while (Date.now() - startTime < timeout) {
      const currentCount = await this.getRowCount();
      if (currentCount === expectedCount) {
        return;
      }
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    throw new Error(`Expected row count ${expectedCount} not reached within ${timeout}ms`);
  }
}
