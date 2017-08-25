var agentStaus = {
    inuse: "glyphicon-earphone",
    paused: "glyphicon-pause",
    onhold: "glyphicon-pause",
    not_inuse: "glyphicon-phone-alt",
    ringing: "glyphicon-bell",
    loggedoff: "glyphicon-log-out",
    loggedon: "glyphicon-log-in",
    active: "glyphicon-headphones"};

function replaceGlyphiconClass(id, newGlyphicon){
    var cls = id.attr('class');
    if (!(typeof cls === 'undefined' || cls === null)) {
        var c = cls.match(/glyphicon-\S+/i);
        if (c !== null ) {
            id.removeClass(c.join(' '));
        }
    }
    id.addClass(newGlyphicon);
}

function ajaxQueueAction(agent, agentName, action, queue) {
    var localAction = action;
    if (action === "unpause") {
        localAction = "pause";
    }
    $.ajax({
        type: 'POST',
        url: '/queue/'+localAction,
        async: false,
        data: JSON.stringify(
            {'agent': agent,
                'queue': queue,
                'memberName': agentName,
                'paused': (action === "unpause" || action === "add") ? false : true}),
        contentType: 'application/json; charset=utf-8',
        dataType: 'json'
    });
}