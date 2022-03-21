db.pages.find().forEach(
    function(page) {
        db.pages.updateOne({_id: page._id}, {$set: {homepage: false}}, { upsert: true} );
    }
);
