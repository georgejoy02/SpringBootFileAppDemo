package com.tcs.demo.controller;

import com.tcs.demo.dto.FileUploadResponse;
import com.tcs.demo.dto.SearchRequest;
import com.tcs.demo.dto.SearchResult;
import com.tcs.demo.model.FileEntity;
import com.tcs.demo.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FileController {

	@Autowired
	private FileService fileService;

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

	@PostMapping("/upload")
	public ResponseEntity<Map<String, Object>> uploadFiles(@RequestParam Map<String, MultipartFile> fileMap) {
		Map<String, Object> response = new HashMap<>();

		try {
			List<CompletableFuture<FileUploadResponse>> futures = fileMap.values().parallelStream()
					.map(file -> {
						try {
							return fileService.saveFile(file);
						} catch (Exception e) {
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			List<FileUploadResponse> results = futures.stream()
					.map(CompletableFuture::join)
					.collect(Collectors.toList());

			response.put("files", results);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("error from upload: \n", e);
			response.put("message", "File upload failed: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/download/{id}")
	public ResponseEntity<?> downloadFile(@PathVariable Long id) {
		FileEntity fileEntity = fileService.getFile(id);
		if (fileEntity != null) {
			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"" + fileEntity.getFileName() + "\"").body(fileEntity.getContent());
		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("/search")
	public ResponseEntity<List<SearchResult>> searchJson(@RequestBody SearchRequest request) {
		List<SearchResult> results = fileService.searchJsonFiles(request.getValue());
		return ResponseEntity.ok(results);
	}
}
