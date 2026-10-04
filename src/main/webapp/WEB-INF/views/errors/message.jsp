<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Thông báo — Chợ C2C</title><link rel="stylesheet" href="<c:url value='/assets/css/startup.css' />"></head>
<body><main><section><h1>Không thể xử lý yêu cầu</h1><p class="error" role="alert"><c:out value="${errorMessage}" /></p>
<a href="<c:url value='/home' />">Về trang chủ</a></section></main></body></html>
