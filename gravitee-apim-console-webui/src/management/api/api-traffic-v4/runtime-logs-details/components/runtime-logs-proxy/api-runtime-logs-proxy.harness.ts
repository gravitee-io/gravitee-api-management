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
import { ComponentHarness } from '@angular/cdk/testing';

export abstract class AbstractApiRuntimeLogsProxyHarness extends ComponentHarness {
  public async getAllKeyValues(): Promise<Array<{ key: string; value: string | null }>> {
    const keyValues: Array<{ key: string; value: string | null }> = [];
    const dtElements = await this.locatorForAll('dt')();
    const ddElements = await this.locatorForAll('dd')();

    if (dtElements.length !== ddElements.length) {
      throw new Error("Mismatched number of dt and dd elements. This may indicate a problem with the component's DOM structure.");
    }

    for (let i = 0; i < dtElements.length; i++) {
      const key = await dtElements[i].text();
      const value = await ddElements[i].text();
      keyValues.push({ key: key.trim(), value: value ? value.trim() : null });
    }

    return keyValues;
  }
}
