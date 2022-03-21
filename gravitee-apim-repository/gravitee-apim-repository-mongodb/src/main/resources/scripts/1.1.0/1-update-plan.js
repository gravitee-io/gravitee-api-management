db.plans.find().forEach(
    function(plan) {
        db.plans.updateOne({_id: plan._id}, {$set: {status: 'PUBLISHED'}}, { upsert: true} );
    }
);
