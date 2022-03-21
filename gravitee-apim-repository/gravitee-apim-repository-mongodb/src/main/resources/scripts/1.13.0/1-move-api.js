print("\n\Move API reference into the subscription.");

db.subscriptions.find().forEach(
    function (subscription) {
        var plan = db.plans.findOne({'_id': subscription.plan});
        if (plan !== null) {
            if (subscription.api) {
                print("Subscription id["+subscription._id+"] already associated to an API. Skipping.");
            } else {
                db.subscriptions.updateOne(
                    {_id: subscription._id},
                    {
                      $set:
                        {
                          api: plan.apis[0],
                        }
                    },
                    {upsert: false}
                  );
            }
        } else {
            print('No plan for subscription: id[' + subscription._id + '] plan[' + subscription.plan + ']')
        }
    }
);
