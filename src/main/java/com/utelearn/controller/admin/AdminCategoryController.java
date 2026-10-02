package com.utelearn.controller.admin;

import com.utelearn.entity.Category;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 1 trang: bên trái là bảng danh mục, bên phải là form thêm/sửa
    @GetMapping
    public String list(@RequestParam(required = false) Long editId, Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("courseCounts", categoryService.countCoursesPerCategory());
        model.addAttribute("editing", editId != null ? categoryService.findById(editId) : new Category());
        return "admin/categories";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) String icon,
                       RedirectAttributes redirect) {
        try {
            categoryService.save(id, name, description, icon);
            redirect.addFlashAttribute("success", id == null ? "Đã thêm danh mục" : "Đã cập nhật danh mục");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            categoryService.delete(id);
            redirect.addFlashAttribute("success", "Đã xóa danh mục");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
