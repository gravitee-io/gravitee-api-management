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
import { Component, CUSTOM_ELEMENTS_SCHEMA, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ConsoleExtensionRegistryService } from '../../services-ngx/console-extension-registry.service';

@Component({
  selector: 'plugin-content',
  template: '<div #container style="height: 100%"></div>',
  styles: [':host { display: block; height: 100%; }'],
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class PluginContentComponent implements OnInit {
  @ViewChild('container', { static: true }) container: ElementRef;

  constructor(
    private activatedRoute: ActivatedRoute,
    private consoleExtensionRegistryService: ConsoleExtensionRegistryService,
  ) {}

  ngOnInit() {
    const pluginId = this.activatedRoute.snapshot.params['pluginId'];
    const plugins = this.consoleExtensionRegistryService.getComponentsByPlacement('center');
    const plugin = plugins.find((p) => p.pluginId === pluginId);
    if (plugin) {
      const el = document.createElement(plugin.tagName);
      el.setAttribute('placement', 'center');
      this.container.nativeElement.appendChild(el);
    }
  }
}
