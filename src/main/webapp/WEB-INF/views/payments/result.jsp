<%@ page pageEncoding="UTF-8" %>
<%@ include file="../layouts/shop-start.jspf" %>
<section class="payment-result"><p class="eyebrow">VNPAY Sandbox – môi trường thử nghiệm</p>
<c:choose>
<c:when test="${attempt.status == 'SUCCEEDED' and payment.status != 'REFUND_PENDING'}"><div class="success-icon"><svg class="icon" aria-hidden="true"><use href="#i-check" /></svg></div><h1>Thanh toán thành công</h1><p>Kết quả đã được backend xác nhận từ VNPAY và lưu vào đơn hàng.</p></c:when>
<c:when test="${attempt.status == 'NEEDS_REVIEW' or payment.status == 'REFUND_PENDING'}"><h1>Giao dịch cần đối chiếu</h1><p class="notice">${payment.status == 'REFUND_PENDING' ? 'Đơn đã hủy. Chờ xử lý hoàn tiền; hệ thống chưa thực hiện hoàn tiền qua VNPAY.' : 'Có kết quả cần Admin đối chiếu trước khi tiếp tục. Không thanh toán thêm.'}</p></c:when>
<c:when test="${attempt.status == 'FAILED'}"><h1>Thanh toán chưa thành công</h1><p>Gateway ghi nhận giao dịch thất bại hoặc người mua đã hủy thanh toán. Bạn có thể thử lại nếu đơn còn đủ điều kiện.</p></c:when>
<c:when test="${attempt.status == 'EXPIRED' or attempt.status == 'SUPERSEDED'}"><h1>Lần thanh toán đã đóng</h1><p>Lần này đã hết hạn hoặc đã có giao dịch khác xác nhận. Hãy kiểm tra trạng thái đơn trước khi thử lại.</p></c:when>
<c:otherwise><h1>Đang xác nhận thanh toán</h1><p>Thông tin trên URL trình duyệt chưa làm đơn được thanh toán. Bấm kiểm tra kết quả để backend đối chiếu với VNPAY.</p><p class="muted">Khi chạy localhost, VNPAY không gọi được IPN cục bộ. Có thể cần đợi rồi kiểm tra lại sau 30 giây.</p></c:otherwise>
</c:choose>
<c:if test="${not empty queryNotice}"><p class="notice" role="status"><c:out value="${queryNotice}" /></p></c:if>
<div class="payment-summary"><div><span>Số tiền của đơn</span><strong class="price"><fmt:formatNumber value="${order.grand_total}" pattern="#,##0" /> ₫</strong></div><div><span>Trạng thái đơn</span><strong>${labels[order.status]}</strong></div><div><span>Thanh toán đã lưu</span><strong>${labels[payment.status]}</strong></div></div>
<p>Mã tham chiếu: <code class="txn-ref"><c:out value="${attempt.txn_ref}" /></code></p>
<div class="actions">
<c:if test="${canQuery}"><form method="post" action="<c:url value='/payments/vnpay/query' />" data-payment-submit="Đang kiểm tra…"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="ref" value="${attempt.txn_ref}"><button>Kiểm tra kết quả thanh toán</button></form></c:if>
<c:if test="${canPay}"><form method="post" action="<c:url value='/payments/vnpay/create' />" data-payment-submit="Đang chuyển sang VNPAY…"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="orderId" value="${order.id}"><button class="secondary">${attempt.status == 'FAILED' or attempt.status == 'EXPIRED' ? 'Thanh toán lại' : 'Tiếp tục thanh toán VNPAY'}</button></form></c:if>
<a class="button secondary" href="<c:url value='/buyer/orders/detail'><c:param name='id' value='${order.id}' /></c:url>">Xem đơn hàng</a><a href="<c:url value='/buyer/orders' />">Danh sách đơn mua</a>
</div></section>
<%@ include file="attempts.jspf" %>
<%@ include file="../layouts/shop-end.jspf" %>
