package com.get.markdown.doc.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.get.markdown.core.MarkdownAnalyser;
import com.get.markdown.doc.dao.TopicContentDao;
import com.get.markdown.doc.dao.TopicDao;
import com.get.markdown.doc.dao.UserDao;
import com.get.markdown.doc.entity.enumeration.ResultCodeEnum;
import com.get.markdown.doc.entity.enumeration.TopicContentStatusEnum;
import com.get.markdown.doc.entity.enumeration.TopicStatusEnum;
import com.get.markdown.doc.entity.po.Topic;
import com.get.markdown.doc.entity.po.TopicContent;
import com.get.markdown.doc.entity.po.User;
import com.get.markdown.doc.entity.vo.JsonResponse;
import com.get.markdown.doc.entity.vo.Page;
import com.get.markdown.doc.utils.Constants;

@Service
public class TopicService {
	
	private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm";
	private SimpleDateFormat dateformat = new SimpleDateFormat(DEFAULT_FORMAT);
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private TopicDao topicDao;
	@Autowired
	private TopicContentDao topicContentDao;
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private com.get.markdown.doc.sqlitedao.TopicContentDao sqlitetopicContentDao;
	@Autowired
	private com.get.markdown.doc.sqlitedao.TopicDao sqlitetopicDao;
	@Autowired
	private com.get.markdown.doc.sqlitedao.UserDao sqliteuserDao;
	
	public JsonResponse getTopicList(Integer pageNum, Integer pageSize) {
		JsonResponse jr = new JsonResponse();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("status !=", TopicStatusEnum.DELETED.getCode());
		List<Topic> list = sqlitetopicDao.findPage(pageNum, pageSize, params, "uri asc");
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		params = new HashMap<String, Object>();
		params.put("status", TopicContentStatusEnum.DEFAULT.getCode());
		for (Topic topic : list) {
			Map<String, Object> oneMap = new HashMap<String, Object>();
			result.add(oneMap);
			oneMap.put("id", topic.getId());
			oneMap.put("name", topic.getName());
			oneMap.put("updateTime", dateformat.format(topic.getUpdateTime()));
			oneMap.put("operator", topic.getOperatorId());
			oneMap.put("status", topic.getStatus());
			oneMap.put("uri", topic.getUri());
			
			params.put("topic_id", topic.getId());
			List<TopicContent> topicContentList = sqlitetopicContentDao.find(params, "create_time desc");
			if (topicContentList.isEmpty()) {
				oneMap.put("remark", "");
				continue;
			}
			TopicContent topicContent = topicContentList.get(0);
			if (topicContent.getRemark() != null) {
				oneMap.put("remark", topicContent.getRemark());
			} else {
				oneMap.put("remark", "");
			}
		}
		jr.setData(result);
		Page page = new Page(pageNum, pageSize);
		params = new HashMap<String, Object>();
		params.put("status !=", TopicStatusEnum.DELETED.getCode());
		Integer count = sqlitetopicDao.count(params);
		page.setTotal(count);
		jr.setPage(page);
		return jr;
	}

	/**
	 * 首页
	 * @return
	 */
	public String topicIndex() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("URI", Constants.MARKDOWN_INDEX_URI);
		List<Topic> topicList = sqlitetopicDao.find(params, "CREATE_TIME asc");
		if (topicList.isEmpty()) {
			return Constants.MARKDOWN_INDEX_EMPTY;
		}
		Topic topic = topicList.get(0);
		params.clear();
		params.put("TOPIC_ID", topic.getId());
		params.put("STATUS", TopicContentStatusEnum.DEFAULT.getCode());
		List<TopicContent> topicContentList = sqlitetopicContentDao.find(params, "CREATE_TIME desc");
		if (topicContentList.isEmpty()) {
			return Constants.MARKDOWN_INDEX_EMPTY;
		}
		return topicContentList.get(0).getContentHtml();
	}

	public JsonResponse addTopic(String name, String uri, Integer operatorId) {
		JsonResponse jr = new JsonResponse();
		Map<String, Object> params = new HashMap<String, Object>();
		if (uri.endsWith("/")) {
			uri.substring(0, uri.length()-1);
		}
		params.put("uri", uri);
		params.put("status !=", TopicStatusEnum.DELETED.getCode());
		List<Topic> topicList = sqlitetopicDao.find(params, "CREATE_TIME asc");
		if (!topicList.isEmpty()) {
			jr.setCode(ResultCodeEnum.BIZ_ERROR.getCode());
			jr.setMessage("保存失败，URI已经存在！");
			return jr;
		}
		Topic topic = new Topic();
		topic.setName(name);
		topic.setUri(uri);
		topic.setOperatorId(operatorId);
		topic.setStatus(TopicStatusEnum.DEFAULT.getCode());
		topic.setCreateTime(new Date());
		topic.setUpdateTime(new Date());
		sqlitetopicDao.save(topic);
		// 初始化一个内容页
		TopicContent newTopicContent = new TopicContent();
		newTopicContent.setCreateTime(new Date());
		newTopicContent.setUpdateTime(new Date());
		newTopicContent.setOperatorId(operatorId);
		newTopicContent.setRemark("初始化");
		newTopicContent.setStatus(TopicContentStatusEnum.DEFAULT.getCode());
		newTopicContent.setTopicId(topic.getId());
		
		
		String html =null;
		//处理uri
		String [] array = StringUtils.splitByWholeSeparatorPreserveAllTokens(uri,"/");
		if(array.length >2) {
			String [] uriarry  = new String[array.length-2];
			for(int i=1;i<array.length-1;i++) {
				uriarry[i-1] = array[i];
			}
			String parenturl ="/"+String.join("/", uriarry);
			Map<String, Object> ps = new HashMap<String, Object>();
			ps.put("uri", parenturl);
			ps.put("status !=", TopicStatusEnum.DELETED.getCode());
			List<Topic> topics = sqlitetopicDao.find(ps, "CREATE_TIME asc");
			String md = String.format("* [%s](%s) > %s", topics.get(0).getName(),parenturl,name);
			newTopicContent.setContentMarkdown(md);
			 html = MarkdownAnalyser.analyseMarkdown(md);
		}else {		
			newTopicContent.setContentMarkdown(Constants.MARKDOWN_INIT_CONTENT);
			html = MarkdownAnalyser.analyseMarkdown(Constants.MARKDOWN_INIT_CONTENT);
		}
		newTopicContent.setContentHtml(html);
		sqlitetopicContentDao.save(newTopicContent);
		return jr;
	}

	public Map<String, Object> getTopicContentByUri(String uri) {
		logger.debug("请求页面：{}", uri);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("URI", uri);
		params.put("STATUS !=", TopicStatusEnum.DELETED.getCode());
		List<Topic> topicList = sqlitetopicDao.find(params, "CREATE_TIME asc");
		if (topicList.isEmpty()) {
			logger.debug("URI不存在:{}", uri);
			return null;
		}
		Map<String, Object> result = new HashMap<String, Object>();
		Topic topic = topicList.get(0);
		params.clear();
		params.put("TOPIC_ID", topic.getId());
		params.put("STATUS", TopicContentStatusEnum.DEFAULT.getCode());
		List<TopicContent> topicContentList = sqlitetopicContentDao.find(params, "CREATE_TIME desc");
		if (topicContentList.isEmpty()) {
			result.put("contentHtml", Constants.MARKDOWN_INIT_CONTENT);
			return result;
		}
		TopicContent topicContent = topicContentList.get(0);
		result.put("contentHtml", topicContent.getContentHtml());
		result.put("contentId", topicContent.getId());
		result.put("lastUpdateTime", dateformat.format(topicContent.getUpdateTime()));
		result.put("operatorId", topicContent.getOperatorId());
		result.put("remark", topicContent.getRemark());
		if (topicContent.getOperatorId() != null) {
			User user = sqliteuserDao.findById(topicContent.getOperatorId());
			result.put("operatorName", user.getName());
		}
		return result;
	}
	
	public JsonResponse getTopicContentById(Integer id, String action) {
		JsonResponse jr = new JsonResponse();
		TopicContent topicContent = sqlitetopicContentDao.findById(id);
		if (topicContent == null) {
			jr.setCode(ResultCodeEnum.BIZ_ERROR.getCode());
			jr.setMessage("未知的内容ID");
			return jr;
		}
		jr.setData(topicContent);
		return jr;
	}

	public JsonResponse addTopicContent(String contentMarkdown, String remark, Integer preId, Integer operatorId) {
		JsonResponse jr = new JsonResponse();
		TopicContent topicContent = sqlitetopicContentDao.findById(preId);
		if (topicContent == null) {
			logger.warn("提交失败，文档已经被修改");
			jr.setCode(ResultCodeEnum.BIZ_ERROR.getCode());
			jr.setMessage("提交失败，文档已经被修改");
			return jr;
		}
		if (!TopicContentStatusEnum.DEFAULT.getCode().equals(topicContent.getStatus())) {
			logger.warn("提交失败，文档已经被修改:preId={}", preId);
			jr.setCode(ResultCodeEnum.BIZ_ERROR.getCode());
			jr.setMessage("提交失败，文档已经被修改！");
			return jr;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("status", TopicContentStatusEnum.EXPIRE.getCode());
		params.put("update_time", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
		sqlitetopicContentDao.update(topicContent.getId(), params);
		logger.debug("新增topicContent，topicId={}, contentMarkdown={}", topicContent.getTopicId(), contentMarkdown);
		TopicContent newTopicContent = new TopicContent();
		newTopicContent.setCreateTime(new Date());
		newTopicContent.setUpdateTime(new Date());
		newTopicContent.setOperatorId(operatorId);
		newTopicContent.setRemark(remark);
		newTopicContent.setStatus(TopicContentStatusEnum.DEFAULT.getCode());
		newTopicContent.setTopicId(topicContent.getTopicId());
		newTopicContent.setContentMarkdown(contentMarkdown);
		
//		MarkdownProcessor markdownProcessor = new MarkdownProcessor();
//		String html = markdownProcessor.markdown(contentMarkdown);
//		MarkdownPlusUtils markdownPlus = new MarkdownPlusUtils();
//		html = markdownPlus.markdown(html);
		contentMarkdown = HtmlUtils.htmlEscape(contentMarkdown);
		String html = MarkdownAnalyser.analyseMarkdown(contentMarkdown);
		logger.debug("新增topicContent，topicId={}, contentHtml={}", topicContent.getTopicId(), html);
		newTopicContent.setContentHtml(html);
		sqlitetopicContentDao.save(newTopicContent);
		jr.setData(newTopicContent);
		return jr;
	}
	
	public JsonResponse getTopic(Integer id) {
		JsonResponse jr = new JsonResponse();
		Topic topic = sqlitetopicDao.findById(id);
		if (topic == null) {
			jr.setCode(ResultCodeEnum.BIZ_ERROR.getCode());
			jr.setMessage("未知的内容ID");
			return jr;
		}
		jr.setData(topic);
		return jr;
	}
	
	public JsonResponse editTopic(Integer id, String name, String uri, Integer operatorId) {
		JsonResponse jr = new JsonResponse();
		Topic topic = sqlitetopicDao.findById(id);
		if (topic == null) {
			jr.setCode(ResultCodeEnum.BIZ_ERROR.getCode());
			jr.setMessage("未知的内容ID");
			return jr;
		}
		if (uri.endsWith("/")) {
			uri.substring(0, uri.length()-1);
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);
		params.put("uri", uri);
		params.put("update_time", new Date());
		params.put("operator_id", operatorId);
		sqlitetopicDao.update(id, params);
		return jr;
	}
	
	public JsonResponse deleteTopic(Integer id) {
		JsonResponse jr = new JsonResponse();
		Topic topic = sqlitetopicDao.findById(id);
		if (topic == null) {
			jr.setCode(ResultCodeEnum.BIZ_ERROR.getCode());
			jr.setMessage("未知的内容ID");
			return jr;
		}
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("status", TopicStatusEnum.DELETED.getCode());
		params.put("update_time", new Date());
		sqlitetopicDao.update(id, params);
		return jr;
	}
	
}
