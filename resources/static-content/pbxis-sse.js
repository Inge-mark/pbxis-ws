function pbxStart(agents, queues, summaryEvents) {
    $.ajax(
        {
            type: "POST",
            url: "/ticket",
            data: JSON.stringify({agents: agents, queues: queues, summaryEvents: summaryEvents}),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function(ticket) {
                eventSource = new EventSource("/"+ticket+"/sse");
                eventSource.onopen = function() { pbxConnection(true); }
                $.each(["queueMemberStatus","extensionStatus","queueCount","phoneNumber","closed"],
                       function(_, t) { eventSource.addEventListener(t, function (e) {
                           console.log("SSE event " + e.data);
                           var ev = JSON.parse(e.data);
                           ev.type = e.type;
                           if (!handleEvent(ev)) {
                               console.log("Close eventSource");
                               eventSource.close();
                           }
                       }
                                                                    ); });
            }
        }
    )
}
