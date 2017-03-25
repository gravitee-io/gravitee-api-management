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
var ApiEventsController = (function () {
    function ApiEventsController(resolvedEvents) {
        'ngInject';
        this.events = resolvedEvents.data;
        this.eventsTimeline = [];
        this.initTimeline(this.events);
    }
    ApiEventsController.prototype.initTimeline = function (events) {
        var _this = this;
        _.forEach(events, function (event) {
            var eventTimelineType = _this.getEventTypeTimeline(event.type);
            var eventTimeline = {
                event: event,
                badgeClass: eventTimelineType.badgeClass,
                badgeIconClass: eventTimelineType.icon,
                title: event.type,
                when: event.created_at,
                username: event.properties.username
            };
            _this.eventsTimeline.push(eventTimeline);
        });
    };
    ApiEventsController.prototype.reloadEventsTimeline = function (events) {
        this.eventsTimeline = [];
        this.initTimeline(events);
    };
    ApiEventsController.prototype.getEventTypeTimeline = function (eventType) {
        var eventTypeTimeline = {};
        switch (eventType) {
            case 'start_api':
                eventTypeTimeline.icon = "glyphicon-play";
                eventTypeTimeline.badgeClass = "info";
                break;
            case 'stop_api':
                eventTypeTimeline.icon = "glyphicon-stop";
                eventTypeTimeline.badgeClass = "danger";
                break;
            default:
                eventTypeTimeline.icon = "glyphicon-check";
                eventTypeTimeline.badgeClass = "info";
        }
        return eventTypeTimeline;
    };
    return ApiEventsController;
}());
exports.default = ApiEventsController;
