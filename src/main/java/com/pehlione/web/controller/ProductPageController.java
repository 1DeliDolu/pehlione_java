package com.pehlione.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductPageController {

	@GetMapping("/products/{id}")
	public String productDetail(@PathVariable("id") Long id, Model model) {
		model.addAttribute("productId", id);
		return "product-detail";
	}
}
