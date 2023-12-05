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
export interface Identifiable {
  id: string;
  name: string;
}

function compareNames<T extends Identifiable>(t1, t2: T): number {
  if (t1.name === t2.name) {
    return 0;
  }
  return t1.name < t2.name ? -1 : 1;
}

export class Selector<T extends Identifiable> {
  private idToOption: Map<string, T> = new Map();
  private idToSelection: Map<string, T> = new Map();

  private cache: Map<string, T> = new Map();

  private lastSelectKeys = new Set<string>();

  get options() {
    return Array.from(this.idToOption.values()).sort(compareNames);
  }

  get selection() {
    return Array.from(this.idToSelection.values()).sort(compareNames);
  }

  /**
   * update the current selection
   *
   * @param ids a list of IDs representing the new state of the selection
   * @param unSelectCallBack used to restore options on unselect
   */
  updateSelection(ids: string[] = [], unSelectCallBack: () => void) {
    if (this.isSelect(ids)) {
      this.select(ids);
    } else {
      this.unSelect(ids, unSelectCallBack);
    }
    this.lastSelectKeys = new Set(ids);
  }

  private select(ids: string[]) {
    for (const id of ids) {
      this.idToOption.delete(id);
      if (this.cache.has(id)) {
        this.idToSelection.set(id, this.cache.get(id));
      }
    }
  }

  private unSelect(ids: string[], callback: () => void) {
    const unselectedIds = this.getUnselectedIds(ids);

    for (const id of unselectedIds) {
      this.idToSelection.delete(id);
      this.idToOption.set(id, this.cache.get(id));
    }

    callback();
  }

  private getUnselectedIds(ids: string[]): Set<string> {
    const diff = new Set(this.lastSelectKeys);

    for (const id of ids) {
      diff.delete(id);
    }

    return diff;
  }

  private isSelect(ids: string[]): boolean {
    return this.lastSelectKeys.size < ids.length;
  }

  /**
   * update the current options from a list of entities
   *
   * @param data the list of options available for selection
   */
  updateOptions(data: T[] = []) {
    this.idToOption.clear();

    for (const t of data) {
      if (!this.lastSelectKeys.has(t.id)) {
        this.idToOption.set(t.id, t);
      }
    }

    this.updateCache(data);
  }

  private updateCache(data: T[]) {
    for (const t of data) {
      this.cache.set(t.id, t);
    }
  }
}
