package com.utelearn.controller.admin;

import com.utelearn.entity.Coupon;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CouponService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // 1 trang: bên trái là bảng mã, bên phải là form thêm/sửa
    @GetMapping
    public String list(@RequestParam(required = false) Long editId, Model model) {
        model.addAttribute("coupons", couponService.findAll());
        model.addAttribute("editing", editId != null ? couponService.findById(editId) : new Coupon());
        return "admin/coupons";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String code,
                       @RequestParam int discountPercent,
                       @RequestParam(defaultValue = "0") int maxUses,
                       @RequestParam(required = false)
                       @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime expiredAt,
                       @RequestParam(defaultValue = "false") boolean active,
                       RedirectAttributes redirect) {
        try {
            couponService.save(id, code, discountPercent, maxUses, expiredAt, active);
            redirect.addFlashAttribute("success", id == null ? "Đã thêm mã giảm giá" : "Đã cập nhật mã giảm giá");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            couponService.delete(id);
            redirect.addFlashAttribute("success", "Đã xóa mã giảm giá");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }
}
