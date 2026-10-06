<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layouts/shop-start.jspf" %>
<section><h1>Không thể xử lý yêu cầu</h1><p class="error" role="alert"><c:out value="${errorMessage}" /></p>
<a href="<c:url value='/home' />">Về trang chủ</a></section>
<%@ include file="../layouts/shop-end.jspf" %>
