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
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
<<<<<<< HEAD
import { GioBreadcrumbModule, GioSubmenuModule } from '@gravitee/ui-particles-angular';
=======
import { GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { UIRouterModule } from '@uirouter/angular';
>>>>>>> 470ea5521c (fix: enable right click on menu items)

import { SettingsNavigationComponent } from './settings-navigation.component';

@NgModule({
<<<<<<< HEAD
  imports: [CommonModule, GioSubmenuModule, GioBreadcrumbModule],
=======
  imports: [CommonModule, GioSubmenuModule, UIRouterModule],
>>>>>>> 470ea5521c (fix: enable right click on menu items)
  declarations: [SettingsNavigationComponent],
  exports: [SettingsNavigationComponent],
  entryComponents: [SettingsNavigationComponent],
})
export class SettingsNavigationModule {}
