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
import { MatButtonHarness } from '@angular/material/button/testing';

import { GioEntrypointsSelectionListHarness } from '../../component/gio-entrypoints-selection-list/gio-entrypoints-selection-list.harness';
import { GioFormListenersContextPathHarness } from '../../component/gio-form-listeners/gio-form-listeners-context-path/gio-form-listeners-context-path.harness';

export class ApiEntrypointsV4AddDialogHarness extends ComponentHarness {
  public static hostSelector = 'api-entrypoints-v4-add-dialog';

  private entrypointListLocator = this.locatorFor(GioEntrypointsSelectionListHarness);
  private contextPathFormLocator = this.locatorFor(GioFormListenersContextPathHarness);
  private saveButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Add API entrypoints' }));
  private saveWithContextPathButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Validate my entrypoints' }));
  private cancelButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  public getEntrypointSelectionList() {
    return this.entrypointListLocator().then(l => l.getSelectionList());
  }

  public getContextPathForm() {
    return this.contextPathFormLocator();
  }
  public getSaveButton() {
    return this.saveButtonLocator();
  }
  public getSaveWithContextPathButton() {
    return this.saveWithContextPathButtonLocator();
  }
  public getCancelButton() {
    return this.cancelButtonLocator();
  }
}
