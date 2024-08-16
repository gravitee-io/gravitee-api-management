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
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { GioTableWrapperHarness } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.harness';

export class SharedPolicyGroupHistoryHarness extends ComponentHarness {
  static readonly hostSelector = 'shared-policy-group-history';
  public getTable = this.locatorFor(MatTableHarness.with({ selector: '[aria-label="Shared Policy Group"]' }));
  public getTableWrapper = this.locatorFor(GioTableWrapperHarness);
  public compareTwoSPGButton = this.locatorForOptional(MatButtonHarness.with({ text: /compareTwoSPG/ }));
}
