package com.utelearn.service;

import com.utelearn.entity.CartItem;
import com.utelearn.entity.Order;
import com.utelearn.entity.OrderItem;
import com.utelearn.entity.OrderStatus;
import com.utelearn.entity.PaymentMethod;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.CartItemRepository;
import com.utelearn.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

// Tạo đơn hàng từ giỏ + xử lý thanh toán.
// Hiện tại dùng cổng thanh toán GIẢ LẬP (MOCK). VNPay/MoMo xem hand-off.html.
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final EnrollmentService enrollmentService;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                        CartService cartService, EnrollmentService enrollmentService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.enrollmentService = enrollmentService;
    }

    // Bước 1: bấm "Thanh toán" ở giỏ hàng -> tạo đơn PENDING
    @Transactional
    public Order checkout(User user) {
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

        // TODO (hand-off): trừ tiền theo mã giảm giá (Coupon) ở đây
        order.setTotalAmount(total);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(total);
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

    // Dùng chung cho mọi cổng thanh toán: đơn PAID -> ghi danh học viên -> xóa khỏi giỏ
    @Transactional
    public void markPaid(Order order) {
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            enrollmentService.enroll(order.getUser(), item.getCourse());
            cartItemRepository.findByUserAndCourse(order.getUser(), item.getCourse())
                    .ifPresent(cartItemRepository::delete);
        }
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
