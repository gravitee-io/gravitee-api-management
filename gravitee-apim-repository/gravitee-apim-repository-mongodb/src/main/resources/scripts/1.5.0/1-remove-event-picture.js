db.events.find().forEach(
    function(event) {
        var payload = JSON.parse(event.payload);
        if (payload.definition !== undefined) {
            var definition = JSON.parse(payload.definition);
            delete payload.picture;
            delete definition.picture;

            payload.definition = JSON.stringify(definition);

            db.events.updateOne({ _id: event._id}, {$set: {payload: JSON.stringify(payload)}}, { upsert: true} );
        }
    }
);