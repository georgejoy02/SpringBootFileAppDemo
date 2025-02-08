package com.tcs.demo.controller;

import com.tcs.demo.dto.SearchRequest;
import com.tcs.demo.dto.SearchResult;
import com.tcs.demo.model.FileEntity;
import com.tcs.demo.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileController {

	@Autowired
	private FileService fileService;

	// Endpoint to upload a file.
	@PostMapping("/upload")
	public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		try {
			FileEntity fileEntity = fileService.saveFile(file);
			response.put("id", fileEntity.getId());
			response.put("fileName", fileEntity.getFileName());
			response.put("message", "File uploaded successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("id", null);
			response.put("fileName", file.getOriginalFilename());
			response.put("message", "Error uploading file: " + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	// Endpoint to download a file by its ID.
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

	// Endpoint to search JSON files for a given value.
	@PostMapping("/search")
	public ResponseEntity<List<SearchResult>> searchJson(@RequestBody SearchRequest request) {
		List<SearchResult> results = fileService.searchJsonFiles(request.getValue());
		return ResponseEntity.ok(results);
	}
}
