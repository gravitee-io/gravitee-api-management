"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SubmenuComponent = {
    template: require('./submenu.html'),
    bindings: {
        allMenuItems: '<',
        reducedMode: '<'
    },
    require: {
        parent: '^gvSidenav'
    },
    controller: function (SidenavService, $filter, $transitions) {
        'ngInject';
        this.sidenavService = SidenavService;
        var that = this;
        $transitions.onSuccess({}, function () {
            that.reload();
        });
        this.$onInit = function () {
            that.reload();
        };
        this.reload = function () {
            that.submenuItems = $filter('currentSubmenus')(that.allMenuItems);
        };
    }
};
