package com.utelearn.service;

import com.utelearn.entity.CourseStatus;
import com.utelearn.entity.OrderStatus;
import com.utelearn.entity.User;
import com.utelearn.repository.CourseRepository;
import com.utelearn.repository.OrderItemRepository;
import com.utelearn.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Số liệu thống kê cho biểu đồ admin + trang doanh thu giảng viên
@Service
public class StatsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CourseRepository courseRepository;

    public StatsService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        CourseRepository courseRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.courseRepository = courseRepository;
    }

    public record NamedValue(String name, long value) {
    }

    // 1 dòng thống kê doanh thu của giảng viên theo khóa
    public record CourseRevenue(Long courseId, String title, long sold, BigDecimal revenue) {
    }

    // key "MM/yyyy" theo thứ tự thời gian, tháng không có đơn = 0
    public Map<String, BigDecimal> revenueLast12Months() {
        return fillMonths(orderRepository.revenueByMonth(OrderStatus.PAID,
                YearMonth.now().minusMonths(11).atDay(1).atStartOfDay()), 12, 2);
    }

    public List<NamedValue> topCourses(int limit) {
        List<NamedValue> result = new ArrayList<>();
        for (Object[] row : orderItemRepository.topCourses(OrderStatus.PAID, PageRequest.of(0, limit))) {
            result.add(new NamedValue((String) row[0], ((Number) row[1]).longValue()));
        }
        return result;
    }

    public List<NamedValue> publishedCoursesByCategory() {
        List<NamedValue> result = new ArrayList<>();
        for (Object[] row : courseRepository.countByCategoryName(CourseStatus.PUBLISHED)) {
            result.add(new NamedValue((String) row[0], ((Number) row[1]).longValue()));
        }
        return result;
    }

    // ================== GIẢNG VIÊN ==================

    public Map<String, BigDecimal> instructorRevenueLast12Months(User instructor) {
        List<Object[]> rows = orderItemRepository.instructorRevenueByMonth(OrderStatus.PAID, instructor);
        return fillMonths(rows, 12, 2);
    }

    public List<CourseRevenue> instructorRevenueByCourse(User instructor) {
        List<CourseRevenue> result = new ArrayList<>();
        for (Object[] row : orderItemRepository.instructorRevenueByCourse(OrderStatus.PAID, instructor)) {
            result.add(new CourseRevenue((Long) row[0], (String) row[1], ((Number) row[2]).longValue(),
                    toDecimal(row[3])));
        }
        return result;
    }

    // tổng doanh thu mọi tháng của giảng viên
    public BigDecimal instructorTotal(User instructor) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] row : orderItemRepository.instructorRevenueByMonth(OrderStatus.PAID, instructor)) {
            total = total.add(toDecimal(row[2]));
        }
        return total;
    }

    // ================== DÙNG CHUNG ==================

    // rows: [năm, tháng, ..., giá trị ở cột valueIndex] -> 12 tháng gần nhất, thiếu thì điền 0
    private Map<String, BigDecimal> fillMonths(List<Object[]> rows, int months, int valueIndex) {
        Map<YearMonth, BigDecimal> byMonth = new LinkedHashMap<>();
        for (Object[] row : rows) {
            YearMonth ym = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            byMonth.put(ym, toDecimal(row[valueIndex]));
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            result.put(String.format("%02d/%d", ym.getMonthValue(), ym.getYear()),
                    byMonth.getOrDefault(ym, BigDecimal.ZERO));
        }
        return result;
    }

    private BigDecimal toDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
