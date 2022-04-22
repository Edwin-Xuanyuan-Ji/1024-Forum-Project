package com.forum.community.service;

import com.forum.community.dao.elasticsearch.DiscussPostRepository;
import com.forum.community.entity.DiscussPost;
import com.forum.community.entity.Page;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private DiscussService discussService;

    public void saveDiscussPost(DiscussPost post){
        discussPostRepository.save(post);
    }

    public List searchDiscussPost(String keyword, int currentPage, int limit){
        Query query = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword,"title","content"))
                .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC),
                        SortBuilders.fieldSort("score").order(SortOrder.DESC),
                        SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(currentPage,limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                )
                .build();

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

        return list;
    }

    public void deleteDiscussPost(int entityId){
        discussPostRepository.delete(discussService.findDiscussPostById(entityId));
    }

}
