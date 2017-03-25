"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var InstanceComponent = {
    bindings: {
        instance: '<'
    },
    controller: function ($rootScope, SidenavService) {
        'ngInject';
        this.$onInit = function () {
            SidenavService.setCurrentResource(this.instance.hostname);
        };
    },
    template: require('./instance.html')
};
exports.default = InstanceComponent;
