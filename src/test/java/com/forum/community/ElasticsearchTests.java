package com.forum.community;

import com.forum.community.dao.DiscussPostMapper;
import com.forum.community.dao.elasticsearch.DiscussPostRepository;
import com.forum.community.entity.DiscussPost;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void testInsert(){
        discussRepository.save(discussMapper.selectDiscussPostById(1));
        discussRepository.save(discussMapper.selectDiscussPostById(3));
        discussRepository.save(discussMapper.selectDiscussPostById(7));
    }

    @Test
    public void testInsertList(){
        discussRepository.save(discussMapper.selectDiscussPostById(2));
        discussRepository.save(discussMapper.selectDiscussPostById(4));
        discussRepository.save(discussMapper.selectDiscussPostById(5));
        discussRepository.save(discussMapper.selectDiscussPostById(6));
        discussRepository.save(discussMapper.selectDiscussPostById(8));
        discussRepository.save(discussMapper.selectDiscussPostById(9));
    }

    @Test
    public void testSearch() throws IOException {

        Query query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("tittle", "测"))
                .build();

        org.springframework.data.elasticsearch.core.SearchHits<DiscussPost> searchHits = elasticsearchRestTemplate.search(query,DiscussPost.class);

        System.out.println(searchHits.get().toString());

    }

    @Test
    public void testSearch2() throws IOException {

        Query query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("hello","title","content"))
                .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC),
                        SortBuilders.fieldSort("score").order(SortOrder.DESC),
                        SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        SearchHits<DiscussPost> searchHits = elasticsearchRestTemplate.search(query,DiscussPost.class);

        List<DiscussPost> list = new ArrayList<>();
        for(SearchHit hit : searchHits){

            DiscussPost post = (DiscussPost) hit.getContent();

            if(hit.getHighlightField("title").size()>0){
                post.setTitle((String) hit.getHighlightField("title").get(0));
            }

            if(hit.getHighlightField("content").size()>0){
                post.setContent((String) hit.getHighlightField("content").get(0));
            }

            list.add(post);
        }

        System.out.println("-----------------------");
        System.out.println(list.toString());
        System.out.println("-----------------------");
    }

}
