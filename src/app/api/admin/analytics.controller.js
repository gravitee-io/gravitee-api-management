/**
 * Created by david on 27/11/2015.
 */
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
/* global document:false */
class ApiAnalyticsController {
  constructor (ApiService, resolvedApi, $q, $state, $mdDialog, NotificationService, $scope) {
    'ngInject';
    this.ApiService = ApiService;
    this.$mdDialog = $mdDialog;
    this.NotificationService = NotificationService;
    this.$scope = $scope;
    this.$state = $state;
    this.api = resolvedApi.data;
    this.$q = $q;

    this.analytics_apiHits($state.params.apiId);
  }

  openMenu($mdOpenMenu, ev) {
    //originatorEv = ev;
    console.log('open');
    $mdOpenMenu(ev);
  };

  analytics_apiHits(apiId) {
    this.$q.all([
      this.ApiService.apiHits(apiId, 1000000, 1448538051104, 1448581251104),
      this.ApiService.apiHitsByStatus(apiId, 1000000, 1448538051104, 1448581251104)]).then(response => {

      this.$scope.chartConfig = {
        credits: {
          enabled: false
        },
        title: {
          text: ''
        },
        xAxis: {
          type: 'datetime',
          categories: response[0].data.timestamps,
          labels:{
            formatter:function(){
              return Highcharts.dateFormat('%Y %M %d', this.value);
            }
          }
        },
        yAxis: { title: { text: 'API Calls' } },
        plotOptions: {
          area: {
            marker: {
              enabled: false,
              symbol: 'circle',
              radius: 2,
              states: {
                hover: {
                  enabled: true
                }
              }
            }
          },
          column: {
            stacking: 'normal'
          }
        },
        series: []
      };

      // Push data for global hits
      this.$scope.chartConfig.series.push({
        name: this.api.name,
        type:'area',
        showInLegend: false,
        tooltip: {
          pointFormat: '{series.name}: <b>{point.y}</b> calls'
        },
        data: response[0].data.values[0].buckets[0].data
      });

      // Push data for hits by status
      for (var i = 0; i < response[1].data.values[0].buckets.length; i++) {
        this.$scope.chartConfig.series.push({
          type: 'column',
          tooltip: {
            pointFormat: 'HTTP Status <b>{series.name}</b>: <b>{point.y}</b> calls'
          },
          name: response[1].data.values[0].buckets[i].name,
          data: response[1].data.values[0].buckets[i].data
        });
      }
    });
  }
}

export default ApiAnalyticsController;
