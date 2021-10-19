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
import { duration } from 'moment';

import InstancesService from '../../../../services/instances.service';

class InstanceMonitoringController {
  private monitoringData: any;
  private instance: any;
  private instanceStarted: boolean;
  private monitoringCpuChartData: { series: [{ name: string; data: any[] }] };
  private monitoringHeapChartData: { series: [{ name: string; data: any[] }] };
  private interval: any;

  constructor(private $stateParams, private InstancesService: InstancesService) {
    'ngInject';
  }

  $onInit() {
    this.buildChartData();
    this.instanceStarted = this.instance.state === 'STARTED';

    if (this.instanceStarted) {
      this.interval = setInterval(() => {
        this.InstancesService.getMonitoringData(this.$stateParams.instanceId, this.instance.id).then((response) => {
          this.monitoringData = response.data;
          this.buildChartData();
        });
      }, 5000);
    }
  }

  $onDestroy() {
    clearInterval(this.interval);
  }

  humanizeDuration(timeInMillis) {
    return duration(-timeInMillis).humanize(true);
  }

  humanizeSize(bytes, precision) {
    if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
      return '-';
    }
    if (typeof precision === 'undefined') {
      precision = 1;
    }
    const units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
      number = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) + ' ' + units[number];
  }

  ratio(value, value2) {
    return _.round((value / value2) * 100);
  }

  getPieChartPercentColor(value) {
    return value > 80 ? '#d62728' : '#2ca02c';
  }

  buildChartData() {
    this.monitoringCpuChartData = {
      series: [
        {
          name: 'CPU',
          data: [this.monitoringData.process && this.monitoringData.process.cpu_percent],
        },
      ],
    };
    this.monitoringHeapChartData = {
      series: [
        {
          name: 'Heap',
          data: [this.monitoringData.jvm && this.monitoringData.jvm.heap_used_percent],
        },
      ],
    };
  }
}

export default InstanceMonitoringController;
