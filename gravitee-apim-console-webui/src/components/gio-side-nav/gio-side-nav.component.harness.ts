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
import { ComponentHarness, TestElement } from '@angular/cdk/testing';

export class GioSideNavHarness extends ComponentHarness {
  public static readonly hostSelector = 'gio-side-nav';

  private readonly getAllAnchors = this.locatorForAll('a');

  async getAnchorsWithTarget(target: string): Promise<TestElement[]> {
    const result: TestElement[] = [];
    for (const anchor of await this.getAllAnchors()) {
      if ((await anchor.getAttribute('target')) === target) {
        result.push(anchor);
      }
    }
    return result;
  }

  async countAnchorsWithAnyTarget(): Promise<number> {
    let count = 0;
    for (const anchor of await this.getAllAnchors()) {
      if ((await anchor.getAttribute('target')) !== null) {
        count++;
      }
    }
    return count;
  }

  async countAnchorsWithAnyRel(): Promise<number> {
    let count = 0;
    for (const anchor of await this.getAllAnchors()) {
      if ((await anchor.getAttribute('rel')) !== null) {
        count++;
      }
    }
    return count;
  }
}
