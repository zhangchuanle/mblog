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
package me.qyh.blog.core.templatedata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.MapBindingResult;

import me.qyh.blog.core.entity.BlogFile;
import me.qyh.blog.core.entity.BlogFile.BlogFileType;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.pageparam.BlogFileQueryParam;
import me.qyh.blog.core.pageparam.PageResult;
import me.qyh.blog.core.service.FileService;
import me.qyh.blog.core.vo.DataTagProcessor;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.validator.BlogFileQueryParamValidator;

public class FilesDataTagProcessor extends DataTagProcessor<PageResult<BlogFile>> {

	@Autowired
	private FileService fileService;
	@Autowired
	private BlogFileQueryParamValidator validator;

	public FilesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected PageResult<BlogFile> query(Attributes attributes) throws LogicException {

		BlogFileQueryParam param = new BlogFileQueryParam();

		String extensionStr = attributes.get("extensions");
		if (!Validators.isEmptyOrNull(extensionStr, true)) {
			param.setExtensions(Arrays.stream(extensionStr.split(",")).collect(Collectors.toSet()));
		}
		param.setPageSize(attributes.getInteger("pageSize", 0));
		param.setCurrentPage(attributes.getInteger("currentPage", 0));
		param.setType(attributes.getEnum("type", BlogFileType.class, null));
		param.setName(attributes.get("name"));

		validator.validate(param, new MapBindingResult(new HashMap<>(), "blogFileQueryParam"));

		String path = attributes.get("path");
		return fileService.queryFiles(path, param);
	}

}
