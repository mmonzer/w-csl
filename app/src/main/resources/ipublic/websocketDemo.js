//Establish the WebSocket connection and set up event handlers

//alert('location:'+location.hostname+" port:"+location.port);

var dict = {};
dict["time"]=0;
dict["input"]=0;
dict["output"]=0;
dict["setpoint"]=0;


//alert('testcc');
var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/chat");
webSocket.onmessage = function (msg) { updateChat(msg); };
webSocket.onclose = function () { alert("WebSocket connection closed") };

//Send message if "Send" is clicked
//id("send").addEventListener("click", function () {
//    sendMessage(id("message").value);
//});

//Send message if enter is pressed in the input field
//id("message").addEventListener("keypress", function (e) {
//    if (e.keyCode === 13) { sendMessage(e.target.value); }
//});

//Send a message if it's not empty, then clear the input field
function sendMessage(message) {
    if (message !== "") {
        webSocket.send(message);
        id("message").value = "";
    }
}

//Update the chat-panel, and the list of connected users
function updateChat(msg) {
    var data = JSON.parse(msg.data);
    var schange=data.userMessage;
    insert("chat", data.userMessage);
    var change=JSON.parse(schange);
    dict[change.name]=change.newValue
    dict["time"]=change.time;
    
}

//Helper function for inserting HTML as the first child of an element
function insert(targetId, message) {
	var x = id(targetId).innerHTML;
    //id(targetId).insertAdjacentHTML("afterbegin", message);
    x=JSON.stringify(dict)+"<br>"+x;
    x=x.substring(0,300);
    id(targetId).innerHTML=x;
}

//Helper function for selecting element by id
function id(id) {
    return document.getElementById(id);
}
