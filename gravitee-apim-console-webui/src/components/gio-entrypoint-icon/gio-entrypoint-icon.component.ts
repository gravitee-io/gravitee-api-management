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
  selector: 'gio-entrypoint-icon',
  template: require('./gio-entrypoint-icon.component.html'),
  styles: [require('./gio-entrypoint-icon.component.scss')],
})
export class GioEntrypointIconComponent {
  @Input()
  public entrypointName?: string;

  public entrypointIcon: string;

  private entrypointIcons = [
    { icon: 'gio:cloud-server', entrypointPrefix: 'sse' },
    { icon: 'gio:language', entrypointPrefix: 'http' },
    { icon: 'gio:webhook', entrypointPrefix: 'webhook' },
    { icon: 'gio:websocket', entrypointPrefix: 'websocket' },
  ];

  @Input()
  public set entrypointId(entrypointId: string) {
    this.entrypointIcon =
      this.entrypointIcons.find(({ entrypointPrefix }) => entrypointId.startsWith(entrypointPrefix))?.icon || 'gio:layers';
  }
}
