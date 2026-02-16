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
import { ComponentHarness, parallel } from '@angular/cdk/testing';

export class ExposedEntrypointsHarness extends ComponentHarness {
  static readonly hostSelector = 'exposed-entrypoints';

  async getValues(): Promise<string[]> {
    const elements = await this.locatorForAll('.exposed-entrypoints__list__item')();
    const values = await parallel(() => elements.map(element => element.text()));

    return values.map(v => v.replace(' content_copy', ''));
  }

  async isEmpty(): Promise<boolean> {
    const elements = await this.locatorForAll('.exposed-entrypoints__list__item')();
    return elements.length === 0;
  }
}
