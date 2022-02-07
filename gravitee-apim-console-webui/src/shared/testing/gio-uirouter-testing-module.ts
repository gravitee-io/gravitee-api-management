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
import { CUSTOM_ELEMENTS_SCHEMA, Directive, Input, NgModule } from '@angular/core';

import { UIRouterState } from '../../ajs-upgraded-providers';

@Directive({
  selector: '[uiSrefStatus]',
  exportAs: 'uiSrefStatus',
})
export class UisRefStatusMockDirective {
  @Input()
  public uiParams: any;

  @Input()
  public uiSref: string;

  public status = {
    active: true,
  };
}

@NgModule({
  declarations: [UisRefStatusMockDirective],
  exports: [UisRefStatusMockDirective],
  providers: [
    {
      provide: UIRouterState,
      useValue: {
        reload: () => ({}),
      },
    },
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class GioUiRouterTestingModule {}
