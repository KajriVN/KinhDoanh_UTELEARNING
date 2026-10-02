package com.utelearn.util;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Dùng trong Thymeleaf: ${@fmt.money(course.price)}, ${@fmt.dateTime(order.createdAt)}
@Component("fmt")
public class FormatUtil {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // 1200000 -> "1.200.000 đ"
    public String money(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        String text = String.format(Locale.US, "%,.0f", amount);
        return text.replace(',', '.') + " đ";
    }

    // giá khóa học: 0 thì ghi "Miễn phí"
    public String price(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Miễn phí";
        }
        return money(amount);
    }

    public String dateTime(LocalDateTime time) {
        return time == null ? "" : time.format(DATE_TIME);
    }

    public String date(LocalDateTime time) {
        return time == null ? "" : time.format(DATE);
    }

    // link phân trang: giữ nguyên các tham số lọc trên URL hiện tại, chỉ đổi page
    public String pageUrl(int page) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page", page)
                .build()
                .toUriString();
    }

    // 135 phút -> "2 giờ 15 phút"
    public String duration(int minutes) {
        if (minutes < 60) {
            return minutes + " phút";
        }
        int hours = minutes / 60;
        int rest = minutes % 60;
        return rest == 0 ? hours + " giờ" : hours + " giờ " + rest + " phút";
    }
}
