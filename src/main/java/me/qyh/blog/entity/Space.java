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
package me.qyh.blog.entity;

import java.sql.Timestamp;
import java.util.Objects;

import me.qyh.blog.util.Validators;

/**
 * 
 * @author Administrator
 *
 */
public class Space extends BaseLockResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String name;// 空间名
	private String alias;
	private Timestamp createDate;// 创建时间
	private Boolean isPrivate;

	private Boolean articleHidden;
	private Boolean isDefault;
	private Integer articlePageSize;

	/**
	 * default
	 */
	public Space() {
		super();
	}

	/**
	 * 
	 * @param id
	 *            空间id
	 */
	public Space(Integer id) {
		super(id);
	}

	/**
	 * 
	 * @param alias
	 *            空间alias
	 */
	public Space(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getResourceId() {
		return "Space-" + alias;
	}

	public Boolean getIsPrivate() {
		return isPrivate;
	}

	public void setIsPrivate(Boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public Boolean getArticleHidden() {
		return articleHidden;
	}

	public void setArticleHidden(Boolean articleHidden) {
		this.articleHidden = articleHidden;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public Integer getArticlePageSize() {
		return articlePageSize;
	}

	public void setArticlePageSize(Integer articlePageSize) {
		this.articlePageSize = articlePageSize;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (Validators.baseEquals(this, obj)) {
			Space rhs = (Space) obj;
			return Objects.equals(this.id, rhs.id);
		}
		return false;
	}

}
