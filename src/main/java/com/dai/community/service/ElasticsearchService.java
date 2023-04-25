package com.dai.community.service;

import com.dai.community.consts.CommunityConst;
import com.dai.community.dao.elasticsearch.DiscussPostRepository;
import com.dai.community.entity.DiscussPost;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * @author DaiJF
 * @date 2022/8/11 - 18:39
 */
@Service
public class ElasticsearchService implements CommunityConst {

  @Autowired
  private DiscussPostRepository discussRepository;

  @Resource
  ElasticsearchRestTemplate template;


  public void saveDiscussPost(DiscussPost post) {
    discussRepository.save(post);
  }

  public void deleteDiscussPost(int id) {
    discussRepository.deleteById(id);
  }


  /**
   * Elasticsearch高亮搜索
   * current：当前页
   */
  public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {

    Pageable pageable = PageRequest.of(current, limit);

    NativeSearchQuery search = new NativeSearchQueryBuilder()
        //构建搜索条件
        .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
        //排序优先级别type--score--createTime
        .withSorts(
            SortBuilders.fieldSort("type").order(SortOrder.DESC),
            SortBuilders.fieldSort("score").order(SortOrder.DESC),
            SortBuilders.fieldSort("createTime").order(SortOrder.DESC)
        )
        .withPageable(pageable)
        //设置需要高亮的字段
        .withHighlightFields(
            new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
            new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
        ).build();

    SearchHits<DiscussPost> hits = template.search(search, DiscussPost.class);

    long totalHits = hits.getTotalHits();
    if (totalHits <= 0) {
      return null;
    }

    ArrayList<Object> list = new ArrayList<>();

    // 遍历获取搜索到的数据
    for (SearchHit<DiscussPost> hit : hits) {
      DiscussPost hitContent = hit.getContent();
      // 处理高亮
      Map<String, List<String>> highlightFields = hit.getHighlightFields();

      for (Map.Entry<String, List<String>> field : highlightFields.entrySet()) {

        String key = field.getKey();

        if (StringUtils.equals(key, "title")) {
          List<String> fragments = field.getValue();
          StringBuilder sb = new StringBuilder();
          for (String fragment : fragments) {
            sb.append(fragment);
          }
          hitContent.setTitle(sb.toString());
        }

        if (StringUtils.equals(key, "content")) {
          List<String> fragments = field.getValue();
          StringBuilder sb = new StringBuilder();
          for (String fragment : fragments) {
            sb.append(fragment);
          }
          hitContent.setContent(sb.toString());
        }
      }
      list.add(hitContent);
    }
    return new PageImpl(list, pageable, totalHits);
  }
}