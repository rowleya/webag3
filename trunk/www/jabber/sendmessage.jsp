<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${not empty param.text and not empty sessionScope.client}">
    <c:set target="${sessionScope.client}" property="message"
        value="${param.text}"/>
</c:if>
