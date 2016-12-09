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
package me.qyh.blog.service.impl;

import java.io.IOException;

import org.lionsoul.jcseg.analyzer.v5x.JcsegAnalyzer5X;
import org.lionsoul.jcseg.tokenizer.core.ADictionary;
import org.lionsoul.jcseg.tokenizer.core.ILexicon;
import org.lionsoul.jcseg.tokenizer.core.IWord;
import org.lionsoul.jcseg.tokenizer.core.JcsegTaskConfig;

public class JcsegArticleIndexer extends NRTArticleIndexer {

	public enum JcsegMode {

		SIMPLE(1), COMPLEX(2), DECECT(3), SEARCH(4);

		private int mode;

		private JcsegMode(int mode) {
			this.mode = mode;
		}

		public int getMode() {
			return mode;
		}
	}

	public JcsegArticleIndexer(String indexDir, JcsegMode mode) throws IOException {
		super(indexDir, new _Analyzer(mode));
	}

	@Override
	public synchronized void removeTag(String... tags) {
		ADictionary dict = ((JcsegAnalyzer5X) analyzer).getDict();
		for (String tag : tags) {
			dict.remove(ILexicon.CJK_WORD, tag);
		}
	}

	@Override
	public synchronized void addTags(String... tags) {
		ADictionary dict = ((JcsegAnalyzer5X) analyzer).getDict();
		for (String tag : tags) {
			dict.add(ILexicon.CJK_WORD, tag, IWord.T_CJK_WORD);
		}
	}

	private static final class _Analyzer extends JcsegAnalyzer5X {

		public _Analyzer(JcsegMode mode) {
			super(mode.mode);
			JcsegTaskConfig taskConfig = getTaskConfig();
			taskConfig.setClearStopwords(true);
			/**
			 * http://git.oschina.net/lionsoul/jcseg/issues/24
			 */
			taskConfig.setAppendCJKSyn(false);
			taskConfig.setLoadCJKPinyin(false);
			taskConfig.setLoadCJKSyn(false);
			taskConfig.setAppendCJKPinyin(false);
		}
	}

}
