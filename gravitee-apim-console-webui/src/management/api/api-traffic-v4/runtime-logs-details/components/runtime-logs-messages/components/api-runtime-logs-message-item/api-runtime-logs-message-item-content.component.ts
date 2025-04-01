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

import { Component, Input } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';

import { Message } from '../../../../../../../../entities/management-api-v2';

type ContentTab = {
  label: string;
  disabled: boolean;
  content?: unknown;
  isJson: boolean;
};

@Component({
  selector: 'api-runtime-logs-message-item-content',
  templateUrl: './api-runtime-logs-message-item-content.component.html',
  styleUrls: ['./api-runtime-logs-message-item-content.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsMessageItemContentComponent {
  @Input()
  type: string;
  @Input()
  connectorIcon: string;

  tabs: ContentTab[] = [
    { label: 'Payload', content: undefined, disabled: false, isJson: false },
    { label: 'Headers', content: undefined, disabled: false, isJson: true },
    { label: 'Metadata', content: undefined, disabled: false, isJson: true },
  ];
  selected = new UntypedFormControl(0);

  _content: Message;

  @Input()
  get content(): Message {
    return this._content;
  }
  set content(content: Message) {
    this._content = content;
    this.tabs[0].content = this.content.payload;
    this.tabs[0].disabled = !(this.content.payload != null);
    this.tabs[1].content = this.content.headers;
    this.tabs[1].disabled = !(this.content.headers != null);
    this.tabs[2].content = this.content.metadata;
    this.tabs[2].disabled = !(this.content.metadata != null);

    // Select the first tab that is not disabled
    this.selected.setValue(this.tabs.findIndex((tab) => !tab.disabled));
  }
}
