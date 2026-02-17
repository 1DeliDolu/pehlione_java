package com.pehlione.web.category;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;

@Service
public class CategoryService {

	private final CategoryRepository repo;

	public CategoryService(CategoryRepository repo) {
		this.repo = repo;
	}

	@Transactional
	public Category create(Category c) {
		if (repo.existsBySlug(c.getSlug())) {
			throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, "Category slug already exists");
		}
		return repo.save(c);
	}

	@Transactional(readOnly = true)
	public Category getOrThrow(Long id) {
		return repo.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND, "Category not found"));
	}

	@Transactional
	public Category update(Long id, String name) {
		Category c = getOrThrow(id);
		c.setName(name.trim());
		return c;
	}

	@Transactional
	public void delete(Long id) {
		repo.delete(getOrThrow(id));
	}
}
