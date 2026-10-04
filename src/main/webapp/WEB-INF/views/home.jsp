<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nền tảng C2C — Nhóm 6</title>
    <link rel="stylesheet" href="<c:url value='/assets/css/startup.css' />">
</head>
<body><main>
    <%@ include file="layouts/navigation.jspf" %>
    <p class="eyebrow">Đồ án Thương mại điện tử · Nhóm 6</p>
    <h1>Nền tảng kết nối người mua – người bán</h1>
    <p>Một tài khoản để cùng mua và bán. Giai đoạn M2 đã có đăng ký, đăng nhập và hồ sơ cá nhân.</p>
    <c:if test="${param.notice == 'logged-out'}"><p class="notice" role="status">Bạn đã đăng xuất.</p></c:if>
    <section><h2>Trạng thái ứng dụng</h2>
        <c:choose>
            <c:when test="${configurationValid}"><p class="ok">Servlet, JSP/JSTL và UTF-8 hoạt động.</p></c:when>
            <c:otherwise><p>Cấu hình chưa hợp lệ. Kiểm tra file cục bộ và log Tomcat.</p></c:otherwise>
        </c:choose>
        <c:if test="${diagnosticsEnabled}">
            <p><strong>JDBI:</strong> <c:out value="${databaseHealth.message}" /></p>
            <a href="<c:url value='/health/db' />">Kiểm tra kết nối database</a>
        </c:if>
    </section>
    <section><h2>Phạm vi hiện tại</h2>
        <p>Bộ khung, SQL, kết nối database và chức năng tài khoản. Đăng bán, giỏ hàng, đơn hàng, đánh giá và khiếu nại sẽ được triển khai ở các mốc tiếp theo.</p>
        <a href="<c:url value='/hello-servlet' />">Servlet mẫu hiện có</a>
    </section>
</main></body></html>
