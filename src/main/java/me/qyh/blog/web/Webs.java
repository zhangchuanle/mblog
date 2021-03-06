/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Jsons;
import me.qyh.blog.util.UrlUtils;

public class Webs {

	private static final String[] UNLOCK_PATTERNS = { "/unlock", "/unlock/", "/space/*/unlock", "/space/*/unlock/" };

	private Webs() {

	}

	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	public static void writeInfo(HttpServletResponse response, JsonResult result) throws IOException {
		response.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(Constants.CHARSET.name());
		Jsons.write(result, response.getWriter());
	}

	public static boolean isAction(HttpServletRequest request) {
		return FileUtils.getFileExtension(request.getRequestURI()).trim().isEmpty();
	}

	public static String decode(String toDecode) {
		try {
			return URLDecoder.decode(toDecode, Constants.CHARSET.name());
		} catch (UnsupportedEncodingException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public static boolean unlockRequest(HttpServletRequest request) {
		String path = request.getRequestURI();
		for (String unlockPattern : UNLOCK_PATTERNS) {
			if (UrlUtils.match(unlockPattern, path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 保存上传的文件<br>
	 * <b>保存StandardMultipartFile.transferTo时存在异常</b>
	 * 
	 * @param mf
	 *            上传的文件
	 * @param file
	 *            保存的位置
	 * @throws IOException
	 */
	public static void save(MultipartFile mf, Path file) throws IOException {
		try (InputStream is = mf.getInputStream()) {
			Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * 从请求中获取space
	 * 
	 * @param request
	 * @return
	 */
	public static String getSpaceFromRequest(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length() + 1);
		return getSpaceFromPath(path);
	}

	/**
	 * 从路径中获取space
	 * 
	 * @param path
	 * @return
	 */
	public static String getSpaceFromPath(String path) {
		if (UrlUtils.match("space/*/**", path)) {
			return path.split("/")[1];
		}
		if (UrlUtils.match("apis/space/*/**", path)) {
			return path.split("/")[2];
		}
		return null;
	}
}
