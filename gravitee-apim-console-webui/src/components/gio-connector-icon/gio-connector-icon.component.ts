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

@Component({
  selector: 'gio-connector-icon',
  template: require('./gio-connector-icon.component.html'),
  styles: [require('./gio-connector-icon.component.scss')],
})
export class GioConnectorIconComponent {
  @Input()
  public connectorName?: string;

  @Input()
  public connectorType: string;

  public connectorIcon: string;

  private icons = {
    entrypoint: [
      { icon: 'gio:cloud-server', prefix: 'sse' },
      { icon: 'gio:language', prefix: 'http' },
      { icon: 'gio:webhook', prefix: 'webhook' },
      { icon: 'gio:websocket', prefix: 'websocket' },
    ],
    endpoint: [
      { icon: 'gio:kafka', prefix: 'kafka' },
      { icon: 'gio:page', prefix: 'mock' },
      { icon: 'gio:mqtt', prefix: 'mqtt' },
    ],
  };

  @Input()
  public set connectorId(connectorId: string) {
    this.connectorIcon = this.icons[this.connectorType].find(({ prefix }) => connectorId.startsWith(prefix))?.icon || 'gio:layers';
  }
}
