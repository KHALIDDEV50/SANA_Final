
package com.example.sana.Enums;

/**
 * Defines relationship types between Business domain nodes.
 */
public enum BusinessEdgeType {

    // Business Model Canvas
    SERVES,
    DELIVERS,
    REACHES,
    BUILDS_RELATIONSHIP_WITH,
    GENERATES_REVENUE,
    REQUIRES,
    PERFORMED_BY,
    PARTNERS_WITH,
    INCURS_COST,

    // Target Audience
    BELONGS_TO_AUDIENCE,
    HAS_NEED,
    HAS_PREFERENCE,

    // SWOT Analysis
    SUPPORTS,
    LIMITS,
    ENABLES,
    THREATENS,

    // Competitor Analysis
    COMPETES_WITH,
    OFFERS,
    DIFFERENTIATES_FROM,

    // Risk Analysis
    CAUSED_BY,
    AFFECTS,
    MITIGATES
}
