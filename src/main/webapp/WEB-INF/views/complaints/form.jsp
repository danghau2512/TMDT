<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layouts/shop-start.jspf" %>
<section class="account-card">
    <a href="<c:url value='/buyer/orders/detail'><c:param name='id' value='${order.id}' /></c:url>">← Chi tiết đơn</a>
    <h1>Gửi khiếu nại</h1><p>Đơn <c:out value="${order.order_code}" /></p>
    <p>Bạn có thể phản ánh cả khi chưa nhận hàng. Hồ sơ và ảnh minh chứng chỉ dành cho bạn và Admin.</p>
    <c:if test="${not empty formError}"><p class="error" role="alert"><c:out value="${formError}" /></p></c:if>
    <form method="post" enctype="multipart/form-data" action="<c:url value='/buyer/complaints/create' />" class="account-form">
        <input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="orderId" value="${order.id}">
        <label>Lý do<select name="reason" required><option value="">Chọn vấn đề cần hỗ trợ</option><c:forTokens items="NOT_AS_DESCRIBED,NOT_RECEIVED,DAMAGED,OTHER" delims="," var="reason"><option value="${reason}" ${param.reason == reason ? 'selected' : ''}>${feedbackLabels[reason]}</option></c:forTokens></select></label>
        <label>Mô tả vấn đề<textarea name="content" required minlength="10" maxlength="5000"><c:out value="${param.content}" /></textarea></label>
        <label>Ảnh minh chứng (tùy chọn)<input type="file" name="photos" accept="image/jpeg,image/png" multiple></label>
        <small>Tối đa 5 ảnh JPEG/PNG, 5 MB/ảnh. Mỗi đơn có một hồ sơ; nếu đã gửi, bạn sẽ được đưa về hồ sơ đó.</small>
        <button>Gửi khiếu nại</button>
    </form>
</section>
<%@ include file="../layouts/shop-end.jspf" %>
