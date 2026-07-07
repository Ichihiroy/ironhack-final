package com.ironhack.backend.overcast.rules;

import com.ironhack.backend.overcast.domain.Category;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Typed view of rules-config.yaml — the single auditable home of every
 * constant behind the savings number. Loaded once at startup; the engine
 * cross-checks that YAML specs and Java implementations match 1:1.
 */
public record RulesConfig(
        BigDecimal prevGenDelta,
        BigDecimal offhoursFactor,
        BigDecimal riDiscount,
        BigDecimal premiumDelta,
        int snapshotAgeDays,
        BigDecimal sustainedHours,
        Pattern nonprodRgPattern,
        List<String> requiredTags,
        List<String> prevGenSkus,
        Map<String, RuleSpec> rules) {

    public record RuleSpec(String id, String name, Category category, String predicate,
                           String saving, String remediation) {}

    public static RulesConfig load() {
        try (InputStream in = RulesConfig.class.getResourceAsStream("/rules-config.yaml")) {
            if (in == null) throw new IllegalStateException("rules-config.yaml not on classpath");
            Map<String, Object> root = new Yaml().load(in);

            @SuppressWarnings("unchecked")
            Map<String, Object> c = (Map<String, Object>) root.get("constants");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ruleList = (List<Map<String, Object>>) root.get("rules");

            Map<String, RuleSpec> specs = new LinkedHashMap<>();
            for (Map<String, Object> r : ruleList) {
                RuleSpec spec = new RuleSpec(
                        str(r, "id"), str(r, "name"),
                        Category.fromDb(str(r, "category")),
                        str(r, "predicate"), str(r, "saving"), str(r, "remediation"));
                specs.put(spec.id(), spec);
            }

            @SuppressWarnings("unchecked")
            List<String> requiredTags = (List<String>) c.get("required_tags");
            @SuppressWarnings("unchecked")
            List<String> prevGenSkus = (List<String>) c.get("prev_gen_skus");

            return new RulesConfig(
                    decimal(c, "prev_gen_delta"),
                    decimal(c, "offhours_factor"),
                    decimal(c, "ri_discount"),
                    decimal(c, "premium_delta"),
                    ((Number) c.get("snapshot_age_days")).intValue(),
                    decimal(c, "sustained_hours"),
                    Pattern.compile((String) c.get("nonprod_rg_pattern")),
                    List.copyOf(requiredTags),
                    List.copyOf(prevGenSkus),
                    Map.copyOf(specs));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load rules-config.yaml", e);
        }
    }

    private static BigDecimal decimal(Map<String, Object> map, String key) {
        return new BigDecimal(map.get(key).toString());
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) throw new IllegalStateException("rules-config.yaml: rule missing '" + key + "'");
        return v.toString();
    }
}
