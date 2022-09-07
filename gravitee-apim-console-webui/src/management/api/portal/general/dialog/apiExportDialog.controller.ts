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
function DialogApiExportController($scope, $mdDialog, ApiService, apiId, base64, Build) {
  'ngInject';

  const defaultFilteredFields = [
    { id: 'groups', description: 'Groups', checked: true, disabled: false },
    { id: 'members', description: 'Members', checked: true, disabled: false },
    { id: 'pages', description: 'Pages', checked: true, disabled: false },
    { id: 'plans', description: 'Plans', checked: true, disabled: false },
    { id: 'metadata', description: 'Metadata', checked: true, disabled: false },
  ];

  const crdFilteredFields = [
    { id: 'groups', description: 'Groups', checked: false, disabled: true },
    { id: 'members', description: 'Members', checked: false, disabled: true },
    { id: 'pages', description: 'Pages', checked: false, disabled: true },
    { id: 'plans', description: 'Plans', checked: true, disabled: true },
    { id: 'metadata', description: 'Metadata', checked: false, disabled: true },
  ];

  $scope.data = {
    exportVersion: null,
  };

  $scope.filteredFields = defaultFilteredFields;

  $scope.updateFilteredFields = () => {
    if ($scope.data.exportVersion === 'crd') {
      $scope.filteredFields = crdFilteredFields;
    } else {
      $scope.filteredFields = defaultFilteredFields;
    }
  };

  $scope.hide = function () {
    $mdDialog.hide();
  };

  $scope.graviteeVersion = Build.version;

  $scope.export = function () {
    if ($scope.data.exportVersion !== 'crd') {
      exportAsJson(buildExcludes());
    } else {
      exportAsCrd();
    }
  };

  function buildExcludes() {
    return $scope.filteredFields.filter((field) => !field.checked).map((field) => field.id);
  }

  function exportAsJson(excludes) {
    ApiService.export(apiId, excludes, $scope.data.exportVersion)
      .then((response) => buildDownloadLink('json', JSON.stringify(response.data)))
      .then((link) => download(link, '.json'));
  }

  function exportAsCrd() {
    ApiService.exportCrd(apiId)
      .then((response) => buildDownloadLink('yaml', response.data))
      .then((link) => download(link, '-crd.yml'));
  }

  function download(link, extension) {
    ApiService.get(apiId).then((response) => {
      link.download = buildFileName(response.data, extension);
      link.target = '_self';
      link.click();
      document.body.removeChild(link);
      $mdDialog.hide();
    });
  }

  function buildFileName(api, extension) {
    return `${api.name}-${api.version}`.replace(/[\s]/gi, '-').replace(/[^\w]/gi, '-') + extension;
  }

  function buildDownloadLink(format, data) {
    const link = document.createElement('a');
    document.body.appendChild(link);
    link.href = `data:application/${format};charset=utf-8;base64,` + base64.encode(data);
    return link;
  }
}

export default DialogApiExportController;
