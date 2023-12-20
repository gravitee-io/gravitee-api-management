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

import { ActivatedRoute, Router } from "@angular/router";
import { Component } from "@angular/core";

import { catalogData } from "../../data/catalog.data";

@Component({
  selector: "app-catalog",
  templateUrl: "./catalog.component.html",
  styleUrls: ["./catalog.component.scss"]
})
export class CatalogComponent {
  protected readonly catalogData = catalogData;


  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) {
  }


  public handleAdd(provider: string): void {
    this.router.navigate([`../add-integration/${provider}`], { relativeTo: this.route });
  }

  public handleView(provider: string) {
    return;
  }

  public getIntegrationsCountMessage(integrationsCount) {
    return integrationsCount ? `${integrationsCount} integration(s) configured` : "No integration configured";
  }
}
