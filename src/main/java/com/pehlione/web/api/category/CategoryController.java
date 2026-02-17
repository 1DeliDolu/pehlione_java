package com.pehlione.web.api.category;

import static com.pehlione.web.api.category.CategoryDtos.CategoryResponse;
import static com.pehlione.web.api.category.CategoryDtos.CreateCategoryRequest;
import static com.pehlione.web.api.category.CategoryDtos.UpdateCategoryRequest;

import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.category.Category;
import com.pehlione.web.category.CategoryRepository;
import com.pehlione.web.category.CategoryService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Categories", description = "Product category endpoints")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryRepository repo;
	private final CategoryService service;

	public CategoryController(CategoryRepository repo, CategoryService service) {
		this.repo = repo;
		this.service = service;
	}

	@GetMapping
	public List<CategoryResponse> list() {
		return repo.findAll().stream().map(CategoryResponse::from).toList();
	}

	@SecurityRequirement(name = "bearerAuth")
	@PostMapping
	public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest req) {
		Category c = new Category();
		c.setSlug(req.slug().trim().toLowerCase(Locale.ROOT));
		c.setName(req.name().trim());
		return CategoryResponse.from(service.create(c));
	}

	@SecurityRequirement(name = "bearerAuth")
	@PutMapping("/{id}")
	public CategoryResponse update(@PathVariable("id") Long id, @Valid @RequestBody UpdateCategoryRequest req) {
		return CategoryResponse.from(service.update(id, req.name()));
	}

	@SecurityRequirement(name = "bearerAuth")
	@DeleteMapping("/{id}")
	public void delete(@PathVariable("id") Long id) {
		service.delete(id);
	}
}
