db.apis.find().forEach(
    function(api) {
        var definition = JSON.parse(api.definition);
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
        
        db.apis.updateOne({ _id: api._id}, {$set: {definition: JSON.stringify(definition)}}, { upsert: true} );
    }
);
