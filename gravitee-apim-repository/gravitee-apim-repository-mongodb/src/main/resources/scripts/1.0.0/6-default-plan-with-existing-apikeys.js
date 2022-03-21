function guid() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

// For each API, create a default plan
db.apis.find().forEach(
    function(api) {
        var plan = {
            _id: guid(),
            _class: 'io.gravitee.repository.mongodb.management.internal.model.PlanMongo',
            name: api.name + ' Plan',
            description: 'Default ' + api.name + ' Plan',
            validation: 'AUTO',
            type: 'API',
            order: new NumberInt(1),
            apis: [api._id],
            definition: '{"/" : [ ]}',
            characteristics: [],
            createdAt: new Date(),
            updatedAt: new Date(),
        };
        
        db.plans.insertOne(plan);
    }
);

// For each api / application group, create a subscription
db.keys.group(
   {
     key: { 'application.$id': 1, 'api.$id': 1 },
     cond: { },
     reduce: function ( curr, result ) { },
     initial: { }
   }
).forEach(
    function(group) {
        var app = group['application.$id'];
        var api = group['api.$id'];
        
        var plan = db.plans.findOne({'apis': api, 'type': 'API'});
        
       // Get the plan relative to the API
       var subscription = {
           '_id': guid(),
            '_class' : "io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo",
            'plan' : plan._id,
            'application' : app,
            'api' : api,
            'status' : 'ACCEPTED',
            'processedAt' : new Date(),
            'processedBy' : "system",
            'subscribedBy' : "admin",
            'startingAt' : new Date(),
            'createdAt' : new Date(),
            'updatedAt' : new Date()
       };
       
       db.subscriptions.insertOne(subscription);
   }
);
   
// Now, existing api-keys must be based on previously created subscriptions
db.keys.find().forEach(
    function(apikey) {
        var app = apikey.application.$id;
        var api = apikey.api.$id;
        
        var subscription = db.subscriptions.findOne({'api': api, 'application': app});
        var apikey2 = {
            '_id' : apikey.key.key,
            '_class' : 'io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo',
            'subscription' : subscription._id,
            'application' : app,
            'plan' : subscription.plan,
            'createdAt' : apikey.key.createdAt,
            'updatedAt' : apikey.key.createdAt,
            'revoked' : apikey.key.revoked
        }
        
        if (apikey.key.expiration !== undefined) {
            apikey2.expireAt = apikey.key.expiration;
        } 
        
        db.keys.remove(apikey);
        db.keys.insertOne(apikey2);
    }
);
    
// Clear subscriptions
db.subscriptions.update(
    {},
    {$unset: {api: ''}},
    {multi: true}
);
