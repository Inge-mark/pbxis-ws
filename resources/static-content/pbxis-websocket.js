var socket;
function pbxStart(agents, queues, summaryEvents) {
    $.ajax(
        {
            type: "POST",
            url: "/ticket",
            data: JSON.stringify({agents: agents, queues: queues, summaryEvents: summaryEvents}),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function(ticket) {
                socket = new WebSocket(
                    "ws" + document.location.origin.substring(4) + "/" + ticket + "/websocket");
                socket.onopen = function() { pbxConnection(true); }
                socket.onclose = function() { console.log("Websocket closed");
                                              pbxConnection(false); }
                socket.onmessage = function(e) {
                    if (!handleEvent(JSON.parse(e.data))) socket.close();
                };
            }
        }
    )
}
