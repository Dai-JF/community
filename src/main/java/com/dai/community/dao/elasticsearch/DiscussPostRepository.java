package com.dai.community.dao.elasticsearch;

import com.dai.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Description: 类似mybatisPlus，在Repository可以做Elasticsearch的相关数据的增删改查
 *
 * @author DaiJF
 * @date 2022/8/11 - 16:22
 */

@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {


}
