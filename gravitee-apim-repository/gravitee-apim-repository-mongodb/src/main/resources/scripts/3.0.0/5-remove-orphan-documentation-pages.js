print('find all orphan pages and remove them from db')
db.pages.find({parentId: {$exists: true}}).forEach(doc => {
  const parentCursor = db.pages.find({_id: doc.parentId});
  if (!parentCursor.hasNext()) {
    //printjson(doc);  
    db.pages.remove(doc);  
  }
});
