<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đăng nhập — Chợ C2C</title><link rel="stylesheet" href="<c:url value='/assets/css/startup.css' />"></head>
<body><main><%@ include file="../layouts/navigation.jspf" %>
<section class="account-card"><p class="eyebrow">Chào mừng bạn trở lại</p><h1>Đăng nhập</h1>
<c:if test="${param.notice == 'registered'}"><p class="notice" role="status">Đăng ký thành công. Bạn có thể đăng nhập bằng email vừa tạo.</p></c:if>
<c:if test="${param.notice == 'required'}"><p class="notice" role="status">Bạn cần đăng nhập để truy cập trang này.</p></c:if>
<c:if test="${not empty errors.form}"><p class="error" role="alert"><c:out value="${errors.form}" /></p></c:if>
<form method="post" action="<c:url value='/login' />" class="account-form">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
<label for="email">Email đăng nhập</label><input id="email" name="email" type="email" required maxlength="254" autocomplete="username" value="<c:out value='${email}' />">
<label for="password">Mật khẩu</label><input id="password" name="password" type="password" required maxlength="72" autocomplete="current-password">
<button type="submit">Đăng nhập</button>
</form><p>Chưa có tài khoản? <a href="<c:url value='/register' />">Đăng ký</a></p>
</section></main></body></html>
