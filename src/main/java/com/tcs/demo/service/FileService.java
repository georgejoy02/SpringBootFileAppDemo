package com.tcs.demo.service;

import com.tcs.demo.dto.SearchResult;
import com.tcs.demo.model.FileEntity;
import com.tcs.demo.repository.FileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class FileService {

	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	@Autowired
	private FileRepository fileRepository;

	// Remove comments of the form ///*...*/ using a regular expression.
	public String removeComments(String content) {
		String regexPattern = "((['\"])(?:(?!\\2|\\\\).|\\\\.)*\\2)|\\/\\/[^\\n]*|\\/\\*(?:[^*]|\\*(?!\\/))*\\*\\/";
		return content.replaceAll(regexPattern, "$1");
	}

	// Save the uploaded file after processing its content.
	public FileEntity saveFile(MultipartFile file) throws Exception {
		// Read original content and remove comments.
		String originalContent = new String(file.getBytes());
		String cleanedContent = removeComments(originalContent);

		ObjectMapper mapper = new ObjectMapper();
		String newFileName = file.getOriginalFilename();

		try {
			// Attempt to parse the cleaned content as JSON.
			JsonNode jsonNode = mapper.readTree(cleanedContent);

			cleanedContent = mapper.writeValueAsString(jsonNode);
			// If parsing succeeds and the filename ends with .txt, change the extension to .json.
			if (newFileName != null && newFileName.toLowerCase().endsWith(".txt")) {
				newFileName = newFileName.substring(0, newFileName.lastIndexOf('.')) + ".json";
			}
		} catch (JsonProcessingException e) {
			logger.error("An error occurred: ", e);
		}

		FileEntity fileEntity = new FileEntity();
		fileEntity.setFileName(newFileName);
		fileEntity.setContent(cleanedContent);
		return fileRepository.save(fileEntity);
	}

	public FileEntity getFile(Long id) {
		Optional<FileEntity> opt = fileRepository.findById(id);
		return opt.orElse(null);
	}

	// Recursively search JSON structure for a value. Returns the key if found.
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
		} else if (node instanceof List) { // Changed from JavaList to List
			for (Object item : (List<?>) node) { // Changed from JavaList<?> to List<?>
				String res = searchJsonForKey(item, searchValue);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	// Search all JSON files stored in the DB for the given value.
	public List<SearchResult> searchJsonFiles(String searchValue) {
		List<SearchResult> results = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		List<FileEntity> allFiles = fileRepository.findAll();

		// Filter files that are JSON (by file extension)
		allFiles.stream().filter(file -> file.getFileName() != null && file.getFileName().endsWith(".json"))
				.forEach(file -> {
					try {
						Object json = mapper.readValue(file.getContent(), Object.class);
						String foundKey = searchJsonForKey(json, searchValue);
						if (foundKey != null) {
							results.add(new SearchResult(file.getFileName(), foundKey));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
		return results;
	}
}
