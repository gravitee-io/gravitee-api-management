"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
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
var _ = require("lodash");
var ApiHistoryController = (function () {
    function ApiHistoryController($mdDialog, $scope, $rootScope, $state, ApiService, NotificationService, resolvedEvents) {
        'ngInject';
        this.$mdDialog = $mdDialog;
        this.$scope = $scope;
        this.$rootScope = $rootScope;
        this.$state = $state;
        this.ApiService = ApiService;
        this.NotificationService = NotificationService;
        this.resolvedEvents = resolvedEvents;
        this.api = _.cloneDeep(this.$scope.$parent.apiCtrl.api);
        this.events = resolvedEvents.data;
        this.eventsSelected = [];
        this.eventsTimeline = [];
        this.eventsToCompare = [];
        this.eventSelected = {};
        this.diffMode = false;
        this.eventToCompareRequired = false;
        this.eventTypes = "PUBLISH_API";
        this.cleanAPI();
        this.init();
        this.initTimeline(this.events);
    }
    ApiHistoryController.prototype.init = function () {
        var self = this;
        this.$scope.$parent.apiCtrl.checkAPISynchronization(self.api);
        this.$scope.$on("apiChangeSucceed", function () {
            if (self.$state.current.name.endsWith('history')) {
                // reload API
                self.api = _.cloneDeep(self.$scope.$parent.apiCtrl.api);
                self.cleanAPI();
                // reload API events
                self.ApiService.getApiEvents(self.api.id, self.eventTypes).then(function (response) {
                    self.events = response.data;
                    self.reloadEventsTimeline(self.events);
                });
            }
        });
        this.$scope.$on("checkAPISynchronizationSucceed", function () {
            self.reloadEventsTimeline(self.events);
        });
    };
    ApiHistoryController.prototype.initTimeline = function (events) {
        var self = this;
        _.forEach(events, function (event) {
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
    };
    ApiHistoryController.prototype.selectEvent = function (_eventTimeline) {
        if (this.eventToCompareRequired) {
            this.diff(_eventTimeline);
            this.selectEventToCompare(_eventTimeline);
        }
        else {
            this.diffMode = false;
            this.apisSelected = [];
            this.eventsSelected = [];
            this.clearDataToCompare();
            var idx = this.eventsSelected.indexOf(_eventTimeline);
            if (idx > -1) {
                this.eventsSelected.splice(idx, 1);
            }
            else {
                this.eventsSelected.push(_eventTimeline);
            }
            if (this.eventsSelected.length > 0) {
                var eventSelected = this.eventsSelected[0];
                if (eventSelected.isCurrentAPI) {
                    this.eventSelectedPayloadDefinition = eventSelected.event;
                }
                else {
                    this.eventSelectedPayload = JSON.parse(eventSelected.event.payload);
                    this.eventSelectedPayloadDefinition = this.reorganizeEvent(this.eventSelectedPayload);
                }
            }
        }
    };
    ApiHistoryController.prototype.selectEventToCompare = function (_eventTimeline) {
        this.eventsToCompare.push(_eventTimeline);
    };
    ApiHistoryController.prototype.clearDataToCompare = function () {
        this.eventsToCompare = [];
    };
    ApiHistoryController.prototype.clearDataSelected = function () {
        this.eventsSelected = [];
    };
    ApiHistoryController.prototype.isEventSelectedForComparaison = function (_event) {
        return this.eventsToCompare.indexOf(_event) > -1;
    };
    ApiHistoryController.prototype.diffWithMaster = function () {
        this.clearDataToCompare();
        this.diffMode = true;
        var latestEvent = this.events[0];
        if (this.eventsSelected.length > 0) {
            if (this.eventsSelected[0].isCurrentAPI) {
                this.right = this.eventsSelected[0].event;
                this.left = this.reorganizeEvent(JSON.parse(latestEvent.payload));
            }
            else {
                this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
                this.right = this.reorganizeEvent(JSON.parse(latestEvent.payload));
            }
        }
    };
    ApiHistoryController.prototype.enableDiff = function () {
        this.clearDataToCompare();
        this.eventToCompareRequired = true;
    };
    ApiHistoryController.prototype.disableDiff = function () {
        this.eventToCompareRequired = false;
    };
    ApiHistoryController.prototype.diff = function (eventTimeline) {
        this.diffMode = true;
        if (this.eventsSelected.length > 0) {
            if (eventTimeline.isCurrentAPI) {
                this.left = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
                this.right = eventTimeline.event;
            }
            else {
                var eventSelected = {};
                var event1UpdatedAt = eventTimeline.event.updated_at;
                var event2UpdatedAt = this.eventsSelected[0].event.updated_at;
                if (this.eventsSelected[0].isCurrentAPI) {
                    eventSelected = this.eventsSelected[0].event;
                }
                else {
                    eventSelected = this.reorganizeEvent(JSON.parse(this.eventsSelected[0].event.payload));
                }
                if (event1UpdatedAt > event2UpdatedAt) {
                    this.left = eventSelected;
                    this.right = this.reorganizeEvent(JSON.parse(eventTimeline.event.payload));
                }
                else {
                    this.left = this.reorganizeEvent(JSON.parse(eventTimeline.event.payload));
                    this.right = eventSelected;
                }
            }
        }
        this.disableDiff();
    };
    ApiHistoryController.prototype.isEventSelected = function (_eventTimeline) {
        return this.eventsSelected.indexOf(_eventTimeline) > -1;
    };
    ApiHistoryController.prototype.rollback = function (_apiPayload) {
        var _this = this;
        var _apiDefinition = JSON.parse(_apiPayload.definition);
        delete _apiDefinition.id;
        delete _apiDefinition.deployed_at;
        _apiDefinition.description = _apiPayload.description;
        _apiDefinition.visibility = _apiPayload.visibility;
        this.ApiService.rollback(this.api.id, _apiDefinition).then(function () {
            _this.NotificationService.show('Api rollback !');
            _this.$rootScope.$broadcast("apiChangeSuccess");
        });
    };
    ApiHistoryController.prototype.showRollbackAPIConfirm = function (ev, api) {
        ev.stopPropagation();
        var self = this;
        this.$mdDialog.show({
            controller: 'DialogConfirmController',
            controllerAs: 'ctrl',
            template: require('../../../components/dialog/confirm.dialog.html'),
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
    };
    ApiHistoryController.prototype.reloadEventsTimeline = function (events) {
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
    };
    ApiHistoryController.prototype.reorganizeEvent = function (_event) {
        var eventPayloadDefinition = JSON.parse(_event.definition);
        var reorganizedEvent = {
            "id": eventPayloadDefinition.id,
            "name": eventPayloadDefinition.name,
            "version": eventPayloadDefinition.version,
            "description": _event.description,
            "tags": eventPayloadDefinition.tags,
            "proxy": eventPayloadDefinition.proxy,
            "paths": eventPayloadDefinition.paths,
            "properties": eventPayloadDefinition.properties,
            "services": eventPayloadDefinition.services,
            "resources": eventPayloadDefinition.resources
        };
        return reorganizedEvent;
    };
    ApiHistoryController.prototype.cleanAPI = function () {
        delete this.api.deployed_at;
        delete this.api.created_at;
        delete this.api.updated_at;
        delete this.api.visibility;
        delete this.api.state;
        delete this.api.permission;
        delete this.api.owner;
        delete this.api.picture_url;
        delete this.api.views;
    };
    return ApiHistoryController;
}());
exports.default = ApiHistoryController;
