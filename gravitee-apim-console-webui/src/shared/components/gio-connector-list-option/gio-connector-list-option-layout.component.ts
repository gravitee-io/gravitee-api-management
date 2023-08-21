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

import { Component, Directive } from '@angular/core';

@Directive({
  selector: 'gio-connector-list-option-layout-title',
})
export class GioConnectorListOptionLayoutTitleDirective {}

@Directive({
  selector: 'gio-connector-list-option-layout-body',
})
export class GioConnectorListOptionLayoutBodyDirective {}

@Directive({
  selector: 'gio-connector-list-option-layout-action',
})
export class GioConnectorListOptionLayoutActionDirective {}

@Component({
  selector: 'gio-connector-list-option-layout',
  template: require('./gio-connector-list-option-layout.component.html'),
  styles: [require('./gio-connector-list-option-layout.component.scss')],
})
export class GioConnectorListOptionLayoutComponent {}
