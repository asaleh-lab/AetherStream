package com.aetherstream.gateway.api;

import com.aetherstream.application.cqrs.QueryBus;
import com.aetherstream.application.query.GetRecommendationsQuery;
import com.aetherstream.domain.model.Recommendation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationsQueryController {

    private final QueryBus queryBus;

    public RecommendationsQueryController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping
    public List<Recommendation> recommendations(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return queryBus.dispatch(new GetRecommendationsQuery(limit));
    }
}
