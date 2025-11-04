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
import { IPromise } from 'angular';

import { Identifiable, Selector } from './selector';

interface SearchParam {
  term: string;
}

interface OnSelectParam {
  selection: Identifiable[];
}

export class SearchAndSelectController {
  // bindings
  public init: () => IPromise<Identifiable[]>;
  public search: (term: SearchParam) => IPromise<Identifiable[]>;
  public onSelect: (selection: OnSelectParam) => void;
  public context: string;
  public selectModel: string[];

  // search model
  private searchTerm = '';

  // select handler
  private selector = new Selector();

  get label() {
    return this.context;
  }

  get placeholder() {
    if (this.context.toUpperCase() === this.context) {
      return `Search ${this.context}`;
    }
    return `Search ${this.context.toLowerCase()}`;
  }

  get selection(): Identifiable[] {
    return this.selector.selection;
  }

  get options(): Identifiable[] {
    return this.selector.options;
  }

  async $onInit() {
    // Normalize selectModel if it has nested arrays (fix for decodeQueryFilters bug)
    // Check if any nested arrays exist before flattening for better performance
    if (this.selectModel && Array.isArray(this.selectModel) && this.selectModel.some(Array.isArray)) {
      this.selectModel = this.flattenArray(this.selectModel);
    }

    if (this.init) {
      const options = await this.init();
      this.selector.updateOptions(options);
    }
    this.select();
  }

  /**
   * Recursively flattens nested arrays
   * Example: [[1], [2]] -> [1, 2]
   */
  private flattenArray(arr: any[]): string[] {
    const result: string[] = [];
    for (const item of arr) {
      if (Array.isArray(item)) {
        result.push(...this.flattenArray(item));
      } else if (item !== null && item !== undefined) {
        result.push(item);
      }
    }
    return result;
  }

  async onSearch() {
    const options = await this.search({ term: this.searchTerm });
    this.selector.updateOptions(options);
  }

  select() {
    // Normalize selectModel before using it (defensive check)
    // Only flatten if nested arrays are detected for better performance
    let normalizedModel = this.selectModel || [];
    if (Array.isArray(normalizedModel) && normalizedModel.some(Array.isArray)) {
      normalizedModel = this.flattenArray(normalizedModel);
    }

    this.selector.updateSelection(normalizedModel, () => this.onSearch());
    this.onSelect({ selection: this.selection });
  }

  hasSelection(): boolean {
    return this.selection.length > 0;
  }
}
