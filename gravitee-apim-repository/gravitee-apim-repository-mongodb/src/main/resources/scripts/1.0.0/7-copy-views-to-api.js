db.apis.find().forEach(
    function(api) {
        var definition = JSON.parse(api.definition);
        var views = definition.views;
        
        delete definition.views;

        db.apis.updateOne({ _id: api._id}, {$set: {views: views, definition: JSON.stringify(definition)}}, { upsert: true} );
    }
);
