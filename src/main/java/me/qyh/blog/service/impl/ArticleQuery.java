package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.ui.widget.ArticleDateFile;
import me.qyh.blog.ui.widget.ArticleDateFiles;
import me.qyh.blog.ui.widget.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.ui.widget.ArticleSpaceFile;
import me.qyh.util.Validators;

/**
 * 事实上，一般只有一个线程做写操作，真的需要考虑那么多吗。。
 * 
 * @author mhlx
 *
 */
public class ArticleQuery implements InitializingBean {

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private ArticleIndexer articleIndexer;

	private Map<Integer, Article> store = new ConcurrentHashMap<Integer, Article>();
	public static final Comparator<Article> defaultComparator = new ArticleCommparator() {

		@Override
		int _compare(Article o1, Article o2) {
			return 0;
		}
	};
	public static final Comparator<Article> commentsComparator = new ArticleCommparator() {

		@Override
		int _compare(Article o1, Article o2) {
			return o1.getComments() > o2.getComments() ? -1 : ((o1.getComments() < o2.getComments()) ? 1 : 0);
		}
	};

	public static final Comparator<Article> hitsComparator = new ArticleCommparator() {

		@Override
		int _compare(Article o1, Article o2) {
			return o1.getHits() > o2.getHits() ? -1 : ((o1.getHits() < o2.getHits()) ? 1 : 0);
		}
	};

	public static final Comparator<ArticleSpaceFile> articleSpaceFileComparator = new ArticleSpaceFileComparator();
	public static final Comparator<ArticleDateFile> articleDateFileComparator = new ArticleDateFileComparator();

	/**
	 * 如果开启了内存模式，将会将<strong>所有的</strong>博客都放入内存中
	 */
	private boolean memoryMode = true;

	/**
	 * 根据id查询博客
	 * 
	 * @param id
	 * @return null如果不存在
	 */
	public Article getArticle(Integer id) {
		Article article = store.get(id);
		if (!memoryMode) {
			if (article == null) {
				article = articleDao.selectById(id);
			}
		}
		return article;
	}

	public List<Article> selectPublished(Space space) {
		if (memoryMode) {
			List<Article> articles = new ArrayList<>();
			for (Article article : store.values()) {
				if (article.isPublished()) {
					if (space != null && !space.equals(article.getSpace())) {
						continue;
					}
					articles.add(article);
				}
			}
			return articles;
		} else {
			return articleDao.selectPublished(space);
		}
	}

	public PageResult<Article> query(ArticleQueryParam param) {
		if (memoryMode) {
			if (luceneQuery(param)) {
				PageResult<Integer> result = articleIndexer.query(param);
				List<Article> articles = new ArrayList<Article>();
				for (Integer id : result.getDatas()) {
					Article article = store.get(id);
					if (article == null) {
						// ?????
						result.decrease(1);
						continue;
					}
					articles.add(article);
				}
				return new PageResult<Article>(param, result.getTotalRow(), articles);
			} else {
				List<Article> results = new ArrayList<Article>();
				for (Article article : store.values()) {
					if (param.getBegin() != null && param.getEnd() != null) {
						Date pubDate = article.getPubDate();
						if (pubDate == null || pubDate.before(param.getBegin()) || pubDate.after(param.getEnd())) {
							continue;
						}
					}
					if (param.getHasLock() != null && !article.hasLock()) {
						continue;
					}
					if (param.getFrom() != null && !article.getFrom().equals(param.getFrom())) {
						continue;
					}
					if (param.getSpace() != null && !article.getSpace().equals(param.getSpace())) {
						continue;
					}
					if (param.getStatus() != null && !article.getStatus().equals(param.getStatus())) {
						continue;
					}
					if (param.getTag() != null && !article.hasTag(param.getTag())) {
						continue;
					}
					results.add(article);
				}
				if (param.getSort() == null) {
					Collections.sort(results, defaultComparator);
				} else {
					switch (param.getSort()) {
					case COMMENTS:
						Collections.sort(results, commentsComparator);
						break;
					case HITS:
						Collections.sort(results, hitsComparator);
						break;
					}
				}
				int size = results.size();
				if (param.getOffset() >= size) {
					return new PageResult<>(param, size, Collections.emptyList());
				} else {
					int endIndex = param.getOffset() + param.getPageSize();
					int max = endIndex >= size ? size : endIndex;
					return new PageResult<>(param, size, results.subList(param.getOffset(), max));
				}
			}
		} else {
			if (luceneQuery(param)) {
				PageResult<Integer> result = articleIndexer.query(param);
				List<Article> articles = result.hasResult() ? articleDao.selectByIds(result.getDatas())
						: new ArrayList<Article>();
				return new PageResult<Article>(param, result.getTotalRow(), articles);
			} else {
				int count = articleDao.selectCount(param);
				List<Article> datas = articleDao.selectPage(param);
				return new PageResult<Article>(param, count, datas);
			}
		}
	}

	public ArticleDateFiles queryArticleDateFiles(Space space, ArticleDateFileMode mode, boolean queryPrivate) {
		if (mode == null) {
			mode = ArticleDateFileMode.YM;
		}
		List<ArticleDateFile> files;
		if (memoryMode) {
			Map<Object, Integer> countMap = new HashMap<>();
			for (Article article : store.values()) {
				if (!article.isPublished() || (article.isPrivate() && !queryPrivate)
						|| (space != null && !space.equals(article.getSpace()))) {
					continue;
				}
				Calendar cal = Calendar.getInstance();
				cal.setTime(article.getPubDate());
				Object key = genDateFilesKey(mode, cal);
				Integer count = countMap.get(key);
				if (count == null) {
					count = 0;
				}
				count++;
				countMap.put(key, count);
			}
			files = new ArrayList<>(countMap.size());
			for (Map.Entry<Object, Integer> it : countMap.entrySet()) {
				ArticleDateFile file = new ArticleDateFile();
				Calendar cal = Calendar.getInstance();
				cal.clear();
				switch (mode) {
				case YM:
					YMKey key = (YMKey) it.getKey();
					cal.set(Calendar.YEAR, key.y);
					cal.set(Calendar.MONTH, key.m);
					file.setBegin(cal.getTime());
					cal.add(Calendar.MONTH, 1);
					file.setEnd(cal.getTime());
					break;
				case Y:
					Integer ykey = (Integer) it.getKey();
					cal.set(Calendar.YEAR, ykey);
					file.setBegin(cal.getTime());
					cal.add(Calendar.YEAR, 1);
					file.setEnd(cal.getTime());
					break;
				}
				file.setCount(it.getValue());
				files.add(file);
			}
			Collections.sort(files, articleDateFileComparator);
		} else {
			files = articleDao.selectDateFiles(space, mode, queryPrivate);
		}
		ArticleDateFiles _files = new ArticleDateFiles(files, mode);
		if (!memoryMode) {
			_files.calDate();
		}
		return _files;
	}

	public List<ArticleSpaceFile> queryArticleSpaceFiles(boolean queryPrivate) {
		if (memoryMode) {
			Map<Space, Integer> countMap = new HashMap<Space, Integer>();
			for (Article article : store.values()) {
				if (!article.isPublished() || article.isPrivate() && !queryPrivate) {
					continue;
				}
				Space key = article.getSpace();
				Integer count = countMap.get(key);
				if (count == null) {
					count = 0;
				}
				count++;
				countMap.put(key, count);
			}
			List<ArticleSpaceFile> files = new ArrayList<>(countMap.size());
			for (Map.Entry<Space, Integer> it : countMap.entrySet()) {
				ArticleSpaceFile file = new ArticleSpaceFile();
				file.setSpace(it.getKey());
				file.setCount(it.getValue());
				files.add(file);
			}
			Collections.sort(files, articleSpaceFileComparator);
			return files;
		} else {
			return articleDao.selectSpaceFiles(queryPrivate);
		}
	}

	public List<Article> queryScheduled(Timestamp date) {
		if (memoryMode) {
			List<Article> articles = new ArrayList<>();
			for (Article article : store.values()) {
				if (article.isSchedule() && article.getPubDate().compareTo(date) <= 0) {
					articles.add(article);
				}
			}
			return articles;
		} else {
			return articleDao.selectScheduled(date);
		}
	}

	public void remove(Integer id) {
		store.remove(id);
	}

	public void addOrUpdate(Article article) {
		while (true) {
			Article inStore = store.get(article.getId());
			if (inStore == null) {
				inStore = store.putIfAbsent(article.getId(), article);
				if (inStore == null) {
					return;
				}
			}
			if (store.replace(article.getId(), inStore, article)) {
				return;
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (memoryMode) {
			List<Article> articles = articleDao.selectAll();
			for (Article article : articles) {
				store.put(article.getId(), article);
			}
		}
	}

	private boolean luceneQuery(ArticleQueryParam param) {
		return !Validators.isEmptyOrNull(param.getQuery(), true);
	}

	private static abstract class ArticleCommparator implements Comparator<Article> {

		@Override
		public int compare(Article o1, Article o2) {
			int compare = 0;
			if (o1.getLevel() != null) {
				if (o2.getLevel() != null) {
					compare = -o1.getLevel().compareTo(o2.getLevel());
				} else {
					compare = -1;
				}
			}
			if (o2.getLevel() != null) {
				if (o1.getLevel() != null) {
					compare = o2.getLevel().compareTo(o1.getLevel());
				} else {
					compare = 1;
				}
			}
			if (compare == 0) {
				compare = _compare(o1, o2);
				if (compare == 0) {
					if (o1.getPubDate() != null) {
						if (o2.getPubDate() != null) {
							compare = -o1.getPubDate().compareTo(o2.getPubDate());
						} else {
							compare = -1;
						}
					}
					if (o2.getPubDate() != null) {
						if (o1.getPubDate() != null) {
							compare = o2.getPubDate().compareTo(o1.getPubDate());
						} else {
							compare = 1;
						}
					}
					if (compare == 0)
						compare = -o1.getId().compareTo(o2.getId());
				}
			}
			return compare;
		}

		abstract int _compare(Article o1, Article o2);
	}

	private static final class ArticleSpaceFileComparator implements Comparator<ArticleSpaceFile> {

		@Override
		public int compare(ArticleSpaceFile o1, ArticleSpaceFile o2) {
			return -(o1.getSpace().getId().compareTo(o2.getSpace().getId()));
		}
	}

	private static final class ArticleDateFileComparator implements Comparator<ArticleDateFile> {

		@Override
		public int compare(ArticleDateFile o1, ArticleDateFile o2) {
			return o1.getBegin().compareTo(o2.getBegin());
		}

	}

	private final class YMKey {
		private int y;
		private int m;

		public YMKey(Calendar cal) {
			this.y = cal.get(Calendar.YEAR);
			this.m = cal.get(Calendar.MONTH);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + m;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			YMKey other = (YMKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (m != other.m)
				return false;
			if (y != other.y)
				return false;
			return true;
		}

		private ArticleQuery getOuterType() {
			return ArticleQuery.this;
		}
	}

	private Object genDateFilesKey(ArticleDateFileMode mode, Calendar cal) {
		switch (mode) {
		case YM:
			return new YMKey(cal);
		case Y:
			return cal.get(Calendar.YEAR);
		}
		throw new SystemException("无法识别的模式:" + mode);
	}

	public void setMemoryMode(boolean memoryMode) {
		this.memoryMode = memoryMode;
	}
}
