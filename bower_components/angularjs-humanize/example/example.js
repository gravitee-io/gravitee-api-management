(function (angular, humanize) {

     angular.module('example', ['ng', 'angular-humanize']).
        controller(
         'Example',
         ['$scope',
         function ($scope) {
             $scope.data = [
                {
                    name: 'bruce_timm.png',
                    filesize: 230757,
                    uploaded: 1405652728
                },
                {
                    name: 'joker.gif',
                    filesize: '76160',
                    uploaded: 1393729528
                }
             ];
         }]
     );

})(angular, window.humanize);
