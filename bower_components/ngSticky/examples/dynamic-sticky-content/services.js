angular.module('appService', ['ngRoute'])

    // data struct?
    .service('city', [function() {

        this.woeid;
        this.name;
        this.links;
        this.data = {
            weather: null,
            image: null
        };

    }])


    .factory('citiesLibrary', ['$http', '$q', '$filter', function($http, $q, $filter) {

        var flickrApiKey = "fa214b1215cd1a537018cfbdfa7fb9a6"; // demo key, you ought to use your *own*

        return {

            fetchingConfiguration: function() {
                // yields a promise
                return $http.get ('data/cities.json');
            },


            fetchingCityById: function(woeid) {

                return $http.get("http://query.yahooapis.com/v1/public/yql", {
                    params: {
                        q: "select item.condition from weather.forecast where woeid = " + woeid,
                        format: "json",
                        env: "store://datatables.org/alltableswithkeys"
                        //, rnd: parseInt (Math.random() * 100000)
                    }
                });

            },


            fetchingFlickrPhotoMatchesByTag: function(tag) {

                // narrow down on the kind of photo that works well for demoing
                var photoAssetType = "people";

                var p = $http.get(
                    "https://api.flickr.com/services/rest/",
                    {
                        params: {
                            api_key: flickrApiKey,
                            method: 'flickr.photos.search',
                            format: 'json',
                            nojsoncallback: 1,
                            tags: tag + " " + photoAssetType,
                            per_page: 5 // keep it light
                        }
                    });

                //     .success(function(data) { });


                return p;

            },


            // grab some photo by luck, provided a previous response ( of potential matches )
            fetchingFlickrRandomPhotoReferenceByResponse: function(responseData) {

                if (!responseData.photos.photo.length) {
                    console.error("nothing matched or service error")
                    return;
                }

                // make up your mind, pick a pic
                var photo = responseData.photos.photo[Math.floor(Math.random() * responseData.photos.photo.length)];

                return $http.get(
                    "https://api.flickr.com/services/rest/",
                    {
                        params: {
                            api_key: flickrApiKey,
                            method: 'flickr.photos.getSizes',
                            format: 'json',
                            photo_id: photo.id,
                            nojsoncallback: 1
                        }
                    });

            },


            // populate the city with data feeds
            initializingCity: function(city) {

                // 1st data service - weather

                var p1 = this.fetchingCityById(city.id)
                    .success(function(data) {
                        try {
                            city.data.weather = data.query.results.channel.item.condition;

                        } catch (e) {
                            console.error("couldn't parse", data);
                        }

                    });


                // 2nd data service - photo image
                var d2 = $q.defer();

                this.fetchingFlickrPhotoMatchesByTag(city.name)
                    .success((function(data) {

                        this.fetchingFlickrRandomPhotoReferenceByResponse(data)
                            .success((function(data) {

                                // guessing at least 3 image sizes as fail over
                                var preferredSizeLabelsList = ["Large Square", "Square", "Small"];

                                try {

                                    preferredSizeLabelsList.every(function(dimensionType) {

                                        // exact extraction of the square shaped image
                                        var squareImagesFetch = $filter ('filter') (data.sizes.size, {label: dimensionType}, true);
                                        // got a good image that I prefer to render
                                        if (squareImagesFetch && squareImagesFetch.length) {

                                            // memorize good image
                                            city.data.image = squareImagesFetch[0];
                                            // really complete
                                            d2.resolve();
                                            return false;
                                        }

                                        // loop more
                                        return true;
                                    });


                                } catch (e) {
                                    console.error("couldn't parse", data, e);
                                }

                            }).bind(this));


                    }).bind(this));


                // combined network actions
                return $q.all([p1, d2.promise]);
                // /initializingCity
            }



            // /factory def
        };

    }])


    // physical to logical path map
    .config(["$routeProvider", function($routeProvider) {

        $routeProvider
            .when('/', {
                templateUrl: 'views/home.html',
                controller: 'homeCtrl',
                controllerAs: 'home',

                resolve: {

                    // DI - link the cities factory
                    "citiesResponse": ["citiesLibrary", function(citiesLibrary) {
                        return citiesLibrary.fetchingConfiguration();
                    }]

                }
            })


            .otherwise({
                redirectTo: '/'
            });

    }]);


