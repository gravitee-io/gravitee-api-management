"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var RoleController = (function () {
    function RoleController(UserService) {
        'ngInject';
        this.UserService = UserService;
    }
    return RoleController;
}());
var RoleDirective = ({
    restrict: 'AE',
    link: function (scope, elem, attr, ctr) {
        var roles = attr['graviteeRolesAllowed'].replace(/ /g, '').split(',');
        if (!(ctr.UserService.isUserInRoles(roles))) {
            elem.css('display', 'none');
        }
    },
    controller: RoleController,
});
exports.default = RoleDirective;
