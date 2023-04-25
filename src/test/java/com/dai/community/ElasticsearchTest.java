package com.dai.community;

import com.dai.community.dao.DiscussPostMapper;
import com.dai.community.dao.elasticsearch.DiscussPostRepository;
import com.dai.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * Description:
 *
 * @author DaiJF
 * @date 2022/8/11 - 16:26
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTest {

  @Autowired
  DiscussPostMapper discussPostMapper;
  @Autowired
  DiscussPostRepository discussPostRepository;
  /** 7版本使用ElasticsearchTemplate会启动报错，使用ElasticsearchRestTemplate */
  @Resource
  ElasticsearchRestTemplate elasticsearchRestTemplate;


  @Test
  public void testInsert() {
    discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
    discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
    discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
  }



  @Test
  public void testUpdate() {
    DiscussPost post = discussPostMapper.selectDiscussPostById(231);
    post.setContent("我是新人,使劲灌水.");
    discussPostRepository.save(post);
  }

  @Test
  public void testDelete() {
    // discussRepository.deleteById(231);
    discussPostRepository.deleteAll();
  }


  @Test
  public void testSearchByRepository() {
    NativeSearchQuery search = new NativeSearchQueryBuilder()
        //构建搜索条件:关键字既从title中搜，又从content中搜
        .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
        //排序优先级别type--score--createTime
        .withSorts(
            SortBuilders.fieldSort("type").order(SortOrder.DESC),
            SortBuilders.fieldSort("score").order(SortOrder.DESC),
            SortBuilders.fieldSort("createTime").order(SortOrder.DESC)
        )
        //分页
        .withPageable(PageRequest.of(0, 10))
        //高亮显示字段
        .withHighlightFields(
            new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
            new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
        ).build();



    SearchHits<DiscussPost> hits = elasticsearchRestTemplate.search(search, DiscussPost.class);
    if (hits.getTotalHits() <= 0) {
      return;
    }

    for (SearchHit<DiscussPost> post :hits){
      System.out.println(post.getContent());
    }

  }


}
