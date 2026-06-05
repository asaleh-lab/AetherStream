package com.aetherstream.application.handler;

import com.aetherstream.application.cqrs.QueryHandler;
import com.aetherstream.application.port.out.AlertReadModel;
import com.aetherstream.application.query.GetAlertsQuery;
import com.aetherstream.domain.model.Alert;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetAlertsHandler implements QueryHandler<GetAlertsQuery, List<Alert>> {

    private final AlertReadModel readModel;

    public GetAlertsHandler(AlertReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<Alert> handle(GetAlertsQuery query) {
        return readModel.findRecent(query.limit());
    }

    @Override
    public Class<GetAlertsQuery> queryType() {
        return GetAlertsQuery.class;
    }
}
