package com.utelearn.controller;

import com.utelearn.dto.ChangePasswordForm;
import com.utelearn.dto.ProfileForm;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profilePage(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());

        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        form.setBio(user.getBio());

        model.addAttribute("profileForm", form);
        model.addAttribute("passwordForm", new ChangePasswordForm());
        return "profile/profile";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
            return "profile/profile";
        }
        User user = userService.findByEmail(principal.getName());
        try {
            userService.updateProfile(user, form);
            redirect.addFlashAttribute("success", "Cập nhật hồ sơ thành công");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult result,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName());
        if (result.hasErrors()) {
            ProfileForm profileForm = new ProfileForm();
            profileForm.setFullName(user.getFullName());
            profileForm.setPhone(user.getPhone());
            profileForm.setBio(user.getBio());
            model.addAttribute("profileForm", profileForm);
            return "profile/profile";
        }
        try {
            userService.changePassword(user, form);
            redirect.addFlashAttribute("success", "Đổi mật khẩu thành công");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
