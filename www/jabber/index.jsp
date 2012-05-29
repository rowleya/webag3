<%@page import="webag3.JabberClient, org.jivesoftware.smack.XMPPException, org.jivesoftware.smack.packet.XMPPError"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
String server = config.getInitParameter("jabber.server");
int port = (config.getInitParameter("jabber.port") == null ? 5222 :
	Integer.valueOf(config.getInitParameter("jabber.port")).intValue());
String conferenceServer = config.getInitParameter("jabber.conference.server");
%>
<html>
    <head>
        <title>
            Jabber
            <c:if test="${not empty param.groupname}">
                - ${param.groupname}
            </c:if>
        </title>
        <link rel="stylesheet" type="text/css" href="/css/jabber.css"/>
        <script language="JavaScript" src="/js/update.js"></script>
        <script language="JavaScript" src="/js/jabber.js"></script>
        <script language="JavaScript">
            var currentNickname = '${param.nickname}';
            
            function changeNick() {
                if (currentNickname == null) {
                    currentNickname = '${param.nickname}';
                }
                currentNickname = changeNickname(currentNickname);
            }
        </script>
    </head>
    <c:choose>
	    <c:when test="${empty param.groupname}">
		    <body>
		        <form method="post" action="index.jsp">
			        <p>Group: <input type="text" name="groupname"/></p>
			        <p>Nickname: <input type="text" name="nickname"/></p>
			        <p><input type="submit" name="submit" value="Join"/></p>
			    </form>
		    </body>
	    </c:when>
	    <c:otherwise>
	        <%
	        boolean done = false;
	        String nickname = request.getParameter("nickname");
	        int count = 0;
	        while (!done) {
		        try {
			        JabberClient client = new JabberClient(server, port, false,
			    	        request.getParameter("groupname") + "@"
			    	             + conferenceServer, 
			    	        nickname);
			        session.setAttribute("client", client);
			        session.setMaxInactiveInterval(365 * 24 * 60 * 60);
			        done = true;
		        } catch (XMPPException e) {
			        XMPPError error = e.getXMPPError();
			        if (error != null) {
				        if (error.getCode() == 409) {
					        if (count < 2) {
						        nickname += "_";
						        done = false;
						        count++;
					        } else {
						        done = true;
						        throw e;
					        }
				        } else {
					        done = true;
					        throw e;
				        }
			        } else {
				        done = true;
				        throw e;
			        }
		        }
	        }
	        %>
	        <body onfocus="dofocus();"
	                onresize="doresize('messages', 'participants', 
	                                 'sendmessage', 'vertsep', 'horizsep');"
  				    onbeforeunload="return beforeclose();">
			    <div class="message" id="messages"></div>
			    <div class="participants" id="participants"></div>
			    <div class="sendmessage" id="sendmessage">
			        <textarea class="sendbox" id="sendbox" 
			            onkeypress="return sendOnReturn(
			                event, 'sendbox');"></textarea>
			        <input class="sendbutton" type="button" value="Send"
			            onclick="sendMessage('sendbox');"/>
				    <input class="closebutton" type="button" name="close"
				        value="Close" onclick="closeButton();"/>
				    <input class="aboutbutton" type="button" value="About" 
				        onclick="about();"/>
				    <input class="changenickbutton" type="button" 
				        value="Change Nickname" onclick="changeNick();"/>
				    <div class="focusbox">
    				    <input type="checkbox" checked id="focusbutton"/>
				        Focus on new message
				    </div>
			    </div>
			    <div class="verticalseparator" id="vertsep" 
			        onmousedown="mouseOverVertical(event, 
			            new Array('messages'), new Array('participants'), 
			            'vertsep', new Array('messages'));"
			        onmouseup="stopDrag();calculateWidthPercent('messages');">
			    </div>
	        </body>
	        <script language="JavaScript">
	            doresize('messages', 'participants', 'sendmessage',
	                     'vertsep', 'horizsep');
	            new Update('messages', 'participants', 'sendmessage', 
	                'focusbutton', 'getmessage.jsp');
	            window.onbeforeunload = function (event) {
	                return beforeclose();
	            };
	        </script>
	    </c:otherwise>
    </c:choose>
</html>
