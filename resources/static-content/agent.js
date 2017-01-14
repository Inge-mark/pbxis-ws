

function pbxAgentStatus(agent, queue, status) {
    if (!status) status = 'loggedoff';
    var agentQueueId = queue + '_agent_status';
    replaceGlyphiconClass($('#' + agentQueueId), agentStaus[status]);
}

function pbxQueueCount(queue, count) {
    $('#' + queue + '_queue_count').html(count)
}

var userAgent;
var session;
var options = {
    media: {
        constraints: {
            audio: true,
            video: false
        },
        render: {
            remote: document.getElementById('remoteVideo'),
            local: document.getElementById('localVideo')
        }
    }
};

$(function() {
    $('#click-to-dial').click( function() {
        var numberToDial = $('#number-to-dial').val();
        alert('Dialing' + numberToDial);
        session = userAgent.invite(numberToDial);
        session.on('accepted', function(e) {

        });
    });
});

function initUserAgent(agentUri, wsServer, authorizationUser, pass){
    userAgent = new SIP.UA({
        traceSip: true,
        uri: agentUri,
        wsServers: [wsServer],
        authorizationUser: authorizationUser,
        password: pass
        //stunServers: []
    });

    userAgent.on('invite', function (session) {
        alert("Recieved Call");
        session.accept(options);
    });


//    session = userAgent.invite('sip:193@192.168.18.40', options);

}


