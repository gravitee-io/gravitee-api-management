///
/// Copyright (C) 2015 The Gravitee team (http://gravitee.io)
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///         http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {Component, View, NgFor, NgIf} from 'angular2/angular2';

import {ApisService} from '../../services/ApisService';
import {Api} from './api';

@Component({
  selector: 'apis',
  appInjector: [ApisService]
})
@View({
  templateUrl: './components/apis/apis.html?v=<%= VERSION %>',
  directives: [NgFor, NgIf]
})

export class Apis {

  apis: Array<Api>;
  apisService: ApisService;

  constructor(apisService: ApisService) {
    this.apisService = apisService;
    this.list();
  }

  list() {
    this.apisService.list().then(data => {
      this.apis = data;
    });
  }

  start(name: string) {
    this.apisService.start(name).then(data => {
      this.apisService.reload(name).then(data => {
        this.list();
      });
    });
  }

  stop(name: string) {
    this.apisService.stop(name).then(data => {
      this.apisService.reload(name).then(data => {
        this.list();
      });
    });
  }
}