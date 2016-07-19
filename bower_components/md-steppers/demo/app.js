var app = angular.module('app', ['ngMaterial', 'md-steppers']);

app.controller('MainCtrl', function ($scope, $q, $timeout) {

    var vm = this;

    vm.selectedStep = 0;
    vm.stepProgress = 1;
    vm.maxStep = 3;
    vm.showBusyText = false;
    vm.stepData = [
        { step: 1, completed: false, optional: false, data: {} },
        { step: 2, completed: false, optional: false, data: {} },
        { step: 3, completed: false, optional: false, data: {} },
    ];

    vm.enableNextStep = function nextStep() {
        //do not exceed into max step
        if (vm.selectedStep >= vm.maxStep) {
            return;
        }
        //do not increment vm.stepProgress when submitting from previously completed step
        if (vm.selectedStep === vm.stepProgress - 1) {
            vm.stepProgress = vm.stepProgress + 1;
        }
        vm.selectedStep = vm.selectedStep + 1;
    }

    vm.moveToPreviousStep = function moveToPreviousStep() {
        if (vm.selectedStep > 0) {
            vm.selectedStep = vm.selectedStep - 1;
        }
    }

    vm.submitCurrentStep = function submitCurrentStep(stepData, isSkip) {
        var deferred = $q.defer();
        vm.showBusyText = true;
        console.log('On before submit');
        if (!stepData.completed && !isSkip) {
            //simulate $http
            $timeout(function () {
                vm.showBusyText = false;
                console.log('On submit success');
                deferred.resolve({ status: 200, statusText: 'success', data: {} });
                //move to next step when success
                stepData.completed = true;
                vm.enableNextStep();
            }, 1000)
        } else {
            vm.showBusyText = false;
            vm.enableNextStep();
        }
    }

});
