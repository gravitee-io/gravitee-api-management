db.apis.find().forEach(
    function(api) {
        db.events.find({'type': 'PUBLISH_API', 'properties.api_id': api._id}).sort({'createdAt':-1}).limit(1).forEach(
            function(event) {
                    var payload = JSON.parse(event.payload);
                    var definition = JSON.parse(payload.definition);

                    var dumpRequest = false;

                    if (definition.proxy.http !== undefined) {
                        dumpRequest = definition.proxy.http.configuration.dumpRequest;
                    }
		    
		    if (definition.proxy.endpoints !== undefined) {
                    for (var i = 0 ; i < definition.proxy.endpoints.length ; i++) {
                        var endpoint = definition.proxy.endpoints[i];
                        endpoint.name = 'endpoint_' + i;

                        if (definition.proxy.http !== undefined) {
                            endpoint.http = definition.proxy.http.configuration;

                            if (definition.proxy.http.http_proxy) {
                                endpoint.proxy = definition.proxy.http.http_proxy;
                            }

                            if (definition.proxy.http.ssl) {
                                endpoint.ssl = definition.proxy.http.ssl;
                            }

                            delete endpoint.http.dumpRequest;
                        }
                    }
		    }
		    
 		    delete definition.proxy.http;
                    if (dumpRequest) {
                        definition.proxy.dumpRequest = dumpRequest;
                    }
                    
                    var paths = Object.keys(definition.paths);

                    for (var i = 0 ; i < paths.length ; i++) {
                        var path = definition.paths[paths[i]];
                        var apiKey = -1;

                        for (var j = 0 ; j < path.length ; j++) {
                            var fields = Object.keys(path[j]);
                            for(var k = 0 ; k < fields.length ; k++) {
                                if (fields[k] === 'api-key') {
                                    apiKey = j;
                                    break;
                                }
                            }

                        }

                        if (apiKey !== -1) {
                            path.splice(apiKey, 1);
                        }
                    }

                    payload.definition = JSON.stringify(definition);
                    db.events.updateOne({ _id: event._id}, {$set: {payload: JSON.stringify(payload)}}, { upsert: true} );
            }
        );
    }
);
