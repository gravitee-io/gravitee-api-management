print('Rename ApiType from SYNC & ASYNC to PROXY & MESSAGE');
// Override this variable if you use prefix
const prefix = "";

print('Update apis collection');
let apisCollection = db.getCollection(`${prefix}apis`);
apisCollection.find({"definitionVersion": "V4"}).forEach((api) => {
	if (api.type == "SYNC") {
		api.definition = api.definition.replace('"type" : "sync"', '"type" : "proxy"');
		api.type = "PROXY";
        apisCollection.replaceOne({ _id: api._id }, api);
	}
	if (api.type == "ASYNC") {
		api.definition = api.definition.replace('"type" : "async"', '"type" : "message"');
		api.type = "MESSAGE";
	    apisCollection.replaceOne({ _id: api._id }, api);
	}
});

print('Update events collection');
let eventsCollection = db.getCollection(`${prefix}events`);
eventsCollection.find({"type": "PUBLISH_API"}).forEach((event) => {
    event.payload = event.payload.replace('\\"type\\" : \\"sync\\"', '\\"type\\" : \\"proxy\\"');
    event.payload = event.payload.replace('\\"type\\" : \\"async\\"', '\\"type\\" : \\"message\\"');
	event.payload = event.payload.replace('"type" : "sync"', '"type" : "proxy"');
	event.payload = event.payload.replace('"type" : "async"', '"type" : "message"');
		
    eventsCollection.replaceOne({ _id: event._id }, event);
});