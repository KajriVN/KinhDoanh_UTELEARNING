package com.utelearn.service;

import com.utelearn.entity.Category;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.CategoryRepository;
import com.utelearn.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    public CategoryService(CategoryRepository categoryRepository, CourseRepository courseRepository) {
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục"));
    }

    // id = null -> thêm mới, ngược lại -> cập nhật
    @Transactional
    public void save(Long id, String name, String description, String icon) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên danh mục không được để trống");
        }
        name = name.trim();

        Category category;
        if (id == null) {
            if (categoryRepository.existsByNameIgnoreCase(name)) {
                throw new BusinessException("Danh mục \"" + name + "\" đã tồn tại");
            }
            category = new Category();
        } else {
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                throw new BusinessException("Danh mục \"" + name + "\" đã tồn tại");
            }
            category = findById(id);
        }
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon == null || icon.isBlank() ? "bi-folder" : icon.trim());
        categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        if (courseRepository.countByCategory(category) > 0) {
            throw new BusinessException("Danh mục đang có khóa học, không thể xóa");
        }
        categoryRepository.delete(category);
    }

    // đếm số khóa học của từng danh mục: key = categoryId
    public Map<Long, Long> countCoursesPerCategory() {
        Map<Long, Long> result = new HashMap<>();
        for (Category c : findAll()) {
            result.put(c.getId(), courseRepository.countByCategory(c));
        }
        return result;
    }
}
