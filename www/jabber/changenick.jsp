<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not empty param.nickname and not empty sessionScope.client}">
    <c:set var="oldnickname" value="${sessionScope.client.nickname}"/>
    <c:catch var="error">
	    <c:set target="${sessionScope.client}" property="nickname"
	        value="${param.nickname}"/>
    </c:catch>
    <c:if test="${not empty error}">
        <c:set target="${sessionScope.client}" property="nickname"
            value="${oldnickname}"/>
        Error: ${error.message}
    </c:if>
</c:if>
