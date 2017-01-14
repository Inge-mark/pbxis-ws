function pbxConnection(is_connected) {}
function pbxAgentStatus(agent, queue, status) {}
function pbxAgentName(agent, name) {}
function pbxExtensionStatus(status) {}
function pbxQueueCount(queue, count) {}
function pbxPhoneNum(num) {}
function pbxWallboard(e){}

function handleEvent(e) {
    console.log("Handling event " + JSON.stringify(e));
    switch (e.type) {
        case "queueMemberStatus":
            pbxAgentStatus(e.agent, e.queue, e.status);
            break;
        case "agentName":
            pbxAgentName(e.agent, e.name);
            break;
        case "extensionStatus":
            pbxExtensionStatus(e.agent, e.status);
            break;
        case "queueCount":
            pbxQueueCount(e.queue, e.count);
            break;
        case "phoneNumber":
            pbxPhoneNum(e.agent, e.number, e.name);
            break;
        case "QueueSummary":
            pbxWallboard(e);
            break;
        case "MemberSummary":
            pbxWallboard(e);
            break;
        case "closed":
            pbxConnection(false);
            return false;
    }
    return true;
}
