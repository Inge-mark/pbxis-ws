var gAgent, gQueues;

function pbxAgentStatus(agent, queue, status) {
    if (!status) status = 'loggedoff';
    var agentQueueId = queue + '_agent_status';
    replaceGlyphiconClass($('#' + agentQueueId), agentStaus[status]);
}

function pbxQueueCount(queue, count) {
    $('#' + queue + '_queue_count').html(count)
}

function pbxPhoneNum(agent, number, name) {
    if (number)
        $('#caller-info').html("" + number + " - " + name);
    else
        $('#caller-info').html("free");
}

function loginAndUnpause(agent, user, queues) {
    gAgent = agent[0];
    gQueues = queues;
    for( q in gQueues) {
        ajaxQueueAction(gAgent, user, 'add', queues[q]);
    };
}


function pbxExtensionStatus(agent, status) {
    if (status === "not_inuse")
        $('#caller-info').html("");
}

function logoutAgent() {
    for( q in gQueues) {
        ajaxQueueAction(gAgent, '', 'remove', gQueues[q]);
    };
    window.location.href = "agentpage";
}



