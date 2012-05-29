<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/xml"%>
<c:choose>
	<c:when test="${not empty sessionScope.client}">
	    <c:set var="message" value="${client.message}"/>
	</c:when>
	<c:otherwise>
	    <c:set var="message" value="Done"/>
	</c:otherwise>
</c:choose>
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<messages>
    <message><c:out value="${message}" escapeXml="true"/></message>
</messages>
