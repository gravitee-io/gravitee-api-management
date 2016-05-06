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
class InstanceMonitoringController {
  constructor(resolvedMonitoringData, $stateParams, resolvedInstance, InstancesService, $scope) {
    'ngInject';
    this.monitoringData = resolvedMonitoringData.data;
    this.instanceStarted = resolvedInstance.data.state === 'started';

    if (this.instanceStarted) {
      var that = this;
      var interval = setInterval(function () {
        InstancesService.getMonitoringData($stateParams.id, resolvedInstance.data.id).then(function (response) {
          that.monitoringData = response.data;
        });
      }, 5000);

      $scope.$on('$stateChangeStart', function () {
        clearInterval(interval);
      });
    }
  }

  humanizeDuration(timeInMillis) {
    return moment.duration(-timeInMillis).humanize(true);
  }

  humanizeSize(bytes, precision) {
    if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
    if (typeof precision === 'undefined') precision = 1;
    var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
      number = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) + ' ' + units[number];
  }

  ratio(value, value2) {
    return _.round(value / value2 * 100);
  }

  getPieChartPercentColor(value) {
    return value > 80 ? '#d62728' : '#2ca02c';
  }
}

export default InstanceMonitoringController;
