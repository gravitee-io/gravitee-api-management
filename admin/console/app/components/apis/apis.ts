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

import {Component, View, NgFor} from 'angular2/angular2';

import {ApisService} from '../../services/ApisService';
import {Api} from './api';

@Component({
  selector: 'component-apis',
  appInjector: [ApisService]
})
@View({
  templateUrl: './components/apis/apis.html?v=<%= VERSION %>',
  directives: [NgFor]
})

export class Apis {
  apis: Array<Api>;

  constructor(apisService: ApisService) {
    apisService.list().then(data => {
      this.apis = data;
    })
  }
}