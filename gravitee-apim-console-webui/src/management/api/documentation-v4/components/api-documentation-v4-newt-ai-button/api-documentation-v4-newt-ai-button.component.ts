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
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { PageType } from '../../../../../entities/management-api-v2';
import { ApiSpecGenState } from '../../../../../services-ngx/api-spec-gen.service';

@Component({
  selector: 'api-documentation-v4-newt-ai-button',
  templateUrl: './api-documentation-v4-newt-ai-button.component.html',
})
export class ApiDocumentationV4NewtAiButtonComponent {
  @Input()
  state: ApiSpecGenState = ApiSpecGenState.UNAVAILABLE;

  @Output()
  generate = new EventEmitter<PageType>();

  getIcon() {
    switch (this.state) {
      case ApiSpecGenState.AVAILABLE:
        return 'gio:magic-wand';
      case ApiSpecGenState.STARTED:
      case ApiSpecGenState.GENERATING:
        return 'gio:question-mark-circle';
      default:
        return '';
    }
  }

  isNotUnavailable(): boolean {
    return this.state !== ApiSpecGenState.UNAVAILABLE;
  }
}
