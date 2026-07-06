package com.beewax.controller;

import com.beewax.dto.response.ApiResponse;
import com.beewax.dto.response.FileUploadResponse;
import com.beewax.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

	private final FileStorageService fileStorageService;

	public FileController(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	@PostMapping(value = "/inspect-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<FileUploadResponse> uploadInspectReport(@RequestPart("file") MultipartFile file) {
		String url = fileStorageService.storeInspectReport(file);
		return ApiResponse.ok(new FileUploadResponse(url));
	}

	@GetMapping("/inspect-reports/{filename}")
	public ResponseEntity<Resource> downloadInspectReport(@PathVariable String filename) {
		Resource resource = fileStorageService.loadInspectReport(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
				.body(resource);
	}
}
