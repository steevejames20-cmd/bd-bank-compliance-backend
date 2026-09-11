package com.bridge.bdbank.api.dto;

import com.bridge.bdbank.persistence.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour un groupe d'alertes rattachées à une même règle.
 * activeCount reflète le nombre d'anomalies distinctes actuellement en
 * anomalie pour cette règle : une même anomalie détectée à plusieurs
 * cycles consécutifs n'est comptée qu'une seule fois.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertGroupResponse {

    private Long ruleId;
    private String ruleName;
    private RuleSeverity ruleSeverity;
    private long activeCount;
    private long totalCount;
    private List<AlertResponse> alerts;
}
