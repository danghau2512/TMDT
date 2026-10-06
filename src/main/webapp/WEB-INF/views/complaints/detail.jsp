<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layouts/shop-start.jspf" %>
<section>
    <a href="<c:url value='${complaintBase}' />">← Danh sách khiếu nại</a>
    <h1><c:out value="${complaint.title}" /></h1><p><span class="badge" data-status="${complaint.status}">${feedbackLabels[complaint.status]}</span></p>
    <p>Đơn <c:out value="${order.order_code}" /> · ${labels[order.status]}<br>Người mua: <c:out value="${order.buyer_name_snapshot}" /><br>Người bán: <c:out value="${order.seller_name_snapshot}" /></p>
    <a href="<c:url value='${complaintAdmin ? "/admin/orders/detail" : "/buyer/orders/detail"}'><c:param name='id' value='${order.id}' /></c:url>">Xem chi tiết đơn liên quan</a>
    <ol class="complaint-stepper" aria-label="Trạng thái hiện tại"><li class="active ${complaint.status == 'RECEIVED' ? 'current' : ''}">1. Đã tiếp nhận</li><li class="${complaint.status != 'RECEIVED' ? 'active' : ''} ${complaint.status == 'PROCESSING' ? 'current' : ''}">2. Đang xử lý</li><li class="${complaint.status == 'RESOLVED' ? 'active current' : ''}">3. Đã xử lý</li></ol>
    <div class="case-content"><h2>Nội dung phản ánh</h2><p class="preserve"><c:out value="${complaint.content}" /></p><small>Gửi lúc <time data-local-time><c:out value="${complaint.created_at}" /></time></small></div>
    <c:if test="${not empty formError}"><p class="error" role="alert"><c:out value="${formError}" /></p></c:if>
    <h2>Ảnh minh chứng riêng tư</h2>
    <c:if test="${empty evidence}"><p>Người mua chưa gửi ảnh minh chứng.</p></c:if>
    <div class="evidence-gallery"><c:forEach items="${evidence}" var="photo">
        <a href="<c:url value='/media/complaint-evidence'><c:param name='complaintId' value='${complaint.id}' /><c:param name='asset' value='${photo.asset_id}' /></c:url>" aria-label="Xem ảnh minh chứng lớn"><img src="<c:url value='/media/complaint-evidence'><c:param name='complaintId' value='${complaint.id}' /><c:param name='asset' value='${photo.asset_id}' /></c:url>" alt="Ảnh minh chứng khiếu nại"></a>
    </c:forEach></div>
    <c:if test="${complaint.status == 'RESOLVED'}"><div class="case-resolution"><h2>✓ Kết quả xử lý: ${feedbackLabels[complaint.resolution_code]}</h2><p class="preserve"><c:out value="${complaint.resolution_summary}" /></p><small>Thời điểm: <time data-local-time><c:out value="${complaint.resolved_at}" /></time></small></div></c:if>
</section>
<div class="detail-grid"><section>
    <h2>Phản hồi và bổ sung</h2>
    <c:if test="${empty messages}"><p>Chưa có phản hồi.</p></c:if>
    <c:forEach items="${messages}" var="message"><article class="response-card" data-role="${message.author_role_snapshot}"><div class="response-head"><strong><c:out value="${message.author_name}" /> · ${message.author_role_snapshot == 'ADMIN' ? 'Admin' : 'Người mua'}</strong><span class="badge">${feedbackLabels[message.message_type]}</span></div><p class="preserve"><c:out value="${message.body}" /></p><small><time data-local-time><c:out value="${message.created_at}" /></time> · ${message.author_role_snapshot == 'ADMIN' ? 'Admin' : 'Người mua'}</small></article></c:forEach>
</section><section>
    <h2>Tiến độ xử lý</h2><ol class="timeline"><c:forEach items="${complaintHistory}" var="event"><li><strong>${feedbackLabels[event.to_status]}</strong><p class="preserve"><c:out value="${event.reason}" /></p><c:if test="${not empty event.resolution_summary_snapshot}"><p>Kết quả lưu: ${feedbackLabels[event.resolution_code_snapshot]}<br><c:out value="${event.resolution_summary_snapshot}" /></p></c:if><small><c:out value="${event.actor_name}" default="Hệ thống" /> · <time data-local-time><c:out value="${event.created_at}" /></time></small></li></c:forEach></ol>
</section></div>
<c:choose><c:when test="${complaintAdmin}"><section>
    <h2>Admin xử lý hồ sơ</h2><p>Phản hồi không tự đổi trạng thái đơn, kho hoặc thanh toán.</p>
    <c:if test="${complaint.status != 'RESOLVED'}">
        <form method="post" action="<c:url value='/admin/complaints/action' />" class="account-form">
            <input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${complaint.id}">
            <label>Thao tác<select name="action"><option value="RESPOND">Gửi phản hồi</option><c:if test="${complaint.status == 'RECEIVED'}"><option value="PROCESS">Chuyển sang đang xử lý</option></c:if><c:if test="${complaint.status == 'PROCESSING'}"><option value="RESOLVE" ${param.action == 'RESOLVE' ? 'selected' : ''}>Kết thúc xử lý</option></c:if></select></label>
            <label>Phản hồi / lý do<textarea name="response" required maxlength="5000"><c:out value="${param.response}" /></textarea></label>
            <c:if test="${complaint.status == 'PROCESSING'}"><div data-resolution-fields><label>Kết quả (bắt buộc khi kết thúc)<select name="resolution"><option value="">Chọn kết quả</option><c:forTokens items="SELLER_CONTACTED,ORDER_CANCELLED,NO_ACTION,OTHER" delims="," var="code"><option value="${code}" ${param.resolution == code ? 'selected' : ''}>${feedbackLabels[code]}</option></c:forTokens></select></label><label>Kết luận xử lý<textarea name="summary" maxlength="5000"><c:out value="${param.summary}" /></textarea></label><small>“Đơn đã được hủy” chỉ dùng khi đơn thực sự đã hủy qua luồng đơn hàng.</small></div></c:if>
            <button>Lưu xử lý</button>
        </form>
    </c:if>
    <c:if test="${complaint.status == 'RESOLVED'}"><form method="post" action="<c:url value='/admin/complaints/action' />" class="account-form"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${complaint.id}"><input type="hidden" name="action" value="REOPEN"><label>Lý do mở lại<textarea name="response" required maxlength="5000"></textarea></label><button class="secondary">Mở lại hồ sơ</button></form></c:if>
</section></c:when><c:otherwise><c:if test="${complaint.status != 'RESOLVED'}"><section>
    <h2>Bổ sung cho Admin</h2><form method="post" enctype="multipart/form-data" action="<c:url value='/buyer/complaints/supplement' />" class="account-form"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${complaint.id}"><label>Nội dung bổ sung<textarea name="content" required maxlength="5000"><c:out value="${param.content}" /></textarea></label><label>Ảnh bổ sung<input type="file" name="photos" data-photo-picker="5" accept="image/jpeg,image/png" multiple></label><small>Tối đa 5 ảnh/lần, 20 ảnh/hồ sơ; nội dung cũ được giữ nguyên.</small><button>Gửi bổ sung</button></form>
</section></c:if></c:otherwise></c:choose>
<section><h2>Đối chiếu giao dịch</h2><p>Trạng thái đơn: ${labels[order.status]} · Thanh toán: ${labels[payment.status]} (${labels[payment.method]})</p><p>Tổng: <fmt:formatNumber value="${order.grand_total}" maxFractionDigits="0" /> đ</p>
<%@ include file="../orders/snapshot-items.jspf" %>
</section>
<div class="detail-grid"><section><h2>Lịch sử đơn</h2><ol class="timeline"><c:forEach items="${orderHistory}" var="event"><li><strong>${labels[event.to_status]}</strong><p><c:out value="${event.reason}" /></p><small><c:out value="${event.actor_name}" default="Hệ thống" /> · <time data-local-time><c:out value="${event.created_at}" /></time></small></li></c:forEach></ol></section><section><h2>Lịch sử thanh toán</h2><ol class="timeline"><c:forEach items="${paymentHistory}" var="event"><li><strong>${labels[event.to_status]}</strong><p><c:out value="${event.note}" /></p><small><c:out value="${event.actor_name}" default="Hệ thống" /> · <time data-local-time><c:out value="${event.created_at}" /></time></small></li></c:forEach></ol></section></div>
<%@ include file="../layouts/shop-end.jspf" %>
