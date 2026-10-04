<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đăng ký — Chợ C2C</title><link rel="stylesheet" href="<c:url value='/assets/css/startup.css' />"></head>
<body><main><%@ include file="../layouts/navigation.jspf" %>
<section class="account-card"><p class="eyebrow">Một tài khoản · Cùng mua và bán</p><h1>Tạo tài khoản</h1>
<p>Đăng ký bằng email. Số điện thoại là tùy chọn và được giữ riêng tư.</p>
<c:if test="${not empty errors}"><p class="error" role="alert">Vui lòng kiểm tra các trường được đánh dấu.</p></c:if>
<form method="post" action="<c:url value='/register' />" class="account-form">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
<label for="displayName">Họ tên</label><input id="displayName" name="displayName" required minlength="2" maxlength="100" autocomplete="name" value="<c:out value='${displayName}' />">
<span class="error"><c:out value="${errors.displayName}" /></span>
<label for="email">Email đăng nhập</label><input id="email" name="email" type="email" required maxlength="254" autocomplete="username" value="<c:out value='${email}' />">
<span class="error"><c:out value="${errors.email}" /></span>
<label for="phone">Số điện thoại (tùy chọn)</label><input id="phone" name="phone" type="tel" maxlength="16" autocomplete="tel" value="<c:out value='${phone}' />">
<span class="error"><c:out value="${errors.phone}" /></span>
<label for="password">Mật khẩu</label><input id="password" name="password" type="password" required minlength="8" maxlength="72" autocomplete="new-password" aria-describedby="password-help">
<small id="password-help">Ít nhất 8 ký tự, tối đa 72 byte UTF-8 (ký tự có dấu có thể chiếm nhiều byte).</small>
<span class="error"><c:out value="${errors.password}" /></span>
<label for="confirmation">Xác nhận mật khẩu</label><input id="confirmation" name="confirmation" type="password" required maxlength="72" autocomplete="new-password">
<span class="error"><c:out value="${errors.confirmation}" /></span>
<button type="submit">Đăng ký</button>
</form><p>Đã có tài khoản? <a href="<c:url value='/login' />">Đăng nhập</a></p>
</section></main></body></html>
