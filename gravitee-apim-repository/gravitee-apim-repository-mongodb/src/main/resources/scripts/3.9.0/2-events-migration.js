print('Migrate events to reference multiple environments');
// Override this variable if you use prefix
const prefix = '';

const events = db.getCollection(`${prefix}events`);

events.find({environments: {$exists: false}}).forEach((event) => {
        event.environments = [event.environmentId];
        delete event.environmentId;
        events.replaceOne({ _id: event.id }, event);
    }
);
