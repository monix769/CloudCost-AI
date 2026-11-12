package com.cloudcostai.optimization.engine;

import com.cloudcostai.entity.OptimizationRecommendation;
import com.cloudcostai.repository.OptimizationRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationExportService {

    private final OptimizationRecommendationRepository recommendationRepository;

    public String exportToCsv(Long organizationId) {
        List<OptimizationRecommendation> recs = recommendationRepository.findByOrganizationIdOrderByEstimatedMonthlySavingsDesc(organizationId);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // CSV Header
        pw.println("ID,Cloud Provider,Cloud Account,Resource ID,Resource Name,Resource Type,Recommendation Type,Current Config,Suggested Config,Priority,Status,Confidence,Reason,Current Monthly Cost ($),Recommended Monthly Cost ($),Estimated Monthly Savings ($),Estimated Annual Savings ($),Realized Monthly Savings ($),Realized Annual Savings ($),Currency,Detected At,Implementation Notes");

        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;

        for (OptimizationRecommendation r : recs) {
            String accountName = r.getCloudAccount() != null ? r.getCloudAccount().getAccountName() : "N/A";
            pw.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    r.getId(),
                    csvEscape(r.getCloudProvider() != null ? r.getCloudProvider().name() : ""),
                    csvEscape(accountName),
                    csvEscape(r.getResourceId()),
                    csvEscape(r.getResourceName() != null ? r.getResourceName() : ""),
                    csvEscape(r.getResourceType()),
                    csvEscape(r.getRecommendationType() != null ? r.getRecommendationType().name() : ""),
                    csvEscape(r.getCurrentConfig()),
                    csvEscape(r.getSuggestedConfig()),
                    csvEscape(r.getPriority() != null ? r.getPriority().name() : ""),
                    csvEscape(r.getStatus() != null ? r.getStatus().name() : ""),
                    csvEscape(r.getConfidenceLevel() != null ? r.getConfidenceLevel().name() : ""),
                    csvEscape(r.getReason()),
                    r.getCurrentMonthlyCost() != null ? r.getCurrentMonthlyCost().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00",
                    r.getRecommendedMonthlyCost() != null ? r.getRecommendedMonthlyCost().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00",
                    r.getEstimatedMonthlySavings() != null ? r.getEstimatedMonthlySavings().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00",
                    r.getEstimatedAnnualSavings() != null ? r.getEstimatedAnnualSavings().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00",
                    r.getRealizedMonthlySavings() != null ? r.getRealizedMonthlySavings().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00",
                    r.getRealizedAnnualSavings() != null ? r.getRealizedAnnualSavings().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00",
                    csvEscape(r.getCurrency() != null ? r.getCurrency() : "USD"),
                    csvEscape(r.getDetectedAt() != null ? dtf.format(r.getDetectedAt()) : ""),
                    csvEscape(r.getImplementationNotes() != null ? r.getImplementationNotes() : "")
            );
        }

        pw.flush();
        return sw.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "\"\"";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}

