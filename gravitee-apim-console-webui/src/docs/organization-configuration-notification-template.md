# Customize your notification template

## Portal Notifications

Customize the notifications displayed in the bell in the navigation bar. You can change the title and the content.

## Email Notifications

Customize the notifications sent by email. You can change the subject and the body of the message.

---
**NOTE**: For a **template to include**, you can only override the body.

---

## Parameters
You can use Freemarker template engine to add specific information to your templates:
Examples:

* `${user.name}`
* `${api.metadata['foo-bar']}`

For all notifications, you can use these parameters:

| API               | Application      | Group            |
|-------------------|------------------|------------------|
| name              | name             | name             |
| description       | description      | -                |
| version           | type             | -                |
| role              | status           | -                |
| metadata (Map)    | role             | -                |
| deployedAt (Date) | -                | -                |
| createdAt (Date)  | createdAt (Date) | createdAt (Date) |
| updatedAt (Date)  | updatedAt (Date) | updatedAt (Date) |

| Plan               | Owner/User  | Subscription        |
|--------------------|-------------|---------------------|
| name               | name        | status              |
| description        | description | request             |
| order              | order       | reason              |
| publishedAt (Date) | displayName | processedAt (Date)  |
| closedAt (Date)    | email       | startingAt (Date)   |
| -                  | -           | endingAt (Date)     |
| createdAt (Date)   | -           | closedAt (Date)     |
| updatedAt (Date)   | -           | subscribedAt (Date) |
