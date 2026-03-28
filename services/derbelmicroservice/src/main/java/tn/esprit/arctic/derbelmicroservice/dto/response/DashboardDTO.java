package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {
    private long totalPatients;
    private long activeRecords;
    private long activePrescriptions;
    private long severityLow;
    private long severityMedium;
    private long severityHigh;
}
