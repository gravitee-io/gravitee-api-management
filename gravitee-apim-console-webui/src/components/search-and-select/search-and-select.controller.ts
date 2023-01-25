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
    if (this.init) {
      const options = await this.init();
      this.selector.updateOptions(options);
    }
    this.select();
  }

  async onSearch() {
    const options = await this.search({ term: this.searchTerm });
    this.selector.updateOptions(options);
  }

  select() {
    this.selector.updateSelection(this.selectModel, () => this.onSearch());
    this.onSelect({ selection: this.selection });
  }

  hasSelection(): boolean {
    return this.selection.length > 0;
  }
}
