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
class TimeframeDirective {
  constructor() {

    let directive = {
      restrict: 'E',
      templateUrl: 'app/components/analytics/timeframe.html',
      controller: TimeframeController,
      controllerAs: 'timeframeCtrl',
      bindToController: true
    };

    return directive;
  }
}

class TimeframeController {
  constructor($scope, $rootScope, $state, $timeout) {
    'ngInject';
    this.$scope = $scope;
    this.now = moment().toDate();

    this.$scope.$on('timeframeReload', function () {
      let updated = false;
      if (_that.$state.params.from && _that.$state.params.to) {
        updated = true;
        _that.update({
          from: _that.$state.params.from,
          to: _that.$state.params.to
        });
      }

      _that.setTimeframe(_that.$state.params.timeframe || '1d', ! updated);
    });

    this.$state = $state;
    this.$rootScope = $rootScope;
    this.$timeout = $timeout;

    this.timeframes = [
      {
        id: '5m',
        title: 'Last 5m',
        range: 1000 * 60 * 5,
        interval: 1000 * 10
      }, {
        id: '30m',
        title: ' 30m',
        range: 1000 * 60 * 30,
        interval: 1000 * 15
      }, {
        id: '1h',
        title: ' 1h',
        range: 1000 * 60 * 60,
        interval: 1000 * 30
      }, {
        id: '3h',
        title: ' 3h',
        range: 1000 * 60 * 60 * 3,
        interval: 1000 * 60
      }, {
        id: '6h',
        title: ' 6h',
        range: 1000 * 60 * 60 * 6,
        interval: 1000 * 60 * 2
      }, {
        id: '12h',
        title: ' 12h',
        range: 1000 * 60 * 60 * 12,
        interval: 1000 * 60 * 5
      }, {
        id: '1d',
        title: '1d',
        range: 1000 * 60 * 60 * 24,
        interval: 1000 * 60 * 10
      }, {
        id: '3d',
        title: '3d',
        range: 1000 * 60 * 60 * 24 * 3,
        interval: 1000 * 60 * 30
      }, {
        id: '7d',
        title: '7d',
        range: 1000 * 60 * 60 * 24 * 7,
        interval: 1000 * 60 * 60
      }, {
        id: '14d',
        title: '14d',
        range: 1000 * 60 * 60 * 24 * 14,
        interval: 1000 * 60 * 60 * 3
      }, {
        id: '30d',
        title: '30d',
        range: 1000 * 60 * 60 * 24 * 30,
        interval: 1000 * 60 * 60 * 6
      }, {
        id: '90d',
        title: '90d',
        range: 1000 * 60 * 60 * 24 * 90,
        interval: 1000 * 60 * 60 * 12
      }
    ];

    var _that = this;

    // Event received when a zoom is done on a chart
    this.$rootScope.$on('timeframeZoom', function (event, zoom) {
      let diff = zoom.to - zoom.from;

      let timeframe = _.findLast(_that.timeframes, function(timeframe) {
        return timeframe.range < diff;
      });

      if (!timeframe) {
        timeframe = _that.timeframes[0];
      }

      _that.update({
        interval: timeframe.interval,
        from: zoom.from,
        to: zoom.to
      });
    });

    this.$rootScope.$on('queryUpdated', function (event, query) {
      console.log(query);
    });
  }

  updateTimeframe(timeframeId) {
    if (timeframeId) {
      this.$state.transitionTo(
        this.$state.current,
        _.merge(this.$state.params, {
          timestamp: '',
          timeframe: timeframeId
        }),
        {notify: false});
      this.setTimeframe(timeframeId, true);
    }
  }

  setTimestamp(timestamp) {
    var momentDate = moment.unix(timestamp);

    var startDate = Math.floor(momentDate.startOf('day').valueOf() / 1000);
    var endDate = Math.floor(momentDate.endOf('day').valueOf() / 1000);

    this.update({
      interval: 1000 * 60 * 5,
      from: startDate * 1000,
      to: endDate * 1000
    });
  }

  setTimeframe(timeframeId, update) {
    var that = this;

    this.timeframe = _.find(this.timeframes, function (timeframe) {
      return timeframe.id === timeframeId;
    });

    if (update) {
      var now = Date.now();

      this.update({
        interval: that.timeframe.interval,
        from: now - that.timeframe.range,
        to: now
      });
    }
  }

  update(timeframe) {
    let that = this;

    timeframe = {
      interval: parseInt(timeframe.interval),
      from: parseInt(timeframe.from),
      to: parseInt(timeframe.to)
    };

    let diff = timeframe.to - timeframe.from;

    let tf = _.findLast(that.timeframes, function(timeframe) {
      return timeframe.range < diff;
    });

    if (!tf) {
      tf = that.timeframes[0];
    }

    // timeframeChange event is dynamically initialized, so we have to define a timeout to correctly fired it
    this.$timeout(function () {
      that.$scope.$broadcast('timeframeChange', {
        interval: tf.interval,
        from: timeframe.from,
        to: timeframe.to
      });
      that.$scope.$emit('timeframeChange', {
        interval: tf.interval,
        from: timeframe.from,
        to: timeframe.to
      });
    }, 200);

    this.$scope.current = {
      interval: tf.interval,
      intervalLabel: moment.duration(tf.interval).humanize(),
      from: timeframe.from,
      to: timeframe.to
    };

    this.$state.transitionTo(
      this.$state.current,
      _.merge(this.$state.params, {
        timeframe: tf.timeframe,
        interval: timeframe.interval,
        from: timeframe.from,
        to: timeframe.to
      }),
      {notify: false});

    this.pickerStartDate = moment(timeframe.from).toDate();
    this.pickerEndDate = moment(timeframe.to).toDate();
  }

  updateRangeDate() {
    let _that = this;

    let from =  moment(_that.pickerStartDate).startOf('day').unix() * 1000;
    let to = moment(_that.pickerEndDate).endOf('day').unix() * 1000;

    let diff = to - from;

    let timeframe = _.findLast(_that.timeframes, function(timeframe) {
      return timeframe.range < diff;
    });

    if (!timeframe) {
      timeframe = _that.timeframes[0];
    }

    this.update({
      interval: timeframe.interval,
      from: from,
      to: to
    });
  }
}

export default TimeframeDirective;
