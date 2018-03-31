<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<%@page import="java.util.Date"%>

<c:set var="datePattern" value="yyyy-MM-dd" />
<c:set var="dateTimePattern" value="yyyy-MM-dd HH:mm:ss" />
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="jsPath" value="/static/js" />
<c:set var="cssPath" value="/static/css"/>
<c:set var="imagePath" value="/static/image"/>

