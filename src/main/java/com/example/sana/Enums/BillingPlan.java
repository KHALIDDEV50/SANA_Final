package com.example.sana.Enums;

public enum BillingPlan {
    SPARK(0, 1), GLOW(2900, 10), HORIZON(7900, null);
    private final int amountHalalas;
    private final Integer maxProjects;
    BillingPlan(int amountHalalas, Integer maxProjects) {
        this.amountHalalas = amountHalalas; this.maxProjects = maxProjects;
    }
    public int amountHalalas() { return amountHalalas; }
    public Integer maxProjects() { return maxProjects; }
}
