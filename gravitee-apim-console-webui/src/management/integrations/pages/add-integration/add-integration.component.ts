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

import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { FormBuilder, FormGroup } from "@angular/forms";

@Component({
  selector: "app-add-integration",
  templateUrl: "./add-integration.component.html",
  styleUrls: ["./add-integration.component.scss"]
})
export class AddIntegrationComponent implements OnInit {
  public provider = "";
  public serviceName = "";
  public addIntegrationForm: FormGroup;

// {
//   "type": "aws",
//   "name": "EU-AWS",
//   "description": "AWS integration for EU region",
//   "configuration": {
//     "region": "",
//     "accessKey": "",
//     "secretAccessKey": ""
//   }
// }




  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private formBuilder: FormBuilder
  ) {
  }

  ngOnInit() {
    this.provider = this.route.snapshot.paramMap.get("provider");
    this.serviceName = this.providerToServiceName(this.provider);

    this.addIntegrationForm = this.formBuilder.group({
      name: [""],
      description: [""],

      configuration: this.formBuilder.group({
        region: [""],
        accessKey: [""],
        secretAccessKey: [""]
      })
    });
  }

  public providerToServiceName(provider) {
    const titles = {
      aws: "Amazon API Gateway",
      solace: "Solace"
    };
    return titles[provider];
  }

  onSubmit(form: FormGroup) {
    console.log('Valid?', form.valid);
    console.log('BANG : the form values!!', form.value);
  }

  public handleExit() {
    this.router.navigate(["../../"], { relativeTo: this.route });
  }

}
