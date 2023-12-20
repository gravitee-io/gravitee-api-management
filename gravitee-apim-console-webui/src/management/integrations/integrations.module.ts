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


import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { MatRadioModule } from "@angular/material/radio";
import { MatTabsModule } from "@angular/material/tabs";
import { NgModule } from "@angular/core";

import { IntegrationsRoutingModule } from "./integrations-routing.module";
import { AddIntegrationComponent } from "./pages/add-integration/add-integration.component";
import { CatalogComponent } from "./components/catalog/catalog.component";
import { IntegrationsComponent } from "./integrations.component";
import { MyIntegrationsComponent } from "./components/my-integrations/my-integrations.component";


@NgModule({
  imports: [
    CommonModule,
    IntegrationsRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatButtonModule,
    MatInputModule,
    MatRadioModule
  ],
  declarations: [
    IntegrationsComponent,
    CatalogComponent,
    MyIntegrationsComponent,
    AddIntegrationComponent
  ]
})
export class IntegrationsModule {
}
