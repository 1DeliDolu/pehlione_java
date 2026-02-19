package com.pehlione.web.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

	@Value("${app.images.storage-root:uploads/product-images}")
	private String imageStorageRoot;

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("home");
		registry.addViewController("/home").setViewName("home");
		registry.addViewController("/products").setViewName("product");
		registry.addViewController("/hello").setViewName("hello");
		registry.addViewController("/admin").setViewName("admin");
		registry.addViewController("/login").setViewName("login");
		registry.addViewController("/register").setViewName("register");
		registry.addViewController("/adress").setViewName("adress");
		registry.addViewController("/address").setViewName("adress");
		registry.addViewController("/karte").setViewName("karte");
		registry.addViewController("/card").setViewName("karte");
		registry.addViewController("/password").setViewName("password");
		registry.addViewController("/admin-product-create").setViewName("admin-product-create");
		registry.addViewController("/impressum").setViewName("impressum");
		registry.addViewController("/datenschutz").setViewName("datenschutz");
		registry.addViewController("/agb").setViewName("agb");
		registry.addViewController("/widerruf").setViewName("widerruf");
		registry.addViewController("/versand-zahlung").setViewName("versand-zahlung");
		registry.addViewController("/kontakt").setViewName("kontakt");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path root = Path.of(imageStorageRoot).toAbsolutePath().normalize();
		String location = root.toUri().toString();
		if (!location.endsWith("/")) {
			location = location + "/";
		}
		registry.addResourceHandler("/uploads/product-images/**")
				.addResourceLocations(location);
	}
}
