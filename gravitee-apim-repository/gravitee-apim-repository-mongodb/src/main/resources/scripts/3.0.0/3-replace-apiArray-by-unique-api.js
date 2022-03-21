print('Plans migration - plans have now only one api');
db.plans.dropIndexes();
db.plans.find().forEach(
    function(plan) {
        
        var apiId = plan.apis && plan.apis[0];
         
        if(apiId) {
            db.plans.updateOne(
                { _id: plan._id },
                {
                    $set: { api: apiId},
                    $unset: { apis: '' }
                }
            );
        }
        
    }
);
db.plans.createIndex( { "api" : 1 } );
db.plans.reIndex();
