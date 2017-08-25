
String.prototype.repeat = function(cnt) {return new Array(cnt+1).join(this);}

var ringing_phone = '<img src="/img/ringing.png"/>'
var callsCharts, callsSLACharts

function pbxAgentStatus(agent, queue, status) {
    if (!status) status = 'loggedoff';
    var agentQueueId = agent + '_' + queue + '_agent_status';
    replaceGlyphiconClass($('#' + agentQueueId), agentStaus[status]);
}

function pbxExtensionStatus(agent, status) {
    if (status == -1) status = 'not_inuse';
    var agntSpanId = agent + '_ext_status';
    replaceGlyphiconClass($('#' + agntSpanId), agentStaus[status])
}

function pbxAgentName(agent, name) {
    $('#' + agent + '_name').html(name + ' (' + agent + ')')
}
function pbxQueueCount(queue, count) {
    $('#' + queue + '_queue_count').html(count)
}
function pbxPhoneNum(agent, num, name) {
    $('#' + agent + '_phone_num').html((num || '') + (name? '(' + name + ')' : ''));
}
function queueAction(action) {
    var agent = $('#agent').val();
    var queue = $('#queue').val();
    var agentName = $('#agent-name').val();
    var localAction = action;
    if (action === "unpause")
        localAction = "pause";
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


function pbxWallboard(e) {
    if ($('#' + e.queue + '_callsChart').length > 0) {
        if (e.type === 'QueueSummary') {
            for (i in e) {
                $('#' + e['queue'] + '_' + i).html(e[i]);
            }
            var summary = e.completed + e.abandoned;
            var ratio = summary == 0 ? 100 : (e.completed / summary) * 100;
            callsCharts[e.queue + '_callsChart'].refresh(ratio);
            callsSLACharts[e.queue + '_callsSLAChart'].refresh(e.serviceLevelPerf);
        }
        if (e.type === 'MemberSummary') {
            var members = e.members;
            members.sort(function (a, b) {
                if (a.callsTaken > b.callsTaken)
                    return -1;
                else (e.callsTaken < b.callsTaken)
                return 1;
                return 0;
            });
            var tbody = $('#' + e['queue'] + '_agents');
            var row = '<tr>';
            var cols = ['memberName', 'location', 'callsTaken', 'paused']
            for (i in members) {
                for (x in cols) {
                    if (x != 3) {
                        row += "<td>" + members[i][cols[x]] + '</td>';
                    } else {
                        td_incall = ''
                        ico = members[i][cols[x]] ? "glyphicon-pause" : "glyphicon-headphones";
                        if (members[i]['incall'])
                            td_incall="info";
                        row += "<td class='" + td_incall +"'><span class='glyphicon " + ico + "'></span></td>";
                    }
                }
                row += '</tr>'
                tbody.html(row);
            }
        }
    }
}

function drawCharts(elements, title) {
    var r=[];
    for(i=0; i<elements.length; i++) {
        r[elements[i].id] =
            new JustGage({
                id: elements[i].id,
                value: 0,
                min: 0,
                max: 100,
                title: title,
                relativeGaugeSize: true
            })}
    return r;
}


$(function() {
    pbxConnection(false);
    $('#log-on').click(function() {
        queueAction('add');
    });
    $('#pause').click(function() {
        queueAction('pause');
    });
    $('#unpause').click(function() {
        queueAction('unpause');
    });
    $('#log-off').click(function() {
        queueAction('remove');
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

    callsCharts = drawCharts($('[id*=_callsChart'), 'Success (%)');
    callsSLACharts = drawCharts($('[id*=_callsSLAChart'),'SLA (%)');
})
