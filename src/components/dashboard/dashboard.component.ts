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

const DashboardComponent: ng.IComponentOptions = {
  template: require('./dashboard.html'),
  bindings: {
    model: '<',
    accessLogs: '<',
    onFilterChange: '&',
    onTimeframeChange: '&',
    onViewLogClick: '&'
  },
  controller: function($scope) {
    'ngInject';

    this.initialEventCounter = 2;
    this.initialTimeFrame;
    this.initialQuery;


    this.dashboardOptions = {
      margins: [10, 10],
      columns: 6,
      swapping: false,
      draggable: {
        enable: true,
        handle: 'md-card-title'
      },
      resizable: {
        enabled: true,
        stop: function () {
          $scope.$broadcast('onWidgetResize');
        }
      },
      rowHeight: 330
    };

    this.timeframeChange = function(timeframe) {
      if(this.initialEventCounter > 0) {
          this.initialEventCounter--;
      }
      if(this.initialEventCounter == 0) {
        //TODO: remove event broadcast and call a widget function instead
        $scope.$broadcast('onTimeframeChange', timeframe);
        if (this.onTimeframeChange) {
            this.onTimeframeChange({timeframe: timeframe});
        }
        if(this.initialQuery) {
            $scope.$broadcast('onQueryFilterChange', {query: this.initialQuery, source: undefined});
            if (this.onFilterChange) {
              this.onFilterChange({query: this.initialQuery});
            }
            delete(this.initialQuery);
        }
      } else {
        //waiting for queryFilterChange event ==> store timeframe for further broadcast
        this.initialTimeFrame = timeframe;
      }
    };

    this.queryFilterChange = function(query, widget) {
      if(this.initialEventCounter > 0) {
          this.initialEventCounter--;
        }
        if(this.initialEventCounter == 0) {
          //TODO: remove event broadcast and call a widget function instead
          $scope.$broadcast('onQueryFilterChange', {query: query, source: widget});
          if (this.onFilterChange) {
            this.onFilterChange({query: query});
          }
          if(this.initialTimeFrame) {
            $scope.$broadcast('onTimeframeChange', this.initialTimeFrame);
            if (this.onTimeframeChange) {
              this.onTimeframeChange({timeframe: this.initialTimeFrame});
            }
            delete(this.initialTimeFrame);
          }
        } else {
          //waiting for timeFrameChange event ==> store query for further broadcast
          this.initialQuery = query;
        }
    };

    this.viewLogs = function() {
      if (this.onViewLogClick) {
        this.onViewLogClick();
      }
    };

    this.$onInit = () => {
      _.each(this.model, widget => {
        widget.$uid = this.guid();
      });
    };

    this.guid = function() {
      function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
          .toString(16)
          .substring(1);
      }
      return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
    }
  }
};

export default DashboardComponent;
