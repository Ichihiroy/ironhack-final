package com.ironhack.backend.overcast.repo;

import com.ironhack.backend.overcast.domain.Category;
import com.ironhack.backend.overcast.domain.Finding;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class FindingRepository {

    private final JdbcClient jdbc;

    public FindingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertAll(List<Finding> findings) {
        for (Finding f : findings) {
            jdbc.sql("""
                    INSERT INTO finding (id, scan_id, resource_id, resource_type, resource_group,
                                         region, rule_id, category, monthly_cost, monthly_saving,
                                         remediation, explanation_cache)
                    VALUES (:id, :scanId, :resourceId, :resourceType, :resourceGroup,
                            :region, :ruleId, :category, :cost, :saving, :remediation, :explanation)
                    """)
                    .param("id", f.id())
                    .param("scanId", f.scanId())
                    .param("resourceId", f.resourceId())
                    .param("resourceType", f.resourceType())
                    .param("resourceGroup", f.resourceGroup())
                    .param("region", f.region())
                    .param("ruleId", f.ruleId())
                    .param("category", f.category().json())
                    .param("cost", f.monthlyCost())
                    .param("saving", f.monthlySaving())
                    .param("remediation", f.remediation())
                    .param("explanation", f.explanationCache())
                    .update();
        }
    }

    /** All findings of a scan, biggest saving first — the natural demo order. */
    public List<Finding> findByScan(String scanId) {
        return jdbc.sql("""
                SELECT * FROM finding WHERE scan_id = :scanId
                ORDER BY monthly_saving DESC, monthly_cost DESC, resource_id
                """)
                .param("scanId", scanId)
                .query(this::map)
                .list();
    }

    public Optional<Finding> findById(String id) {
        return jdbc.sql("SELECT * FROM finding WHERE id = :id")
                .param("id", id)
                .query(this::map)
                .optional();
    }

    public void updateExplanation(String id, String explanation) {
        jdbc.sql("UPDATE finding SET explanation_cache = :explanation WHERE id = :id")
                .param("explanation", explanation)
                .param("id", id)
                .update();
    }

    private Finding map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Finding(
                rs.getString("id"),
                rs.getString("scan_id"),
                rs.getString("resource_id"),
                rs.getString("resource_type"),
                rs.getString("resource_group"),
                rs.getString("region"),
                rs.getString("rule_id"),
                Category.fromDb(rs.getString("category")),
                rs.getBigDecimal("monthly_cost"),
                rs.getBigDecimal("monthly_saving"),
                rs.getString("remediation"),
                rs.getString("explanation_cache"));
    }
}
