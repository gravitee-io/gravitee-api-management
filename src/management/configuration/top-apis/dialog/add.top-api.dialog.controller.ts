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
import * as _ from 'lodash';
function AddTopApiDialogController($mdDialog: angular.material.IDialogService, ApiService, TopApiService,
                                   NotificationService, topApis) {
  'ngInject';

  ApiService.list().then((response) => {
    this.apis = _.filter(response.data, function (api:any) {
      return !_.includes(_.map(topApis, 'api'), api.id);
    });
  });

  this.addApi = function () {
    TopApiService.create(this.selectedApi).then((response) => {
      NotificationService.show('Api \'' + this.selectedApi.name + '\' added with success to the list of top APIs');
      $mdDialog.hide(response.data);
    });
  };

  this.hide = function () {
    $mdDialog.cancel();
  };
}

export default AddTopApiDialogController;
