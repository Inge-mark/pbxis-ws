$(function() {
    var form = $('#form')
    form.submit(function(e) {
        if(!$('#local').val()) {
            $('#message').removeClass('hidden');
            e.preventDefault();
        } else {
            $('#message').addClass('hidden');
        }
    });
})