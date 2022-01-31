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

function DialogApiExportController($scope, $mdDialog, ApiService, apiId, base64, Build) {
  'ngInject';

  $scope.filteredFields = [
    { id: 'groups', description: 'Groups', checked: true },
    { id: 'members', description: 'Members', checked: true },
    { id: 'pages', description: 'Pages', checked: true },
    { id: 'plans', description: 'Plans', checked: true },
    { id: 'metadata', description: 'Metadata', checked: true },
  ];

  $scope.data = {
    exportVersion: null,
  };

  $scope.hide = function () {
    $mdDialog.hide();
  };

  $scope.graviteeVersion = Build.version;

  $scope.export = function () {
    const excludes = _.map(
      _.filter($scope.filteredFields, (fl: any) => {
        return !fl.checked;
      }),
      'id',
    );
    ApiService.export(apiId, excludes, $scope.data.exportVersion)
      .then((response) => {
        const link = document.createElement('a');
        document.body.appendChild(link);
        link.href = 'data:application/json;charset=utf-8;base64,' + base64.encode(JSON.stringify(response.data, null, 2));
        let fileName = response.data.name + ' ' + response.data.version;
        fileName = fileName.replace(/[\s]/gi, '-');
        fileName = fileName.replace(/[^\w]/gi, '-');
        link.download = fileName + '.json';
        link.target = '_self';
        link.click();
        document.body.removeChild(link);
      })
      .then(() => {
        ApiService.get(apiId).then((response) => {
          $mdDialog.hide(response);
        });
      });
  };
}

export default DialogApiExportController;
