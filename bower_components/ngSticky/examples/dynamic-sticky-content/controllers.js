angular.module('app', ['sticky', 'appService'])


    // main flow starts
    .controller('homeCtrl', ["citiesResponse", "citiesLibrary", "city", "$filter", "$q", function(citiesResponse, citiesLibrary, city, $filter, $q) {
        this.cities = citiesResponse.data.items;
        this.city = city; // scoped
        this.isInitialized = false; // UI not fully ready
        //console.log("populated cities", this.cities);

        var randomCityResponse = this.cities[parseInt (Math.random() * this.cities.length)];
        // after picking a city, assign the members
        city.id = randomCityResponse.woeid; // special where on earth id convention
        city.links = randomCityResponse.links; // external page links to city
        city.name = randomCityResponse.name;

        // -- state variables
        this.changeCityCount = 0; // how many times has change city been clicked


        // ((slower)) setup other relevant metadata by pulling from the web
        this.initializing = function() {
            // async
            var p = $q.defer();
            // a subtask
            citiesLibrary.initializingCity(city).then((function() {
                this.isInitialized = true;
                p.resolve();
            }).bind(this));
            return p;
        };

        this.getWeatherImageSrc = function() {

            // weather is async
            if (this.city.data && this.city.data.weather) {

                return [
                    "http://l.yimg.com/a/i/us/we/52/",
                    this.city.data.weather.code,
                    ".gif"].join("");
            }
        };

        // temp in Celsius
        this.getWeatherTemperatureCelsius = function() {

            // weather is async
            if (this.city.data && this.city.data.weather) {

                return ((this.city.data.weather.temp - 32) / 1.8 ) | 0;
            }
        };

        // ~~~~~~~~~ user interaction ~~~~~~~~~~

        /**
         * from the actions toolbar, reset state event
         * Change to a new city altogether.
         */
        this.onChangeCity = function() {
            this.isInitialized = false; // UI not fully ready

            var randomCityResponse = this.cities[parseInt (Math.random() * this.cities.length)];
            // reusing city object
            city.id = randomCityResponse.woeid; // special where on earth id convention
            city.links = randomCityResponse.links; // external page links to city
            city.name = randomCityResponse.name;

            // RE-initializing flow
            this.initializing().promise.then((function(promises) {
                // post init processing
                this.changeCityCount++;
                console.log("new city dependencies all loaded for", city);

                // to make it interesting lets insert random text in the sticky to test its ability to resize
                var expressionsList = [
                    "A great city for travelling",
                    "Wonderful cultural cuisines",
                    "Nature and wildlife",
                    "People and artworks",
                    "Celebrate in top teams in sports and activities",
                    "Historic and ancient momuments"
                ];

                this.contentAfterChangeCity = expressionsList[parseInt (Math.random() * expressionsList.length)];


            }).bind(this));


        };

        // ____________ initialize ______________

        this.initializing().promise.then((function(promises) {
            // post init processing

        }).bind(this));


    }])


    .filter('trustAsResourceUrl', ['$sce', function($sce) {
        return function(val) {
            return $sce.trustAsResourceUrl(val);
        };

    }]);
