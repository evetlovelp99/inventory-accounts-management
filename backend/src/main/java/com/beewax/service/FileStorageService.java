package com.beewax.service;

import com.beewax.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
	private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

	private final Path inspectReportDir;
	private final String publicBasePath;

	public FileStorageService(
			@Value("${app.upload.dir:uploads}") String uploadDir,
			@Value("${app.upload.inspect-report-subdir:inspect-reports}") String inspectReportSubdir) {
		this.inspectReportDir = Paths.get(uploadDir, inspectReportSubdir).toAbsolutePath().normalize();
		this.publicBasePath = "/api/files/inspect-reports";
	}

	public String storeInspectReport(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(400, "请选择检测报告文件");
		}
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new BusinessException(400, "检测报告文件不能超过 10MB");
		}

		String extension = resolveExtension(file.getOriginalFilename());
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException(400, "仅支持 PDF、JPG、PNG 格式的检测报告");
		}

		try {
			Files.createDirectories(inspectReportDir);
			String storedName = UUID.randomUUID() + "." + extension;
			Path target = inspectReportDir.resolve(storedName).normalize();
			if (!target.startsWith(inspectReportDir)) {
				throw new BusinessException(400, "无效的文件名");
			}
			file.transferTo(target);
			return publicBasePath + "/" + storedName;
		} catch (IOException ex) {
			throw new BusinessException(500, "文件保存失败，请稍后重试");
		}
	}

	public Resource loadInspectReport(String filename) {
		String safeName = Paths.get(filename).getFileName().toString();
		if (safeName.isBlank()) {
			throw new BusinessException(400, "无效的文件名");
		}

		Path filePath = inspectReportDir.resolve(safeName).normalize();
		if (!filePath.startsWith(inspectReportDir)) {
			throw new BusinessException(400, "无效的文件名");
		}
		if (!Files.exists(filePath)) {
			throw new BusinessException(404, "文件不存在");
		}

		try {
			Resource resource = new UrlResource(filePath.toUri());
			if (!resource.exists() || !resource.isReadable()) {
				throw new BusinessException(404, "文件不存在");
			}
			return resource;
		} catch (MalformedURLException ex) {
			throw new BusinessException(500, "服务器内部错误");
		}
	}

	private String resolveExtension(String originalFilename) {
		if (originalFilename == null || !originalFilename.contains(".")) {
			return "";
		}
		return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
	}
}
