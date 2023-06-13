// Override this variable if you use prefix
const prefix = "";

print("Rename subscription type from SUBSCRIPTION to PUSH in Subscriptions collection");
let subscriptionsCollection = db.getCollection(`${prefix}subscriptions`);
subscriptionsCollection.find({ type: "SUBSCRIPTION" }).forEach((subscription) => {
    subscription.type = "PUSH";
    subscriptionsCollection.replaceOne({ _id: subscription._id }, subscription);
});

print("Define mode field for Plans collection");
let plansCollection = db.getCollection(`${prefix}plans`);
plansCollection.find({}).forEach((plan) => {
    if (plan.security == "SUBSCRIPTION") {
        plan.mode = "PUSH";
        plan.security = null;
    } else {
        plan.mode = "STANDARD";
    }
    plansCollection.replaceOne({ _id: plan._id }, plan);
});

print("Update events collection");
let eventsCollection = db.getCollection(`${prefix}events`);
eventsCollection.find({ type: "PUBLISH_API" }).forEach((event) => {
    let payloadObj = JSON.parse(event.payload);
    if (payloadObj.definitionVersion == "4.0.0") {
        let definition = JSON.parse(payloadObj.definition);
        if (definition.plans) {
            definition.plans.forEach((plan) => {
                if (plan.security && plan.security.type.toLowerCase() == "subscription") {
                    plan.security = null;
                    plan.mode = "PUSH";
                } else {
                    plan.mode = "STANDARD";
                }
            });
            payloadObj.definition = JSON.stringify(definition);
        }
        event.payload = JSON.stringify(payloadObj);
        eventsCollection.replaceOne({ _id: event._id }, event);
    }
});

print("Update events_latest collection");
let eventsLatest = db.getCollection(`${prefix}events_latest`);
eventsLatest.find({ type: "PUBLISH_API" }).forEach((event) => {
    let payloadObj = JSON.parse(event.payload);
    if (payloadObj.definitionVersion == "4.0.0") {
        let definition = JSON.parse(payloadObj.definition);
        if (definition.plans) {
            definition.plans.forEach((plan) => {
                if (plan.security && plan.security.type.toLowerCase() == "subscription") {
                    plan.security = null;
                    plan.mode = "PUSH";
                } else {
                    plan.mode = "STANDARD";
                }
            });
            payloadObj.definition = JSON.stringify(definition);
        }
        event.payload = JSON.stringify(payloadObj);
        eventsLatest.replaceOne({ _id: event._id }, event);
    }
});
