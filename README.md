# pbxis-ws

Exposes functions of the [PBXIS](https://github.com/inge-mark/pbxis) library as a RESTful web service and can provide the event stream directly to a web browser over HTTP long polling, Server-Sent Events, and WebSocket. The project also includes a ready-to-test HTML page which displays the status of an arbitrary number of agents and queues. The relative URL of the page is `/client/<tech>/<agents>/<queues>`, where `<tech>` is one of `long-poll`, `sse`, or `websocket`, `<agents>` is a comma-separated list of agents' extensions, and `<queues>` is a comma-separated list of queue names. For example,

`http://localhost:58615/client/websocket/147,149/q1,q2`

will open a page that tracks agents 147, 149 (these are their extension numbers) and queues q1, q2, using WebSocket to receive the event stream.

## API -- some general notes

### Request parameters

Any string-typed request parameter may be specified either as a query parameter in the URL or as a JSON parameter in the request body. For example, the following two requests would be equivalent:

`POST /originate/148/1562830?callerId=7012888`

```
POST /originate/148/1562830

{"callerId":"7012888"}
```

### JSON representation of an event

An event is represented as a JSON object, such as `{"type":"queueCount", "queue":"q1", "count":1}`. See the documentation of [events reported by PBXIS](https://github.com/Inge-mark/pbxis/tree/develop#reported-events) and note that property names such as `:agent` occur in JSON as simple strings, such as `"agent"`.


## API

### Stop the service

`POST /stop`

### Fetch queue status

`GET /queue/status`

**Parameter**: `queue` (optional) the queue for which to fetch status. If missing, returns the status of all queues.

**JSON response**:

```JSON
{"queue":"<name>",
 "members": [{"location":"<agent_location", <more_member_properties>}, ...],
 <more_queue_properties>}
```
See the AsteriskJava documentation for the explanation of [queue](http://www.asterisk-java.org/1.0.0.M3/apidocs/org/asteriskjava/manager/event/QueueParamsEvent.html) and [member](http://www.asterisk-java.org/1.0.0.M3/apidocs/org/asteriskjava/manager/event/QueueMemberEvent.html) properties.


### Execute an action against a queue

`POST /queue/<action>`

These are some of the available actions:

- `add`: add the agent to the queue (log him on). Parameters: `queue`, `agent` (required); `memberName`, `paused` (optional);

- `remove`: remove the agent from the queue (log him off). Parameters: `queue`, `agent` (required);

- `pause`: set the *paused* status of a logged-on agent. Parameters: `queue`, `agent`, `paused` (required);

- `reset`: reset queue statistics. Parameter: `queue` (optional).

Explanation of parameters:

- `queue`: the queue against which the action is executed;

- `agent`: the agent on whose behalf the action is executed;

- `memberName`: the name of the agent that will be used by Asterisk;

- `paused`: a boolean, the *paused* status of the agent.

### Place a call

`POST /originate/<src>/<dest>`

**Parameter:** `callerId`

Place a call to the `dest` phone number, patching it through to `src`, a local extension number. Optionally use the provided `callerId` to present the call to the remote party.

**JSON response:** action ID, a simple string. This ID may occur in a later `originateFailed` event. If originating succeeds, it won't be needed.

### Acquire a ticket

`POST /ticket`

**Request body:** `{"agents":[agents],"queues":[queues]}`

Acquires a unique string (<em>ticket</em>) needed to receive events. Configures the event filter with a list of agents and queues (both are arrays of strings).

**JSON response:** the ticket, a simple string.

### Long-polling request

`GET /<ticket>/long-poll`

Places a long-polling request. The response will be received once an event is available in the channel, or after a configured timeout.

**JSON response:** `[event1, event2, ...]`.

See [pbxis-long-poll.js](https://github.com/Inge-mark/pbxis-ws/blob/master/static-content/pbxis-long-poll.js) in this project for a complete example of usage in JavaScript.

### WebSocket request

```
GET /<ticket>/websocket
Upgrade: websocket
```
In JavaScript, use e.g.

```JavaScript
new WebSocket("ws://example.org/" + ticket + "/websocket");
```
Requests a Websocket connection. Each message received contains one JSON object corresponding to one PBXIS event. See [pbxis-websocket.js](https://github.com/Inge-mark/pbxis-ws/blob/master/static-content/pbxis-websocket.js) in this project for a complete example of usage in JavaScript.


### Server-Sent Events request

`GET /<ticket>/sse`

Requests a Server-Sent Events stream. The events will look like this:

```
event: queueCount
data: {"queue":"q1", "count":1}
```
A special event type is `close`, which means that there will be no further events and the connection will be closed. Since SSE specifies automatic reconnection, it is important that the client reacts to this event by closing the `EventSource` at its side.

See [pbxis-sse.js](https://github.com/Inge-mark/pbxis-ws/blob/master/static-content/pbxis-sse.js) in this project for a complete example of usage in JavaScript.


## Example usage scenario

The PBXIS web service can be used to implemented a scenario like the following:

* there is a web application that agents use at their workstation;
* the web application subscribes an agent to the PBXIS web service and passes the event-stream URL to the agent's browser;
* the browser contacts the PBXIS web service directly to receive the event stream.

Another use case is a supervisor application that monitors the activities of all the agents in the call center. The HTML client provided with this package is a very simple example of such an application.


## Release notes

0.1.12
 - changed /queue/<action> API: agent is now a query param;
 - added /queue/status to fetch queue status.


## License

Copyright © 2013 Inge-mark d.o.o.

Distributed under the Eclipse Public License, the same as Clojure.
