package com.pehlione.web.product;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pehlione.web.api.error.ApiErrorCode;
import com.pehlione.web.api.error.ApiException;

@Service
public class ProductImageStorageService {

	private static final String PUBLIC_PREFIX = "/uploads/product-images/";
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

	private final Path storageRoot;

	public ProductImageStorageService(
			@Value("${app.images.storage-root:uploads/product-images}") String storageRoot) {
		this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
	}

	public String store(Product product, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Image file is required");
		}

		String extension = resolveExtension(file);
		String categoryFolder = resolveCategoryFolder(product);
		String productFolder = resolveProductFolder(product);

		Path targetDirectory = storageRoot.resolve(categoryFolder).resolve(productFolder).normalize();
		if (!targetDirectory.startsWith(storageRoot)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Invalid image target directory");
		}
		try {
			Files.createDirectories(targetDirectory);
			Path targetFile = targetDirectory.resolve(buildFilename(extension)).normalize();
			if (!targetFile.startsWith(storageRoot)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED, "Invalid image target file");
			}
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
			String relative = storageRoot.relativize(targetFile).toString().replace('\\', '/');
			return PUBLIC_PREFIX + relative;
		}
		catch (IOException ex) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR, "Failed to store image");
		}
	}

	public void deleteIfManaged(String url) {
		if (url == null || !url.startsWith(PUBLIC_PREFIX)) {
			return;
		}
		String relativePath = url.substring(PUBLIC_PREFIX.length());
		Path absolute = storageRoot.resolve(relativePath).normalize();
		if (!absolute.startsWith(storageRoot)) {
			return;
		}
		try {
			Files.deleteIfExists(absolute);
			cleanupEmptyParents(absolute.getParent());
		}
		catch (IOException ex) {
			// orphan file cleanup failure should not block normal delete flow
		}
	}

	private void cleanupEmptyParents(Path directory) throws IOException {
		Path current = directory;
		while (current != null && current.startsWith(storageRoot) && !current.equals(storageRoot)) {
			if (!Files.exists(current)) {
				current = current.getParent();
				continue;
			}
			try (var stream = Files.list(current)) {
				if (stream.findAny().isPresent()) {
					break;
				}
			}
			Files.deleteIfExists(current);
			current = current.getParent();
		}
	}

	private String resolveCategoryFolder(Product product) {
		if (product == null || product.getCategories() == null || product.getCategories().isEmpty()) {
			return "uncategorized";
		}
		return product.getCategories().stream()
				.map(c -> c.getSlug() == null ? "" : c.getSlug())
				.filter(s -> !s.isBlank())
				.map(this::sanitizeSegment)
				.filter(s -> !s.isBlank())
				.sorted(Comparator.naturalOrder())
				.findFirst()
				.orElse("uncategorized");
	}

	private String resolveProductFolder(Product product) {
		String name = product == null || product.getName() == null ? "product" : product.getName();
		String idPart = product != null && product.getId() != null ? String.valueOf(product.getId()) : "new";
		String slug = sanitizeSegment(name);
		if (slug.isBlank()) {
			slug = "product";
		}
		return slug + "-" + idPart;
	}

	private String buildFilename(String extension) {
		return Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
	}

	private String resolveExtension(MultipartFile file) {
		String originalName = file.getOriginalFilename();
		if (originalName != null) {
			int dot = originalName.lastIndexOf('.');
			if (dot >= 0 && dot < originalName.length() - 1) {
				String ext = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
				if (ALLOWED_EXTENSIONS.contains(ext)) {
					return ext;
				}
			}
		}

		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		String inferred = switch (contentType) {
		case "image/jpeg", "image/jpg" -> "jpg";
		case "image/png" -> "png";
		case "image/webp" -> "webp";
		case "image/gif" -> "gif";
		default -> "";
		};
		if (ALLOWED_EXTENSIONS.contains(inferred)) {
			return inferred;
		}
		throw new ApiException(
				HttpStatus.BAD_REQUEST,
				ApiErrorCode.VALIDATION_FAILED,
				"Only image types jpg, jpeg, png, webp, gif are allowed");
	}

	private String sanitizeSegment(String raw) {
		if (raw == null) {
			return "";
		}
		String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-+|-+$", "");
		return normalized;
	}
}
