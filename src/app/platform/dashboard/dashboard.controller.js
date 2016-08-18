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
class DashboardController {

  constructor(EventsService, AnalyticsService, ApiService, ApplicationService, $scope, $state, $timeout) {
    'ngInject';
    this.EventsService = EventsService;
    this.AnalyticsService = AnalyticsService;
    this.ApiService = ApiService;
    this.ApplicationService = ApplicationService;
    this.$scope = $scope;
    this.$state = $state;
    this.$timeout = $timeout;
    this.eventTypes = [];
    this.selectedAPIs = [];
    this.selectedApplications = [];
    this.selectedEventTypes = [];
    this.beginDate = moment().subtract(1, 'weeks').toDate();
    this.endDate = moment().toDate();
    this.now = moment().toDate();
    this.cache = {};

    // init events
    this.initEventTypes();
    this.initPagination();
    this.getEvents = this.getEvents.bind(this);

    // init charts
    this.$scope.paging = [];
    this.analyticsData = this.analytics();
    if ($state.params.from && $state.params.to) {
      this.setRangeDate($state.params.from, $state.params.to);
    } else if ($state.params.timeframe) {
      this.setTimeframe($state.params.timeframe);
    } else {
      this.setTimeframe('7d');
    }

    // get data
    this.updateCharts();
    this.searchEvents();
  }

  undoAPI() {
    this.updateCharts();
    this.searchEvents();
  }

  selectAPI() {
    this.updateCharts();
    this.searchEvents();
  }

  undoApplication() {
    this.updateCharts();
  }

  selectApplication() {
    this.updateCharts();
  }

  selectEvent(eventType) {
    var idx = this.selectedEventTypes.indexOf(eventType);
    if (idx > -1) {
      this.selectedEventTypes.splice(idx, 1);
    }
    else {
      this.selectedEventTypes.push(eventType);
    }
    this.searchEvents();
  }

  getEvents() {
    this.searchEvents();
  }

  searchEvents() {
    var from = moment(this.beginDate).unix() * 1000;
    var to = moment(this.endDate).unix() * 1000;
    if (from === to) {
      to = moment(this.endDate).add(24, 'hours').unix() * 1000;
    }

    // set apis
    var apis = this.selectedAPIs.map(function(api){ return api.id; }).join(",");
    // set event types
    var types = this.eventTypes;
    if (this.selectedEventTypes.length > 0) {
      types = this.selectedEventTypes.join(",");
    }

    // search
    this.$scope.eventsFetchData = true;
    this.EventsService.search(types, apis, from, to, this.query.page - 1, this.query.limit).then(response => {
      this.events = response.data;
      this.$scope.eventsFetchData = false;
    });
  }

  searchAPI(query) {
    if (query) {
      return this.ApiService.list().then(function(response) {
        return _.filter(response.data,
          function(api) {
            return api.name.toUpperCase().indexOf(query.toUpperCase()) > -1;
          });
      });
    }
  }

  searchApplication(query) {
    if (query) {
      return this.ApplicationService.list().then(function(response) {
        return _.filter(response.data,
          function(application) {
            return application.name.toUpperCase().indexOf(query.toUpperCase()) > -1;
          });
      });
    }
  }

  initPagination() {
    this.query = {
      limit: 10,
      page: 1
    };
  }

  initEventTypes() {
    this.eventTypes = ['START_API', 'STOP_API', 'PUBLISH_API', 'UNPUBLISH_API'];
  }

  updateCharts() {
    var _this = this;

    var queryFilter = '';
    if (this.selectedAPIs.length) {
      queryFilter = ' AND(';
      for (var i = 0; i < this.selectedAPIs.length; i++) {
        queryFilter += 'api:' + this.selectedAPIs[i].id + (this.selectedAPIs.length - 1 === i ? ')' : ' OR ');
      }
    }

    if (this.selectedApplications.length) {
      queryFilter = ' AND(';
      for (var i = 0; i < this.selectedApplications.length; i++) {
        queryFilter += 'application:' + this.selectedApplications[i].id + (this.selectedApplications.length - 1 === i ? ')' : ' OR ');
      }
    }

    _.forEach(this.analyticsData.tops, function (top) {
      _this.$scope.fetchData = true;
      var request = top.request.call(_this.AnalyticsService,
        _this.analyticsData.range.from,
        _this.analyticsData.range.to,
        _this.analyticsData.range.interval,
        top.key,
        top.query + queryFilter,
        top.field,
        top.orderField,
        top.orderDirection,
        top.orderType,
        top.size);

      request.then(response => {
        if (response.data && response.data.values) {
          if (Object.keys(response.data.values).length) {
            top.results = _.map(response.data.values, function (value, key) {
              return {topKey: key, topValue: value, model: top.field};
            });
            _this.$scope.paging[top.key] = 1;
          } else {
            delete top.results;
          }
        }
        _this.$scope.fetchData = false;
      });
    });
  }

  updateRangeDate() {
    var from = moment(this.beginDate).unix() * 1000;
    var to = moment(this.endDate).unix() * 1000;
    this.$state.go(this.$state.current, {timeframe: '', from: from, to: to}, {reload: true});
  }

  setRangeDate(from, to) {
    this.beginDate = moment.unix(from / 1000).toDate();
    this.endDate = moment.unix(to / 1000).toDate();

    this.analyticsData.range = {
      interval: 3600000,
      from: from,
      to: to
    };
  }

  updateTimeframe(timeframeId) {
    if (timeframeId) {
      this.$state.go(this.$state.current, {timeframe: timeframeId, from: '', to: ''}, {reload: true});
    }
  }

  setTimeframe(timeframeId) {
    var that = this;

    this.timeframe = _.find(this.analyticsData.timeframes, function (timeframe) {
      return timeframe.id === timeframeId;
    });

    var now = Date.now();

    _.assignIn(this.analyticsData, {
      timeframe: that.timeframe,
      range: {
        interval: that.timeframe.interval,
        from: now - that.timeframe.range,
        to: now
      }
    });
    this.beginDate = moment(now - this.timeframe.range).toDate();
    this.endDate = moment(now).toDate();
  }

  analytics() {
    return {
      tops: [
        {
          title: 'Top applications',
          titleKey: 'Application',
          titleValue: 'Hits',
          subhead: 'Order by API calls',
          request: this.AnalyticsService.topHits,
          key: "top-apps",
          query: "*:*",
          field: "application",
          size: 10000
        },
        {
          title: 'Top failed APIs',
          titleKey: 'API',
          titleValue: 'Hits',
          subhead: 'Order by API 5xx status calls',
          request: this.AnalyticsService.topHits,
          key: "top-failed-apis",
          query: "status:[500 TO 599]",
          field: "api",
          size: 10000
        },
        {
          title: 'Top slow APIs',
          titleKey: 'API',
          titleValue: 'Latency (in ms)',
          subhead: 'Order by API response time calls',
          request: this.AnalyticsService.topHits,
          key: "top-slow-apis",
          query: "*:*",
          field: "api",
          orderField: "response-time",
          orderDirection: "desc",
          orderType: "avg",
          size: 10000
        },
        {
          title: 'Top overhead APIs',
          titleKey: 'API',
          titleValue: 'Latency (in ms)',
          subhead: 'Order by proxy latency',
          request: this.AnalyticsService.topHits,
          key: "top-overhead-apis",
          query: "*:*",
          field: "api",
          orderField: "proxy-latency",
          orderDirection: "desc",
          orderType: "avg",
          size: 10000
        }
      ],
      timeframes: [
        {
          id: '1h',
          title: 'Last hour',
          range: 1000 * 60 * 60,
          interval: 1000 * 60
        },
        {
          id: '24h',
          title: 'Last day',
          range: 1000 * 60 * 60 * 24,
          interval: 1000 * 60 * 60
        },
        {
          id: '7d',
          title: 'Last week',
          range: 1000 * 60 * 60 * 24 * 7,
          interval: 1000 * 60 * 60 * 3
        },
        {
          id: '30d',
          title: 'Last month',
          range: 1000 * 60 * 60 * 24 * 30,
          interval: 10000000
        },
        {
          id: '1y',
          title: 'Last year',
          range: 1000 * 60 * 60 * 24 * 365,
          interval: 10000000
        }
      ]
    };
  }
}

export default DashboardController;
