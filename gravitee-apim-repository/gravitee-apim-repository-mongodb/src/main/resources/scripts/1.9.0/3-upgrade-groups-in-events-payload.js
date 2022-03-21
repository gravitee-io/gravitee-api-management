print("\n\nConvert group to groups in events payload");
db.events.find().forEach(
  function (event) {
    if (event.payload && event.payload.indexOf("group\"") > 0) {
      var payloadAsJson = JSON.parse(event.payload);
      print(" - event id: " + event._id);
      print("         payload.group: " + payloadAsJson.group);
      payloadAsJson.groups = [payloadAsJson.group];
      delete payloadAsJson.group;
      print("         payload.groups: " + payloadAsJson.groups);
      event.payload = JSON.stringify(payloadAsJson);
      print(event.payload);
      db.events.updateOne(
        {
          _id: event._id
        },
        {
          $set: {
            payload: event.payload
          }
        },
        {
          upsert :true
        }
      );
      print('   ...... done. \n');
    }
  }
);
