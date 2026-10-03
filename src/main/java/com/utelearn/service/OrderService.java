package com.utelearn.service;

import com.utelearn.entity.CartItem;
import com.utelearn.entity.Coupon;
import com.utelearn.entity.Order;
import com.utelearn.entity.OrderItem;
import com.utelearn.entity.OrderStatus;
import com.utelearn.entity.PaymentMethod;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.CartItemRepository;
import com.utelearn.repository.CouponRepository;
import com.utelearn.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Tạo đơn hàng từ giỏ + xử lý thanh toán.
// Hiện tại dùng cổng thanh toán GIẢ LẬP (MOCK). VNPay/MoMo xem hand-off.html.
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final EnrollmentService enrollmentService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                        CartService cartService, EnrollmentService enrollmentService,
                        CouponService couponService, CouponRepository couponRepository,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.enrollmentService = enrollmentService;
        this.couponService = couponService;
        this.couponRepository = couponRepository;
        this.notificationService = notificationService;
    }

    // Bước 1: bấm "Thanh toán" ở giỏ hàng -> tạo đơn PENDING
    @Transactional
    public Order checkout(User user, String couponCode) {
        List<CartItem> cartItems = cartService.getItems(user);
        if (cartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống");
        }

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(user);
        order.setPaymentMethod(PaymentMethod.MOCK);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            // khóa đã mua rồi thì bỏ qua (trường hợp mua ở tab khác)
            if (enrollmentService.isEnrolled(user, item.getCourse())) {
                continue;
            }
            BigDecimal price = item.getCourse().getPrice();
            order.getItems().add(new OrderItem(order, item.getCourse(), price));
            total = total.add(price);
        }
        if (order.getItems().isEmpty()) {
            throw new BusinessException("Bạn đã sở hữu tất cả khóa học trong giỏ");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponService.validate(couponCode);
            discount = couponService.calculateDiscount(coupon, total);
            order.setCoupon(coupon);
        }
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setFinalAmount(total.subtract(discount));
        return orderRepository.save(order);
    }

    // Bước 2 (giả lập): người dùng bấm "Xác nhận thanh toán" ở trang cổng thanh toán
    @Transactional
    public void confirmMockPayment(String orderCode, User user) {
        Order order = getOrderOfUser(orderCode, user);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Đơn hàng không ở trạng thái chờ thanh toán");
        }
        markPaid(order);
    }

    // Kết quả xử lý dữ liệu VNPay trả về
    public enum VnPayResult {
        PAID, FAILED, ALREADY_CONFIRMED, ORDER_NOT_FOUND, AMOUNT_MISMATCH, INVALID_SIGNATURE
    }

    // Bấm "Thanh toán VNPay": đơn phải đang chờ thanh toán, đổi phương thức sang VNPAY
    @Transactional
    public Order prepareVnPayPayment(String orderCode, User user) {
        Order order = getOrderOfUser(orderCode, user);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Đơn hàng không ở trạng thái chờ thanh toán");
        }
        order.setPaymentMethod(PaymentMethod.VNPAY);
        return orderRepository.save(order);
    }

    // Xử lý kết quả VNPay (đã kiểm tra chữ ký ở VnPayService). Dùng cho cả return URL và IPN nên phải an toàn khi gọi lặp.
    @Transactional
    public VnPayResult applyVnPayResult(Map<String, String> params) {
        String orderCode = params.get("vnp_TxnRef");
        Order order = orderCode == null ? null : orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null || order.getPaymentMethod() != PaymentMethod.VNPAY) {
            return VnPayResult.ORDER_NOT_FOUND;
        }
        long amount;
        try {
            amount = Long.parseLong(params.getOrDefault("vnp_Amount", ""));
        } catch (NumberFormatException e) {
            return VnPayResult.AMOUNT_MISMATCH;
        }
        if (amount != VnPayService.toVnpAmount(order)) {
            return VnPayResult.AMOUNT_MISMATCH;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            return VnPayResult.ALREADY_CONFIRMED;
        }
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && (transactionStatus == null || "00".equals(transactionStatus));
        if (success) {
            markPaid(order);
            return VnPayResult.PAID;
        }
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
        return VnPayResult.FAILED;
    }

    // Dùng chung cho mọi cổng thanh toán: đơn PAID -> ghi danh học viên -> xóa khỏi giỏ
    @Transactional
    public void markPaid(Order order) {
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // chỉ tính lượt dùng mã khi thanh toán thành công
        if (order.getCoupon() != null) {
            Coupon coupon = order.getCoupon();
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        for (OrderItem item : order.getItems()) {
            enrollmentService.enroll(order.getUser(), item.getCourse());
            cartItemRepository.findByUserAndCourse(order.getUser(), item.getCourse())
                    .ifPresent(cartItemRepository::delete);
            notificationService.notify(item.getCourse().getInstructor(), "Có học viên mới",
                    order.getUser().getFullName() + " vừa đăng ký khóa \"" + item.getCourse().getTitle() + "\".",
                    "/instructor/courses/" + item.getCourse().getId() + "/students");
        }
        notificationService.notify(order.getUser(), "Thanh toán thành công",
                "Đơn " + order.getOrderCode() + " đã được thanh toán, bạn có thể vào học ngay.",
                "/orders/" + order.getOrderCode());
    }

    @Transactional
    public void cancel(String orderCode, User user) {
        Order order = getOrderOfUser(orderCode, user);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Chỉ hủy được đơn đang chờ thanh toán");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public Order getOrderOfUser(String orderCode, User user) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Không tìm thấy đơn hàng");
        }
        return order;
    }

    public List<Order> getOrdersOfUser(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // ================== ADMIN ==================

    public Page<Order> searchForAdmin(OrderStatus status, LocalDate from, LocalDate to, String keyword, int page) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        // "đến ngày" tính trọn ngày đó -> chặn trên là 0h ngày hôm sau
        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("createdAt").descending());
        return orderRepository.search(status, fromTime, toTime, kw, pageable);
    }

    public Order findByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
    }

    public long countPaidOrders() {
        return orderRepository.countByStatus(OrderStatus.PAID);
    }

    public BigDecimal getTotalRevenue() {
        return orderRepository.sumFinalAmountByStatus(OrderStatus.PAID);
    }

    // vd: UTE20261002153000482
    private String generateOrderCode() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(900) + 100;
        return "UTE" + time + random;
    }
}
