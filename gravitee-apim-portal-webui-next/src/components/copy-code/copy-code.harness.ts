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
import { BaseHarnessFilters, ContentContainerComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class CopyCodeHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-copy-code';
  protected locateCodeText = this.locatorFor('.copy-code__command-line__code');
  protected locateVisibilityBtn = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Hide password"]' }));

  public static with(options: BaseHarnessFilters): HarnessPredicate<CopyCodeHarness> {
    return new HarnessPredicate(CopyCodeHarness, options);
  }

  public async changePasswordVisibility(): Promise<void> {
    return await this.locateVisibilityBtn().then(btn => btn.click());
  }

  public async getText(): Promise<string> {
    return await this.locateCodeText().then(div => div.text());
  }
}
