db.apis.find().forEach(
    function(api) {
        var definition = JSON.parse(api.definition);
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
        
        db.apis.updateOne({ _id: api._id}, {$set: {definition: JSON.stringify(definition)}}, { upsert: true} );
    }
);
