<jsp:useBean id="client" type="webag3.JabberClient" scope="session"/>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%client.close();%>
<a href="javascript: top.window.close();">Close this window</a>
