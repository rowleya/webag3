/**
 * This class is used to update the jabber messages
 */

/**
 * Creates a new Update
 *
 * @param messagediv The div containing the messages
 * @param participantdivid The div containing the participants
 * @param senddivid The div containing the send box
 * @param bringtofrontcheckid The checkbox to decide if the window should
 *                                jump to the front on a new message
 * @param url The url to get the message from
 */
function Update(messagedivid, participantdivid, senddivid,
        bringtofrontcheckid, url) {
    this.url = url;
    this.messagedivid = messagedivid;
    this.participantdivid = participantdivid;
    this.senddivid = senddivid;
    this.bringtofrontcheckid = bringtofrontcheckid;

    this.request = null;

    this.getUpdate = function() {
        this.request = newRequest();
        this_ = this;
        this.request.onreadystatechange = function() {this_.handleUpdate()};
        this.request.open("POST", this.url, true);
        this.request.setRequestHeader('Content-Type',
                                      'application/x-www-form-urlencoded');
        this.request.send("");
    };
    this.handleUpdate = function() {
        if (this.request.readyState == 4) {
            if (this.request.status == 200) {
                var response = this.request.responseXML.documentElement;
                var message = response.getElementsByTagName(
                                  'message')[0].firstChild.data;
                if (message.indexOf('Message:') == 0) {
                    var messagediv = document.getElementById(this.messagedivid);
                    var bringtofrontcheck = document.getElementById(
                        this.bringtofrontcheckid);
                    message = message.substring(8);
                    messagediv.innerHTML += message;
                    if (bringtofrontcheck.checked) {
                        window.focus();
                    }
                    messagediv.scrollTop = messagediv.scrollHeight;
                } else if (message.indexOf('RosterAdd:') == 0) {
                    message = message.substring(10);
                    addParticipant(message);
                } else if (message.indexOf('RosterRemove:') == 0) {
                    message = message.substring(13);
                    removeParticipant(message);
                } else {
                    //alert(message);
                }
                if (message != "Done") {
                    this.getUpdate();
                } else {
                    document.getElementById('sendmessage').innerHTML =
                        '<a href="javascript: window.close()">'
                        + 'Close this window</a><br/>'
                        + '<a href="javascript: window.location.reload()">'
                        + 'Re-connect</a><br/>';
                }
            } else {
                alert("An error has occurred " + this.request.status + ": " + this.request.statusText);
            }
        }
    };
    this.getUpdate();
}

function newRequest() {
    if (window.XMLHttpRequest) {
        return new XMLHttpRequest();
    } else if (window.ActiveXObject) {
        return new ActiveXObject("Microsoft.XMLHTTP");
    }
}
