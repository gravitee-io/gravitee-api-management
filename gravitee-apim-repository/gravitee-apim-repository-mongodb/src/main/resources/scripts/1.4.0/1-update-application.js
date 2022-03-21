db.applications.find().forEach(
    function(app) {
        db.applications.updateOne({_id: app._id}, {$set: {status: 'ACTIVE'}}, { upsert: true} );
    }
);
