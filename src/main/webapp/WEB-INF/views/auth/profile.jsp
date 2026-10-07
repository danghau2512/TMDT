<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layouts/shop-start.jspf" %>
<section class="account-card"><h1>Hồ sơ cá nhân</h1>
<c:if test="${sellerVerified}"><span class="seller-approved" title="Giấy tờ trong hồ sơ đã được quản trị viên kiểm tra"><svg class="icon" aria-hidden="true"><use href="#i-shield" /></svg>Đã xác minh hồ sơ</span></c:if><p><a href="<c:url value='/seller/verification' />">Xác minh hồ sơ người bán →</a></p>
<p><a href="<c:url value='/buyer/orders' />">Đơn mua của tôi</a> · <a href="<c:url value='/buyer/complaints' />">Theo dõi khiếu nại</a></p>
<p>Email đăng nhập: <strong><c:out value="${profile.email}" /></strong></p>
<p>Email đăng nhập được giữ cố định. Bạn có thể cập nhật tên và thông tin liên hệ bên dưới.</p>
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
<small id="contact-help">Thông tin bạn tự nguyện công khai trên chi tiết sản phẩm.</small>
<span class="error"><c:out value="${errors.publicContact}" /></span>
<button type="submit">Lưu hồ sơ</button>
</form></section>
<%@ include file="../layouts/shop-end.jspf" %>
