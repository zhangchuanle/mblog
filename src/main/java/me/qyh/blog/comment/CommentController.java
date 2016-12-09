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
package me.qyh.blog.comment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.entity.Article;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.controller.BaseController;

@Controller
public class CommentController extends BaseController {

	@Autowired
	private DftCommentService commentService;
	@Autowired
	private CommentValidator commentValidator;

	@InitBinder(value = "comment")
	protected void initCommentBinder(WebDataBinder binder) {
		binder.setValidator(commentValidator);
	}

	@RequestMapping(value = "comment/config", method = RequestMethod.GET)
	@ResponseBody
	public JsonResult getConfig() {
		return new JsonResult(true, commentService.getCommentConfig());
	}

	@RequestMapping(value = "space/{alias}/article/{id}/addComment", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult addComment(@RequestParam(value = "validateCode", required = false) String validateCode,
			@RequestBody @Validated Comment comment, @PathVariable("id") Integer articleId, HttpServletRequest req)
			throws LogicException {
		if (UserContext.get() == null) {
			HttpSession session = req.getSession(false);
			if (!Webs.matchValidateCode(validateCode, session)) {
				return new JsonResult(false, new Message("validateCode.error", "验证码错误"));
			}
		}
		comment.setArticle(new Article(articleId));
		comment.setIp(Webs.getIp(req));
		return new JsonResult(true, commentService.insertComment(comment));
	}

	@RequestMapping(value = "space/{alias}/article/{articleId}/comment/{id}/conversations")
	@ResponseBody
	public JsonResult queryConversations(@PathVariable("articleId") Integer articleId, @PathVariable("id") Integer id)
			throws LogicException {
		return new JsonResult(true, commentService.queryConversations(articleId, id));
	}
}
