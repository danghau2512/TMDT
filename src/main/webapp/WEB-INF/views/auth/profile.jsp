<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Hồ sơ cá nhân — Chợ C2C</title><link rel="stylesheet" href="<c:url value='/assets/css/startup.css' />"></head>
<body><main><%@ include file="../layouts/navigation.jspf" %>
<section class="account-card"><h1>Hồ sơ cá nhân</h1>
<p>Email đăng nhập: <strong><c:out value="${profile.email}" /></strong></p>
<p>Email, quyền và trạng thái không được thay đổi qua biểu mẫu này.</p>
<c:if test="${param.notice == 'saved'}"><p class="notice" role="status">Đã cập nhật hồ sơ của bạn.</p></c:if>
<c:if test="${not empty errors}"><p class="error" role="alert">Vui lòng kiểm tra thông tin hồ sơ.</p></c:if>
<span class="error"><c:out value="${errors.form}" /></span>
<form method="post" action="<c:url value='/account/profile' />" class="account-form">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
<label for="displayName">Họ tên</label><input id="displayName" name="displayName" required minlength="2" maxlength="100" autocomplete="name" value="<c:out value='${displayName}' />">
<span class="error"><c:out value="${errors.displayName}" /></span>
<label for="phone">Số điện thoại riêng tư (tùy chọn)</label><input id="phone" name="phone" type="tel" maxlength="16" autocomplete="tel" value="<c:out value='${phone}' />">
<span class="error"><c:out value="${errors.phone}" /></span>
<label for="publicContact">Liên hệ công khai (tùy chọn)</label><input id="publicContact" name="publicContact" maxlength="255" value="<c:out value='${publicContact}' />" aria-describedby="contact-help">
<small id="contact-help">Thông tin bạn tự nguyện công khai cho người mua ở mốc đăng bán sau.</small>
<span class="error"><c:out value="${errors.publicContact}" /></span>
<button type="submit">Lưu hồ sơ</button>
</form></section></main></body></html>
