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
import * as angular from 'angular';
import { StateService } from '@uirouter/core';

class ApiHistoryController {
  private api: any;
  private events: any;
  private eventsSelected: any;
  private eventsTimeline: any;
  private eventsToCompare: any;
  private eventSelected: any;
  private diffMode: boolean;
  private eventToCompareRequired: boolean;
  private eventTypes: string;
  private apisSelected: any;
  private eventSelectedPayloadDefinition: any;
  private eventSelectedPayload: any;
  private right: any;
  private left: any;

  constructor (
    private $mdDialog: ng.material.IDialogService,
    private $scope: any,
    private $rootScope: ng.IRootScopeService,
    private $state: StateService,
    private ApiService,
    private NotificationService,
    private resolvedEvents,
    private $timeout
  ) {
    'ngInject';
    this.api = JSON.parse(angular.toJson(_.cloneDeep(this.$scope.$parent.apiCtrl.api)));
    this.events = resolvedEvents.data;
    this.eventsSelected = [];
    this.eventsTimeline = [];
    this.eventsToCompare = [];
    this.eventSelected = {};
    this.diffMode = false;
    this.eventToCompareRequired = false;
    this.eventTypes = 'PUBLISH_API';

    this.cleanAPI(this.api);
    this.init();
    this.initTimeline(this.events);
  }

  init() {
    var self = this;
    this.$scope.$parent.apiCtrl.checkAPISynchronization(self.api);
    this.$scope.$on('apiChangeSuccess', function(event, args) {
      if (self.$state.current.name.endsWith('history')) {
        // reload API
        self.api = JSON.parse(angular.toJson(_.cloneDeep(_.cloneDeep(args.api))));
        self.cleanAPI(self.api);
        // reload API events
        self.ApiService.getApiEvents(self.api.id, self.eventTypes).then(response => {
          self.events = response.data;
          self.reloadEventsTimeline(self.events);
        });
      }
    });
    this.$scope.$on('checkAPISynchronizationSucceed', function() {
      self.reloadEventsTimeline(self.events);
    });
  }

  initTimeline(events) {
    var self = this;
    _.forEach(events, function(event) {
      var eventTimeline = {
        event: event,
        badgeClass: 'info',
        badgeIconClass: 'glyphicon-check',
        title: event.type,
        when: event.created_at,
        user: event.user
      };
      self.eventsTimeline.push(eventTimeline);
    });
  }

  selectEvent(_eventTimeline) {
    if (this.eventToCompareRequired) {
      this.diff(_eventTimeline);
      this.selectEventToCompare(_eventTimeline);
    } else {
      this.diffMode = false;
      this.apisSelected = [];
      this.eventsSelected = [];
      this.clearDataToCompare();

      var idx = this.eventsSelected.indexOf(_eventTimeline);
      if (idx > -1) {
        this.eventsSelected.splice(idx, 1);
      } else {
        this.eventsSelected.push(_eventTimeline);
      }

      if (this.eventsSelected.length > 0) {
        var eventSelected = this.eventsSelected[0];
        if (eventSelected.isCurrentAPI) {
          this.eventSelectedPayloadDefinition = eventSelected.event;
        } else {
          this.eventSelectedPayload = JSON.parse(eventSelected.event.payload);
          this.eventSelectedPayloadDefinition = this.reorganizeEvent(this.eventSelectedPayload);
        }

        this.cleanAPI(this.eventSelectedPayloadDefinition);
      }
    }
  }

  selectEventToCompare(_eventTimeline) {
    this.eventsToCompare.push(_eventTimeline);
  }

  clearDataToCompare() {
    this.eventsToCompare = [];
  }

  clearDataSelected() {
    this.eventsSelected = [];
  }

  isEventSelectedForComparaison(_event) {
    return this.eventsToCompare.indexOf(_event) > -1;
  }

  diffWithMaster() {
    this.clearDataToCompare();
    this.diffMode = true;
    var latestEvent = this.events[0];
    if (this.eventsSelected.length > 0) {
      if (this.eventsSelected[0].isCurrentAPI) {
        this.right = this.eventsSelected[0].event;
        this.left = this.reorganizeEvent(JSON.parse(latestEvent.payload));
      } else {
        this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
        this.right = this.reorganizeEvent(JSON.parse(latestEvent.payload));
      }
    }
  }

  enableDiff() {
    this.clearDataToCompare();
    this.eventToCompareRequired = true;
  }

  disableDiff() {
    this.eventToCompareRequired = false;
  }

  diff(eventTimeline) {
    this.diffMode = true;
    if (this.eventsSelected.length > 0) {
      if (eventTimeline.isCurrentAPI) {
        this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
        this.right = eventTimeline.event;
      } else {
        var eventSelected = {};
        var event1UpdatedAt = eventTimeline.event.updated_at;
        var event2UpdatedAt = this.eventsSelected[0].event.updated_at;

        if (this.eventsSelected[0].isCurrentAPI) {
          eventSelected = this.eventsSelected[0].event;
        } else {
          eventSelected = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
        }

        if (event1UpdatedAt > event2UpdatedAt) {
          this.left = eventSelected;
          this.right = this.reorganizeEvent(JSON.parse(eventTimeline.event.payload));
        } else {
          this.left = this.reorganizeEvent(JSON.parse(eventTimeline.event.payload));
          this.right = eventSelected;
        }
      }
    }
    this.disableDiff();
  }

  isEventSelected(_eventTimeline) {
    return this.eventsSelected.indexOf(_eventTimeline) > -1;
  }

  rollback(_apiPayload) {
    var _apiDefinition = JSON.parse(_apiPayload.definition);
    delete _apiDefinition.id;
    delete _apiDefinition.deployed_at;
    _apiDefinition.description = _apiPayload.description;
    _apiDefinition.visibility = _apiPayload.visibility;

    const that = this;

    that.ApiService.rollback(this.api.id, _apiDefinition).then( () => {
      that.NotificationService.show('Api rollback !');

      that.ApiService.get(that.api.id).then(function (response) {
        that.$rootScope.$broadcast('apiChangeSuccess', {api: response.data});
      });
    });
  }

  showRollbackAPIConfirm(ev, api) {
    ev.stopPropagation();
    let self = this;
    this.$mdDialog.show({
      controller: 'DialogConfirmController',
      controllerAs: 'ctrl',
      template: require('../../../../components/dialog/confirm.dialog.html'),
      clickOutsideToClose: true,
      locals: {
        title: 'Would you like to rollback your API ?',
        confirmButton: 'Rollback'
      }
    }).then(function (response) {
      if (response) {
        self.rollback(api);
      }
    });
  }

  reloadEventsTimeline(events) {
    this.clearDataSelected();
    this.eventsTimeline = [];
    this.initTimeline(events);
    if (!this.$scope.$parent.apiCtrl.apiIsSynchronized && !this.$scope.$parent.apiCtrl.apiJustDeployed) {
      var eventTimeline = {
          event: this.api,
          badgeClass: 'warning',
          badgeIconClass: 'glyphicon-refresh',
          title: 'TO_DEPLOY',
          isCurrentAPI: true
        };
      this.eventsTimeline.unshift(eventTimeline);
    }
  }

  reorganizeEvent(_event) {
    var eventPayloadDefinition = JSON.parse(_event.definition);
    var reorganizedEvent = {
      'id': eventPayloadDefinition.id,
      'name': eventPayloadDefinition.name,
      'version': eventPayloadDefinition.version,
      'description': _event.description,
      'tags': eventPayloadDefinition.tags,
      'proxy': eventPayloadDefinition.proxy,
      'paths': eventPayloadDefinition.paths,
      'properties': eventPayloadDefinition.properties,
      'services': eventPayloadDefinition.services,
      'resources': eventPayloadDefinition.resources,
      'path_mappings': eventPayloadDefinition.path_mappings,
      'response_templates': eventPayloadDefinition.response_templates
    };
    return reorganizedEvent;
  }

  cleanAPI(api) {
    delete api.deployed_at;
    delete api.created_at;
    delete api.updated_at;
    delete api.visibility;
    delete api.state;
    delete api.permission;
    delete api.owner;
    delete api.picture_url;
    delete api.categories;
    delete api.groups;
    delete api.etag;
    delete api.context_path;
  }
}

export default ApiHistoryController;
