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
  selector: 'gio-selection-list-option-layout-icon',
})
export class GioSelectionListOptionLayoutIconDirective {}

@Directive({
  selector: 'gio-selection-list-option-layout-title',
})
export class GioSelectionListOptionLayoutTitleDirective {}

@Directive({
  selector: 'gio-selection-list-option-layout-body',
})
export class GioSelectionListOptionLayoutBodyDirective {}

@Directive({
  selector: 'gio-selection-list-option-layout-action',
})
export class GioSelectionListOptionLayoutActionDirective {}

@Component({
  selector: 'gio-selection-list-option-layout',
  templateUrl: './gio-selection-list-option-layout.component.html',
  styleUrls: ['./gio-selection-list-option-layout.component.scss'],
})
export class GioSelectionListOptionLayoutComponent {}
