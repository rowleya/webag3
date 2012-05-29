<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Vector" %>
<%@ page import="ag3.interfaces.types.ConnectionDescription" %>
<%@ page import="ag3.interfaces.types.VenueState" %>
<%@ page import="ag3.interfaces.Venue" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%!
final String HOST = "sam.ag.manchester.ac.uk";
final String AG3SERVER = "https://" + HOST + ":8000/Venues/default";

HashMap getAllVenues(VenueState parent, String uri, HashMap venues, int depth) {
    Venue venue = new Venue(uri);
    VenueState state;
    try {
        state = venue.getState();
        state.setParent(parent);
        venues.put(uri, state);
        Collections.sort(state.getConnections());
        if (depth > 0) {
            Vector connections = state.getConnections();
            for (int i = 0; i < connections.size(); i++) {
                try {
                    ConnectionDescription connection = 
                        (ConnectionDescription) 
                        connections.get(i);
                    String connectionUri = connection.getUri();
                    if (!venues.containsKey(connectionUri)) {
                        getAllVenues(state, connectionUri, 
                                venues, depth - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return venues;
}

String getJabberName(String room) {
    return room.toLowerCase().replaceAll("[ \"&'/:<>@]", "-") + "(" + HOST + ")";
}
%>
<%
HashMap venues = new HashMap(); 
venues = getAllVenues(null, AG3SERVER, venues, 1);
%>
<html>
    <head>
        <title>
            Jabber
        </title>
        <script language="JavaScript">
		    function enterRoom(room, nick) {
		        while (nick == "") {
		            nick = prompt("Please enter a nickname:", "");
		        }
		        if (nick != null) {
		            if (nick == "") {
		                alert("You must enter a nickname to enter the room");
		            } else {
		                var win = window.open("jabber.jsp?groupname="+room+"&nickname="+nick, "Jabber",
		                    "menubar=no,resizable=yes,toolbar=no,status=no,scrollbars=yes," +
		                    "height=400,width=500");
		            }
		        }
		    }
		</script>
    </head>
    <body>
	    <h2>Web Jabber Client</h2>
		<p>Please enter a nickname below, select a venue and then click on 'Enter Room' to start the client.</p>
		<p>Nickname: <input type='text' name='nickname' id='nickname'/></p>
	    <%
	    VenueState root = (VenueState) venues.get(AG3SERVER);
	    Vector connections = root.getConnections();
	    for (int i = 0; i < connections.size(); i++) {
	        ConnectionDescription connection = 
	            (ConnectionDescription) connections.get(i);
	        VenueState lobby = (VenueState) venues.get(connection.getUri());
	        if (lobby != null) {
		        out.println("<hr/>");
		        out.println("<h3>" + lobby.getName() + "</h3>");
		        out.println("<select name='lobby" + i + "'>");
		        out.println("<option value='" + getJabberName(lobby.getName()) + "'>" + lobby.getName() + "</option>");
		        Vector lobbyConnections = lobby.getConnections();
		        for (int j = 0; j < lobbyConnections.size(); j++) {
		            ConnectionDescription room =
		                (ConnectionDescription) lobbyConnections.get(j);
		            out.println("<option value='" + getJabberName(room.getName())
		                     + "'>" + room.getName() + "</option>");
		        }
		        out.println("</select>");
		        out.println("<input type='button' value='Enter Room' onclick='enterRoom(getElementById(\"lobby" + i + "\").value, getElementById(\"nickname\").value)'/>\n");
	        }
	    }
	    %>
	    <hr/>
	    <p><i>WebJabber was written by Andrew G D Rowley, University of Manchester, 2006</i></p>
	</body>
</html>
