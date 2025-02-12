package com.tcs.demo.service;

import com.tcs.demo.dto.FileUploadResponse;
import com.tcs.demo.dto.SearchResult;
import com.tcs.demo.model.FileEntity;
import com.tcs.demo.repository.FileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Objects;

@Service
public class FileService {

	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	@Autowired
	private FileRepository fileRepository;

	public String removeComments(String content) {
		String regexPattern = "((['\"])(?:(?!\\2|\\\\).|\\\\.)*\\2)" + "|\\/\\/[^\\n]*"
				+ "|\\/\\*(?:[^*]|\\*(?!\\/))*\\*\\/";
		
		/** 
		 * regex 
		 * 
		 * ((['"])(?:(?!\2|\\).|\\.)*\2)
		 * |\/\/[^\n]*
		 * |\/\*(?:[^*]|\*(?!\/))*\*\/ 
		 * 
		 */
		
		return content.replaceAll(regexPattern, "$1");
	}

	@Async
	public CompletableFuture<FileUploadResponse> saveFile(MultipartFile file) throws Exception {

		String originalContent = new String(file.getBytes());
		String cleanedContent = removeComments(originalContent);

		// logger.info("before json parsing of" + file.getOriginalFilename() +
		// cleanedContent);

		String originalFileName = file.getOriginalFilename();
		String newFileName = originalFileName;

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);

		try {
			JsonNode jsonNode = mapper.readTree(cleanedContent);

			cleanedContent = mapper.writeValueAsString(jsonNode);

			// logger.info("after json parsing of " + originalFileName + cleanedContent);

			if (originalFileName != null && originalFileName.toLowerCase().endsWith(".txt")) {
				newFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + ".json";
			}
		} catch (JsonProcessingException e) {
			logger.warn(originalFileName+" content cant be parsed to json");
		}

		FileEntity fileEntity = new FileEntity();
		fileEntity.setFileName(newFileName);
		fileEntity.setContent(cleanedContent);

		FileEntity savedEntity = fileRepository.save(fileEntity);

		FileUploadResponse uploadResponse = new FileUploadResponse();
		uploadResponse.setId(savedEntity.getId());
		uploadResponse.setOriginalFileName(originalFileName);
		uploadResponse.setChangedFileName(newFileName);

		return CompletableFuture.completedFuture(uploadResponse);

	}

	public FileEntity getFile(Long id) {
		Optional<FileEntity> opt = fileRepository.findById(id);
		return opt.orElse(null);
	}

	private String searchJsonForKey(Object node, String searchValue) {
		if (node instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) node;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof String) {
					if (value.equals(searchValue)) {
						return entry.getKey().toString();
					}
				} else {
					String res = searchJsonForKey(value, searchValue);
					if (res != null) {
						return res;
					}
				}
			}
		} else if (node instanceof List) {
			for (Object item : (List<?>) node) {
				String res = searchJsonForKey(item, searchValue);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	public List<SearchResult> searchJsonFiles(String searchValue) {
		ObjectMapper mapper = new ObjectMapper();
		List<FileEntity> allFiles = fileRepository.findAll();

		List<SearchResult> results = allFiles.parallelStream()
				.filter(file -> file.getFileName() != null && file.getFileName().endsWith(".json"))
				.map(file -> {
					try {
						Object json = mapper.readValue(file.getContent(), Object.class);
						String foundKey = searchJsonForKey(json, searchValue);
						if (foundKey != null) {
							return new SearchResult(file.getFileName(), foundKey);
						}
					} catch (Exception e) {
						logger.error("An error occurred: ", e);
					}
					return null;
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		return results;
	}
}
