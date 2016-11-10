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
class ApiAnalyticsController {
  constructor(ApiService, ApplicationService, resolvedApi, $q, $scope, $state) {
    'ngInject';
    this.ApiService = ApiService;
    this.ApplicationService = ApplicationService;
    this.$scope = $scope;
    this.$scope.Object = Object;
    this.$state = $state;
    this.api = resolvedApi.data;
    this.$q = $q;
    this.cache = {};

    this.$scope.filteredApplications = [];

    // md table paging set up
    this.$scope.paging = [];

    this.analyticsData = this.analytics();

    if ($state.params.timestamp) {
      this.setTimestamp($state.params.timestamp);
    } else if ($state.params.timeframe) {
      this.setTimeframe($state.params.timeframe);
    } else {
      this.setTimeframe('3d');
    }

    this.indicatorChartOptions = {
      tooltips: {
        callbacks: {
          label: function (tooltipItem, data) {
            return data.labels[tooltipItem.index];
          }
        }
      }
    };

    $scope.options = {
      elements: {
        point: {
          radius: 5
        }
      },
      scales: {
        xAxes: [{
          display: false
        }],
        yAxes: [{
          stacked: false
        }]
      },
      tooltips: {
        mode: 'label'
      }
    };

    $scope.optionsStacked = _.cloneDeep($scope.options);
    $scope.optionsStacked.scales.yAxes[0].stacked = true;

    //from https://material.google.com/style/color.html#color-color-palette
    //shade 500 & 900
    //
    // deep purple, lime, deep orange, pink, purple,
    // light green, amber, Blue Grey, orange, teal,
    // indigo, purple, red, cyan, brown
    this.colorByBucket = [
      '#673ab7', '#cddc39', '#ff5722', '#e91e63', '#9c27b0',
      '#8bc34a', '#ffc107', '#607d8b', '#ff9800', '#009688',
      '#3f51b5', '#9c27b0', '#f44336', '#00bcd4', '#795548',

      '#311b92', '#827717', '#bf360c', '#880e4f', '#4a148c',
      '#33691e', '#ff6f00', '#263238', '#e65100', '#004d40',
      '#1a237e', '#4a148c', '#b71c1c', '#006064', '#3e2723'

    ];


    //from https://material.google.com/style/color.html#color-color-palette
    //shade 200 & 300
    this.bgColorByBucket = [
      '#b39ddb', '#e6ee9c', '#ffab91', '#f48fb1', '#ce93d8',
      '#c5e1a5', '#ffe082', '#b0bec5', '#ffcc80', '#80cbc4',
      '#9fa8da', '#ce93d8', '#ef9a9a', '#80deea', '#bcaaa4',

      '#9575cd', '#dce775', '#ff8a65', '#f06292', '#ba68c8',
      '#aed581', '#ffd54f', '#90a4ae', '#ffb74d', '#4db6ac',
      '#7986cb', '#ba68c8', '#e57373', '#4dd0e1', '#a1887f'
    ];

    $scope.chartConfig = [];

    var that = this;

    $scope.$watch('filteredApplications', function () {
      that.updateCharts();
    }, true);
  }

  openMenu($mdOpenMenu, ev) {
    $mdOpenMenu(ev);
  }

  updateDate(date) {
    if (date) {
      this.$state.transitionTo(this.$state.current, {apiId: this.api.id, timestamp: date.getTime() / 1000, timeframe: ''}, { notify: false });
      this.setTimestamp(date.getTime() / 1000);
      this.updateCharts();
    }
  }

  updateTimeframe(timeframeId) {
    if (timeframeId) {
      this.$state.transitionTo(this.$state.current, {apiId: this.api.id, timestamp: '', timeframe: timeframeId}, { notify: false });
      this.setTimeframe(timeframeId);
      this.updateCharts();
    }
  }

  setTimestamp(timestamp) {
    var momentDate = moment.unix(timestamp);

    this.$scope.timeframeDate = momentDate.toDate();

    var startDate = Math.floor(momentDate.startOf('day').valueOf() / 1000);
    var endDate = Math.floor(momentDate.endOf('day').valueOf() / 1000);
    this.analyticsData.range = {
      interval: 3600000,
      from: startDate * 1000,
      to: endDate * 1000
    };
  }

  setTimeframe(timeframeId) {
    var that = this;

    this.timeframe = _.find(this.analyticsData.timeframes, function (timeframe) {
      return timeframe.id === timeframeId;
    });

    var now = Date.now();

    var oldReport = (this.analyticsData === undefined) ? undefined : this.analyticsData.report;

    _.assignIn(this.analyticsData, {
      timeframe: that.timeframe,
      report: oldReport,
      range: {
        interval: that.timeframe.interval,
        from: now - that.timeframe.range,
        to: now
      }
    });
  }

  getApplication(id) {
    var deferred = this.$q.defer();
    var _this = this;
    if(this.cache[id]) {
      deferred.resolve(this.cache[id]);
    } else {
      this.ApplicationService.get(id).then(response => {
        _this.cache[id] = response.data;
        deferred.resolve(this.cache[id]);
    });
    }
    return deferred.promise;
  }

  fetchApplicationAnalytics(report, response) {
    // get APIs data
    var promises = [];
    for (var i = 0; i < response[0].data.values[0].buckets.length; i++) {
      promises.push(this.getApplication(response[0].data.values[0].buckets[i].name));
    }
    var _this = this;
    this.$q.all(promises).then(function() {
      _this.pushHitsByData(report, response);
      _this.pushTopHitsData();
    }, function() {
    });
  }

  pushHitsByData(report, response) {
    for (var i = 0; i < response[0].data.values[0].buckets.length; i++) {
      var lineColor = report.id === 'response-status' ? this.getColorByStatus(response[0].data.values[0].buckets[i].name) : this.colorByBucket[i % this.colorByBucket.length];
      var bgColor = report.id === 'response-status' ? this.getBgColorByStatus(response[0].data.values[0].buckets[i].name) : this.bgColorByBucket[i % this.bgColorByBucket.length];
      var label = report.requests[0].label ? report.requests[0].label : (report.label || report.labelPrefix + ' ' + response[0].data.values[0].buckets[i].name);
      if (report.id === 'applications') {
        var application = this.cache[response[0].data.values[0].buckets[i].name];
        label = application.name;
      }

      this.$scope.chartConfig[report.id].datasets.push({
        label: label,
        data: response[0].data.values[0].buckets[i].data,
        backgroundColor: bgColor,
        borderColor: lineColor,
        pointBackgroundColor: 'rgba(220,220,220,0.2)',
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: lineColor,
        pointHoverBorderColor: 'rgba(220,220,220,1)'
      });
    }
  }

  pushTopHitsData() {
    var _this = this;
    _.forEach(this.analyticsData.tops, function (top) {
      if (top.key === 'top-apps') {
        var request = top.request.call(_this.ApiService, _this.api.id,
          _this.analyticsData.range.from,
          _this.analyticsData.range.to,
          _this.analyticsData.range.interval,
          top.key,
          top.query,
          top.field,
          top.size);

        request.then(response => {
          if (Object.keys(response.data.values).length) {
          top.results = _.map(response.data.values, function (value, key) {
            return {topApp: key, topHits: value};
          });
          _this.$scope.paging[top.key] = 1;
          } else {
            delete top.results;
          }
        });
      }
    });
  }

  updateCharts() {
    var _this = this;

    var queryFilter = '';
    if (this.$scope.filteredApplications.length) {
      queryFilter = ' AND(';
      for (var i = 0; i < this.$scope.filteredApplications.length; i++) {
        queryFilter += 'application:' + this.$scope.filteredApplications[i].topApp + (this.$scope.filteredApplications.length - 1 === i ? ')' : ' OR ');
      }
    }

    _.forEach(this.analyticsData.reports, function (report) {
      var requests = _.map(report.requests, function (req) {
        return req.service.call(_this.ApiService, _this.api.id,
          req.key,
          req.query + queryFilter,
          req.field,
          req.aggType,
          _this.analyticsData.range.from,
          _this.analyticsData.range.to,
          _this.analyticsData.range.interval);
      });

      _this.$q.all(requests).then(response => {
        _this.$scope.chartConfig[report.id] = {
          labels: _.map(response[0].data.timestamps, function (timestamp) {
            return moment(timestamp).format("YYYY-MM-DD HH:mm:ss");
          }),
          datasets: []
        };

        // Push data for global hits
        if (response[1] && response[1].data.values && response[1].data.values[0] && response[1].data.values[0].buckets[0]) {
          _this.$scope.chartConfig[report.id].datasets.push({
            data: response[1].data.values[0].buckets[0].data,
            label: report.requests[1].label,
            backgroundColor: 'rgba(220,220,220,0.2)',
            borderColor: 'rgba(220,220,220,1)',
            pointBackgroundColor: 'rgba(220,220,220,1)',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: 'rgba(220,220,220,0.2)',
            pointHoverBorderColor: 'rgba(220,220,220,1)'
          });
        }

        // Push data for hits by 'something'
        if (response[0] && response[0].data.values && response[0].data.values[0]) {
          if (report.id === 'applications') {
            _this.fetchApplicationAnalytics(report, response);
          } else {
            _this.pushHitsByData(report, response);
          }
        }
      });
    });

    // indicators
    _this.indicators = [];
    _this.indicatorChartData = {labels: [], datasets: [{data: [], backgroundColor: []}]};
    // first we need to get total calls to produce ratios
    var totalIndicator = _.find(_this.analyticsData.indicators, 'isTotal');
    var request = totalIndicator.request.call(this.ApiService, this.api.id,
      _this.analyticsData.range.from,
      _this.analyticsData.range.to,
      _this.analyticsData.range.interval,
      totalIndicator.key,
      totalIndicator.query + queryFilter);

    request.then(response => {
      totalIndicator.value = response.data.hits;
      this.total = totalIndicator.value;

      // then we get other indicators
      var indicators = _.filter(this.analyticsData.indicators, function (indicator) {
        return !indicator.isTotal;
      });
      _.forEach(indicators, function (indicator) {
        var request = indicator.request.call(_this.ApiService, _this.api.id,
          _this.analyticsData.range.from,
          _this.analyticsData.range.to,
          _this.analyticsData.range.interval,
          indicator.key,
          indicator.query + queryFilter);

        request.then(response => {
          indicator.value = response.data.hits;
          if (indicator.value) {
            var percentage = _.round(indicator.value / _this.total * 100);
            _this.indicatorChartData.labels.push(indicator.title + ': (' + percentage + '%) ' + indicator.value + ' hits');
            var dataset = _this.indicatorChartData.datasets[0];
            dataset.data.push(percentage);
            dataset.backgroundColor.push(indicator.color);
          }
        });
      });
    });
  }

  //from https://material.google.com/style/color.html#color-color-palette
  //shade 400
  getColorByStatus(status) {
    if (_.startsWith(status, '1')) {
      return '#42a5f5'; //blue
    } else if (_.startsWith(status, '2')) {
      return '#66bb6a'; //green
    } else if (_.startsWith(status, '3')) {
      return '#ffee58'; //yellow
    } else if (_.startsWith(status, '4')) {
      return '#ef5350'; //red
    } else if (_.startsWith(status, '5')) {
      return '#8d6e63'; //brown
    }
    return '#bdbdbd'; //grey
  }

  //from https://material.google.com/style/color.html#color-color-palette
  //shade 200
  getBgColorByStatus(status) {
    if (_.startsWith(status, '1')) {
      return '#90caf9'; //blue
    } else if (_.startsWith(status, '2')) {
      return '#a5d6a7'; //green
    } else if (_.startsWith(status, '3')) {
      return '#fff59d'; //yellow
    } else if (_.startsWith(status, '4')) {
      return '#ef9a9a'; //red
    } else if (_.startsWith(status, '5')) {
      return '#bcaaa4'; //brown
    }
    return '#eeeeee'; //grey
  }

  analytics() {
    var _this = this;
    return {
      tops: [
        {
          title: 'Top applications',
          request: this.ApiService.apiTopHits,
          key: "top-apps",
          query: "api:" + _this.api.id,
          field: "application",
          size: 20
        }
      ],
      indicators: [
        {
          title: 'Total calls',
          request: this.ApiService.apiGlobalHits,
          key: "total",
          query: "api:" + _this.api.id + " AND status:[100 TO 599]",
          color: 'silver',
          isTotal: true
        },
        {
          title: '1xx',
          request: this.ApiService.apiGlobalHits,
          key: "1xx",
          query: "api:" + _this.api.id + " AND status:[100 TO 199]",
          color: _this.getColorByStatus('100')
        },
        {
          title: '2xx',
          request: this.ApiService.apiGlobalHits,
          key: "2xx",
          query: "api:" + _this.api.id + " AND status:[200 TO 299]",
          color: _this.getColorByStatus('200')
        },
        {
          title: '3xx',
          request: this.ApiService.apiGlobalHits,
          key: "3xx",
          query: "api:" + _this.api.id + " AND status:[300 TO 399]",
          color: _this.getColorByStatus('300')
        },
        {
          title: '4xx',
          request: this.ApiService.apiGlobalHits,
          key: "4xx",
          query: "api:" + _this.api.id + " AND status:[400 TO 499]",
          color: _this.getColorByStatus('400')
        },
        {
          title: '5xx',
          request: this.ApiService.apiGlobalHits,
          key: "5xx",
          query: "api:" + _this.api.id + " AND status:[500 TO 599]",
          color: _this.getColorByStatus('500')
        }
      ],
      reports: [
        {
          id: 'response-status',
          type: 'line',
          stacked: true,
          title: 'Response Status',
          labelPrefix: 'HTTP Status',
          requests: [
            {
              "service": this.ApiService.apiHitsBy,
              "key": "api-hits-by-status",
              "query": "api:" + _this.api.id,
              "field": "status",
              "aggType": "terms"
            }
          ]
        }, {
          id: 'response-times',
          type: 'line',
          stacked: false,
          title: 'Response Times',
          requests: [
            {
              "label": "Global latency (average in ms)",
              "service": this.ApiService.apiHitsBy,
              "key": "api-hits-by-latency",
              "query": "api:" + _this.api.id,
              "field": "response-time",
              "aggType": "avg"
            },
            {
              "label": "API latency (average in ms)",
              "service": this.ApiService.apiHitsBy,
              "key": "api-hits-by-api-latency",
              "query": "api:" + _this.api.id,
              "field": "api-response-time",
              "aggType": "avg"
            }
          ]
        }, {
          id: 'applications',
          type: 'line',
          stacked: true,
          title: 'Hits by applications',
          labelPrefix: '',
          requests: [
            {
              "service": this.ApiService.apiHitsBy,
              "key": "api-hits-by-application",
              "query": "api:" + _this.api.id,
              "field": "application",
              "aggType": "terms"
            }
          ]
        }
      ],
      timeframes: [
        {
          id: '5m',
          title: 'Last 5 minutes',
          range: 1000 * 60 * 5,
          interval: 10000
        }, {
          id: '1h',
          title: 'Last hour',
          range: 1000 * 60 * 60,
          interval: 1000 * 60
        }, {
          id: '24h',
          title: 'Last 24 hours',
          range: 1000 * 60 * 60 * 24,
          interval: 1000 * 60 * 60
        }, {
          id: '3d',
          title: 'Last 3 days',
          range: 1000 * 60 * 60 * 24 * 3,
          interval: 1000 * 60 * 60 * 3
        }, {
          id: '14d',
          title: 'Last 14 days',
          range: 1000 * 60 * 60 * 24 * 14,
          interval: 1000 * 60 * 60 * 5
        }, {
          id: '30d',
          title: 'Last 30 days',
          range: 1000 * 60 * 60 * 24 * 30,
          interval: 10000000
        }, {
          id: '90d',
          title: 'Last 90 days',
          range: 1000 * 60 * 60 * 24 * 90,
          interval: 10000000
        }
      ]
    };
  }
}

export default ApiAnalyticsController;
