String.prototype.repeat = function(cnt) {return new Array(cnt+1).join(this);}

var ringing_phone = '<img src="/img/ringing.png"/>'

var agentStaus = {
    inuse: "glyphicon glyphicon-earphone",
    paused: "glyphicon glyphicon-pause",
    onhold: "glyphicon glyphicon-pause",
    not_inuse: "glyphicon glyphicon-phone-alt",
    ringing: "glyphicon glyphicon-bell",
    loggedoff: "glyphicon glyphicon-log-out",
    loggedon: "glyphicon glyphicon-log-in",
    active: "glyphicon glyphicon-headphones"};

function pbx_agent_status(agent, queue, status) {

    if (!status) status = 'loggedoff';
    var agentQueueId = agent + '_' + queue + '_agent_status';
    $('#' + agentQueueId).replaceWith("<span id='" + agentQueueId +  "' class='pull-right " + agentStaus[status] + "'/>");
}
function pbx_extension_status(agent, status) {
    if (status == -1) status = 'not_inuse';
    var agntSpanId = agent + '_ext_status';
    $('#' + agntSpanId).replaceWith("<span id='" + agntSpanId +  "' class='pull-right " + agentStaus[status] + "'/>");
}
function pbx_agent_name(agent, name) {
    $('#' + agent + '_name').html(name + ' (' + agent + ')')
}
function pbx_queue_count(queue, count) {
    $('#' + queue + '_queue_count').html(count)
}
function pbx_phone_num(agent, num, name) {
    $('#' + agent + '_phone_num').html((num || '') + (name? '(' + name + ')' : ''));
}
function queue_action(action) {
    var agent = $('#agent').val();
    var queue = $('#queue').val();
    var agentName = $('#agent-name').val();
    var localAction = action;
    if (action === "unpause")
        localAction = "pause"
    $.ajax({
        type: 'POST',
        url: '/queue/'+localAction,
        data: JSON.stringify(
            {'agent': agent,
                'queue': queue,
                'memberName': agentName,
                'paused': action === "unpause" ? false : true}),
        contentType: 'application/json; charset=utf-8',
        dataType: 'json'
    });
}
$(function() {
    pbx_connection(false);
    $('#log-on').click(function() {
        queue_action('add');
    });
    $('#pause').click(function() {
        queue_action('pause');
    });
    $('#unpause').click(function() {
        queue_action('unpause');
    });
    $('#log-off').click(function() {
        queue_action('remove');
    });
    $('#park-and-announce').submit(function() {
        $.ajax({
            type: 'POST',
            url: '/park-and-announce?agent-or-channel='+$('#agent-or-channel-trans').val(),
            contentType: 'application/json; charset=utf-8',
            dataType: 'json'
        });
    });

    $('#originate').submit(function() {
        $.ajax({
            type: 'POST',
            url: '/originate/'+$('#src').val()+'/'+$('#dest').val(),
            contentType: 'application/json; charset=utf-8',
            dataType: 'json'
        });
    });
    $('#redirect').submit(function() {
        $.ajax({
            type: 'POST',
            url: '/redirect-to/'+$('#redir-dest').val()+'?agent-or-channel='+$('#agent-or-channel').val(),
            contentType: 'application/json; charset=utf-8',
            dataType: 'json'
        });
    });
})
