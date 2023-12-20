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

import { RouterModule, Routes } from "@angular/router";

import { AddIntegrationComponent } from "./pages/add-integration/add-integration.component";
import { CatalogComponent } from "./components/catalog/catalog.component";
import { IntegrationsComponent } from "./integrations.component";
import { MyIntegrationsComponent } from "./components/my-integrations/my-integrations.component";
import { NgModule } from "@angular/core";

const routes: Routes = [
  {
    path: "",
    component: IntegrationsComponent,
    children: [
      {
        path: "catalog",
        component: CatalogComponent
      },
      {
        path: "my-integrations",
        component: MyIntegrationsComponent
      },
      {
        path: "",
        redirectTo: "catalog",
        pathMatch: "full"
      }
    ]
  },

  {
    path: "add-integration",
    component: AddIntegrationComponent
  },

];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class IntegrationsRoutingModule {
}
