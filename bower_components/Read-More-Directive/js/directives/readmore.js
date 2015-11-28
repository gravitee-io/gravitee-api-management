/**
 * Created by Mariandi on 16/04/2014.
 *
 * This directive truncates the given text according to the specified length (n)
 * If words = true then the directive will display n number of words
 * If words = false then the directive will display n number of characters
 * If words flag is omitted then default behaviour is that words == true
 *
 */

/*global angular*/
var readMore = angular.module('readMore', []);

readMore.directive('readMore', function() {
    return {
        restrict: 'AE',
        replace: true,
        scope: {
            text: '=ngModel'
        },
        template:  "<p> {{text | readMoreFilter:[text, countingWords, textLength] }}" +
            "<a ng-show='showLinks' ng-click='changeLength()' class='color3'>" +
            "<strong ng-show='isExpanded'>  Show Less</strong>" +
            "<strong ng-show='!isExpanded'>  Show More</strong>" +
            "</a>" +
            "</p>",
        controller: ['$scope', '$attrs', '$element',
            function($scope, $attrs) {
                $scope.textLength = $attrs.length;
                $scope.isExpanded = false; // initialise extended status
                $scope.countingWords = $attrs.words !== undefined ? ($attrs.words === 'true') : true; //if this attr is not defined the we are counting words not characters

                if (!$scope.countingWords && $scope.text.length > $attrs.length) {
                    $scope.showLinks = true;
                } else if ($scope.countingWords && $scope.text.split(" ").length > $attrs.length) {
                    $scope.showLinks = true;
                } else {
                    $scope.showLinks = false;
                }

                $scope.changeLength = function (card) {
                    $scope.isExpanded = !$scope.isExpanded;
                    $scope.textLength = $scope.textLength !== $attrs.length ?  $attrs.length : $scope.text.length;
                };
            }]
    };
});
readMore.filter('readMoreFilter', function() {
    return function(str, args) {
        var strToReturn = str,
            length = str.length,
            foundWords = [],
            countingWords = (!!args[1]);

        if (!str || str === null) {
            // If no string is defined return the entire string and warn user of error
            console.log("Warning: Truncating text was not performed as no text was specified");
        }

        // Check length attribute
        if (!args[2] || args[2] === null) {
            // If no length is defined return the entire string and warn user of error
            console.log("Warning: Truncating text was not performed as no length was specified");
        } else if (typeof args[2] !== "number") { // if parameter is a string then cast it to a number
            length = Number(args[2]);
        }

        if (length <= 0) {
            return "";
        }


        if (str) {
            if (countingWords) { // Count words

                foundWords = str.split(/\s+/);

                if (foundWords.length > length) {
                    strToReturn = foundWords.slice(0, length).join(' ') + '...';
                }

            } else {  // Count characters

                if (str.length > length) {
                    strToReturn = str.slice(0, length) + '...';
                }

            }
        }

        return strToReturn;
    };
});
