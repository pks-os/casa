//A bootstrap alert to be used when receiving a response from server
var alertRef;

//Sends some browser metadata
function sendBrowserData() {

    if (platform) {
        widget =  zk.Widget.$('$message');
        platform['ua'] = null;
        platform['offset'] = -60 * new Date().getTimezoneOffset();
        platform['screenWidth'] = screen.width;
        zAu.send(new zk.Event(widget, "onData", platform, {toServer:true}));
    }

}

//Maps a ZK notification constant with a bootstrap alert class
function alertClassForType(type) {

    //See java class org.zkoss.zk.ui.util.Clients
    switch (type) {
        case "info":
            return "alert-success";
        case "warning":
            return "alert-danger";
        case "error":
            return "alert-danger";
    }
    return "";

}

function markupIconForType(type) {

    var cls = "";
    switch (type) {
        case "info":
            cls = "fa-check-circle";
        break;
        case "warning":
            cls = "fa-exclamation-triangle";
        break;
        case "error":
            cls ="fa-exclamation-circle";
        break;
    }
    return cls == "" ? cls : "<i class=\"fas " + cls + "\"></i> ";

}

function showAlert(message, type, delay) {

    if (alertRef) {
        var cls = alertClassForType(type);
        cls = cls == "" ? cls : " " + cls;

        alertRef.removeClass();
        alertRef.addClass('alert' + cls);

        alertRef.html(markupIconForType(type) + message);
        alertRef.show();
        alertRef.delay(delay).slideUp(200, function() {});
    }

}

/*
 This couple of functions control behaviour when the hamburguer icon is clicked
 They are heavily coupled to each other, also they are dependant on HTML and CSS
 used in markup.
 Edit carefully
 */
function partialCollapse() {
    $(".collapsible-menu-item").toggleClass("di dn")
    var aside = $("aside")
    if (!aside.is(":visible")) {
        aside.show()
    }
    aside.toggleClass("w-14r")
}

function collapse() {
    var items = $(".collapsible-menu-item")
    if (items.hasClass("dn")) {
        //revert to original state
        items.toggleClass("di dn")
    }
    var aside = $("aside")
    if (!aside.hasClass("w-14r")) {
        aside.toggleClass("w-14r")
    }
    aside.is(":visible") ? aside.hide() : aside.show()
}
