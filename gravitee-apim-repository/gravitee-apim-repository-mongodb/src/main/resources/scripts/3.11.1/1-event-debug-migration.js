print('Remove the `API_ID` property for events of type `DEBUG_API`');
// Override this variable if you use prefix
const prefix = '';

const events = db.getCollection(`${prefix}events`);

events.find({type: "DEBUG_API"}).forEach((event) => {
  if(event.properties.api_id) {
    delete event.properties.api_id;
    events.replaceOne({ _id: event.id }, event);
  }
});

print('`DEBUG_API` has been updated');
