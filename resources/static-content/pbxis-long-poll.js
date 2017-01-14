function handleEvents(events) {
    var result = true;
    console.log("HERE!")
    $.each(events, function(_, e) { result &= handleEvent(e); });
    return result;
}

function pbxLongPoll(ticket) {
    $.getJSON("/" + ticket + "/long-poll", function(r) {
        if (handleEvents(r)) pbxLongPoll(ticket);
        else pbxConnection(false);
    }).error(function() {pbxConnection(false)});
}

function pbxStart(agents, queues, summaryEvents) {
    $.ajax(
        {
            type: "POST",
            url: "/ticket",
            data: JSON.stringify({agents: agents, queues: queues, summaryEvents: summaryEvents}),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function(ticket) {
                pbxConnection(true);
                pbxLongPoll(ticket);
            }
        }
    )
}
