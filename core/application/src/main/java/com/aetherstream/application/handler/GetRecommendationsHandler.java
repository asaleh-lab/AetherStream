package com.aetherstream.application.handler;

import com.aetherstream.application.cqrs.QueryHandler;
import com.aetherstream.application.port.out.RecommendationReadModel;
import com.aetherstream.application.query.GetRecommendationsQuery;
import com.aetherstream.domain.model.Recommendation;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetRecommendationsHandler implements QueryHandler<GetRecommendationsQuery, List<Recommendation>> {

    private final RecommendationReadModel readModel;

    public GetRecommendationsHandler(RecommendationReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<Recommendation> handle(GetRecommendationsQuery query) {
        return readModel.findRecent(query.limit());
    }

    @Override
    public Class<GetRecommendationsQuery> queryType() {
        return GetRecommendationsQuery.class;
    }
}
