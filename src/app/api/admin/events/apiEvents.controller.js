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
class ApiEventsController {
  constructor ($mdDialog, $scope, ApiService, NotificationService, resolvedApi, resolvedEvents) {
    'ngInject';
    this.$mdDialog = $mdDialog;
    this.$scope = $scope;
    this.ApiService = ApiService;
    this.NotificationService = NotificationService;
    this.api = _.cloneDeep(resolvedApi.data);
    this.events = resolvedEvents.data;
    this.eventsSelected = [];
    this.eventsTimeline = [];
    this.eventSelected = {};
    this.apisSelected = [];
    this.diffMode = false;
    this.eventToCompareRequired = false;
    
    delete this.api.deployed_at;
    delete this.api.created_at;
    delete this.api.updated_at;
    delete this.api.visibility;
    delete this.api.state;
    delete this.api.permission;
    delete this.api.owner;
    
    this.initTimeline(this.events);
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
        username: event.properties.username
      };
      self.eventsTimeline.push(eventTimeline);
    });
  }
  
  selectApi(_api) {
    this.diffMode = false;
    this.apisSelected = [];
    this.eventsSelected = [];
    
    var idx = this.apisSelected.indexOf(_api);
    if (idx > -1) {
      this.apisSelected.splice(idx, 1);
    }
    else {
      this.apisSelected.push(_api);
    }
    
    if (this.apisSelected.length > 0) {
      this.eventSelectedPayloadDefinition = _api;
    }
  }
  
  selectEvent( _event) {
    if (this.eventToCompareRequired) {
      this.diff(_event);
    } else {
      this.diffMode = false;
      this.apisSelected = [];
      this.eventsSelected = [];
      
      var idx = this.eventsSelected.indexOf(_event);
      if (idx > -1) {
        this.eventsSelected.splice(idx, 1);
      }
      else {
        this.eventsSelected.push(_event);
      }
      
      if (this.eventsSelected.length > 0) {
        this.eventSelected = this.eventsSelected[0];
        this.eventSelectedPayload = JSON.parse(this.eventSelected.payload);
        this.eventSelectedPayloadDefinition = this.reorganizeEvent(this.eventSelectedPayload);
      }
    }
  }
  
  diffWithMaster() {
    // get published api
    this.diffMode = true;
    var latestEvent = this.events[0];
    if (this.eventsSelected.length > 0) {
      this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].payload));
      this.right = this.reorganizeEvent(JSON.parse(latestEvent.payload));
    } else if (this.apisSelected.length > 0) {
      this.left = this.reorganizeEvent(JSON.parse(latestEvent.payload));
      this.right = this.apisSelected[0];
    }
  }
  
  enableDiff() {
    this.eventToCompareRequired = true;
  }

  disableDiff() {
    this.eventToCompareRequired = false;
  }
  
  diff(event) {
    this.diffMode = true;
    var latestEvent = this.events[0];
    if (this.eventsSelected.length > 0) {
      var event1UpdatedAt = event.updated_at;
      var event2UpdatedAt = this.eventsSelected[0].updated_at;
      if (event1UpdatedAt > event2UpdatedAt) {
        this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].payload));
        this.right = this.reorganizeEvent(JSON.parse(event.payload));
      } else {
        this.left = this.reorganizeEvent(JSON.parse(event.payload));
        this.right = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].payload));
      }
    } else if (this.apisSelected.length > 0) {
      this.left = this.reorganizeEvent(JSON.parse(event.payload));
      this.right = this.apisSelected[0];
    }
    this.disableDiff();
  }
  
  isEventSelected(_event) {
    return this.eventsSelected.indexOf(_event) > -1;
  }
  
  isApiSelected(_api) {
    return this.apisSelected.indexOf(_api) > -1;
  }
  
  enableDiffMode() {
    this.diffMode = true;
  }
  
  rollback(_apiPayload) {
    var _apiDefinition = JSON.parse(_apiPayload.definition);
    delete _apiDefinition.id;
    delete _apiDefinition.deployed_at;
    _apiDefinition.description = _apiPayload.description;
    _apiDefinition.visibility = _apiPayload.visibility;

    this.ApiService.rollback(this.api.id, _apiDefinition).then(() => {
      this.NotificationService.show('Api rollback !');
    });
  }
  
  showRollbackAPIConfirm(ev, api) {
    var confirm = this.$mdDialog.confirm()
      .title('Would you like to rollback your API?')
      .ariaLabel('rollback-api')
      .ok('OK')
      .cancel('Cancel')
      .targetEvent(ev);
    var self = this;
    this.$mdDialog.show(confirm).then(function() {
      self.rollback(api);
    }, function() {
      self.$mdDialog.cancel();
    });
  }
  
  reorganizeEvent(_event) {
    var eventPayloadDefinition = JSON.parse(_event.definition);
    var reorganizedEvent = {
      "id": eventPayloadDefinition.id,
      "name": eventPayloadDefinition.name,
      "version": eventPayloadDefinition.version,
      "description": _event.description,
      "tags": eventPayloadDefinition.tags,
      "proxy": eventPayloadDefinition.proxy,
      "paths": eventPayloadDefinition.paths,
      "deployed_at": _event.deployedAt,
      "properties": eventPayloadDefinition.properties
    };
    
    return reorganizedEvent;
  }
  
}

export default ApiEventsController;