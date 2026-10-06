<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layouts/shop-start.jspf" %>
<div class="auth-layout"><div class="auth-intro"><a class="auth-brand" href="<c:url value='/home' />"><img src="${appLogoUrl}" alt="TraoTay — Trang chủ" width="2172" height="724"></a><p class="auth-slogan"><c:out value="${appSlogan}" /></p><h1>Thêm kết nối.<br>Thêm giá trị.</h1><p class="muted">Một tài khoản để khám phá, đăng bán và theo dõi những giao dịch của bạn.</p><img src="<c:url value='/assets/images/community-hero.svg' />" alt="Minh họa cộng đồng mua bán"></div><section class="account-card"><p class="eyebrow">Chào mừng bạn trở lại</p><h1>Đăng nhập</h1>
<c:if test="${param.notice == 'registered'}"><p class="notice" role="status">Đăng ký thành công. Bạn có thể đăng nhập bằng email vừa tạo.</p></c:if>
<c:if test="${param.notice == 'required'}"><p class="notice" role="status">Bạn cần đăng nhập để truy cập trang này.</p></c:if>
<c:if test="${not empty errors.form}"><p class="error" role="alert"><c:out value="${errors.form}" /></p></c:if>
<form method="post" action="<c:url value='/login' />" class="account-form">
<input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
<label for="email">Email đăng nhập</label><input id="email" name="email" type="email" required maxlength="254" autocomplete="username" value="<c:out value='${email}' />">
<label for="password">Mật khẩu</label><input id="password" name="password" type="password" required maxlength="72" autocomplete="current-password">
<button type="submit">Đăng nhập</button>
</form><p>Chưa có tài khoản? <a href="<c:url value='/register' />">Đăng ký</a></p>
</section></div>
<%@ include file="../layouts/shop-end.jspf" %>
