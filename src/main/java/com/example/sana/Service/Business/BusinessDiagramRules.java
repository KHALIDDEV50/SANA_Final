package com.example.sana.Service.Business;

import com.example.sana.Enums.BusinessEdgeType;
import com.example.sana.Enums.BusinessNodeType;
import com.example.sana.Enums.BusinessOutputType;
import com.example.sana.Service.DiagramServices.BasicDiagramRules;
import com.example.sana.Service.DiagramServices.DiagramRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

// One diagram type per Business tool. Each uses the node and edge types of its tool.
@Configuration
public class BusinessDiagramRules {

    private static final String DOMAIN = "BUSINESS";
    private static final String COMMON_RULES = " Node properties.label is required; properties.description is optional. "
            + "Do not invent market statistics or competitor facts; mark assumptions in properties.description. New IDs must be null.";

    @Bean
    public DiagramRules businessModelCanvasRules() {
        return new BasicDiagramRules(DOMAIN, BusinessOutputType.BUSINESS_MODEL_CANVAS.name(),
                nodeTypes(BusinessNodeType.CUSTOMER_SEGMENT, BusinessNodeType.VALUE_PROPOSITION, BusinessNodeType.CHANNELS,
                        BusinessNodeType.CUSTOMER_RELATIONSHIPS, BusinessNodeType.REVENUE_STREAMS, BusinessNodeType.KEY_RESOURCES,
                        BusinessNodeType.KEY_ACTIVITIES, BusinessNodeType.KEY_PARTNERSHIPS, BusinessNodeType.COST_STRUCTURE),
                edgeTypes(BusinessEdgeType.SERVES, BusinessEdgeType.DELIVERS, BusinessEdgeType.REACHES,
                        BusinessEdgeType.BUILDS_RELATIONSHIP_WITH, BusinessEdgeType.GENERATES_REVENUE, BusinessEdgeType.REQUIRES,
                        BusinessEdgeType.PERFORMED_BY, BusinessEdgeType.PARTNERS_WITH, BusinessEdgeType.INCURS_COST),
                "Business Model Canvas. Each node is one entry in one of the nine canvas blocks, for example one customer segment. "
                        + "Connect entries with the edge types, for example VALUE_PROPOSITION SERVES CUSTOMER_SEGMENT "
                        + "or CHANNELS REACHES CUSTOMER_SEGMENT." + COMMON_RULES);
    }

    @Bean
    public DiagramRules targetAudienceRules() {
        return new BasicDiagramRules(DOMAIN, BusinessOutputType.TARGET_AUDIENCE.name(),
                nodeTypes(BusinessNodeType.TARGET_AUDIENCE, BusinessNodeType.CUSTOMER_PERSONA),
                edgeTypes(BusinessEdgeType.BELONGS_TO_AUDIENCE, BusinessEdgeType.HAS_NEED, BusinessEdgeType.HAS_PREFERENCE),
                "Target Audience. TARGET_AUDIENCE nodes are audience groups and CUSTOMER_PERSONA nodes are representative people. "
                        + "Connect a persona to its group with BELONGS_TO_AUDIENCE. Describe needs and preferences in properties.description "
                        + "and use HAS_NEED or HAS_PREFERENCE edges between related nodes." + COMMON_RULES);
    }

    @Bean
    public DiagramRules swotRules() {
        return new BasicDiagramRules(DOMAIN, BusinessOutputType.SWOT.name(),
                nodeTypes(BusinessNodeType.SWOT_STRENGTH, BusinessNodeType.SWOT_WEAKNESS,
                        BusinessNodeType.SWOT_OPPORTUNITY, BusinessNodeType.SWOT_THREAT),
                edgeTypes(BusinessEdgeType.SUPPORTS, BusinessEdgeType.LIMITS, BusinessEdgeType.ENABLES, BusinessEdgeType.THREATENS),
                "SWOT analysis. Each node is one strength, weakness, opportunity or threat. "
                        + "Edges show how factors affect each other, for example a SWOT_STRENGTH ENABLES a SWOT_OPPORTUNITY "
                        + "or a SWOT_THREAT THREATENS a SWOT_STRENGTH." + COMMON_RULES);
    }

    @Bean
    public DiagramRules competitorAnalysisRules() {
        return new BasicDiagramRules(DOMAIN, BusinessOutputType.COMPETITOR_ANALYSIS.name(),
                nodeTypes(BusinessNodeType.COMPETITOR, BusinessNodeType.COMPETITOR_PRODUCT, BusinessNodeType.COMPETITOR_FEATURE),
                edgeTypes(BusinessEdgeType.COMPETES_WITH, BusinessEdgeType.OFFERS, BusinessEdgeType.DIFFERENTIATES_FROM),
                "Competitor analysis based on information supplied by the user. COMPETITOR OFFERS COMPETITOR_PRODUCT, "
                        + "COMPETITOR_PRODUCT OFFERS COMPETITOR_FEATURE, and COMPETES_WITH or DIFFERENTIATES_FROM compare competitors or products."
                        + COMMON_RULES);
    }

    @Bean
    public DiagramRules riskAnalysisRules() {
        return new BasicDiagramRules(DOMAIN, BusinessOutputType.RISK_ANALYSIS.name(),
                nodeTypes(BusinessNodeType.RISK, BusinessNodeType.RISK_CAUSE, BusinessNodeType.RISK_MITIGATION),
                edgeTypes(BusinessEdgeType.CAUSED_BY, BusinessEdgeType.AFFECTS, BusinessEdgeType.MITIGATES),
                "Risk analysis. RISK CAUSED_BY RISK_CAUSE, RISK_MITIGATION MITIGATES RISK, and AFFECTS links a risk to what it impacts. "
                        + "Put likelihood and impact in properties when known." + COMMON_RULES);
    }

    private static List<String> nodeTypes(BusinessNodeType... types) {
        return Arrays.stream(types).map(Enum::name).toList();
    }

    private static List<String> edgeTypes(BusinessEdgeType... types) {
        return Arrays.stream(types).map(Enum::name).toList();
    }
}
