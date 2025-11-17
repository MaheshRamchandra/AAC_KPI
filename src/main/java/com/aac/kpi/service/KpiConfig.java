package com.aac.kpi.service;

public class KpiConfig {
    public int robustMinInPerson = 2;
    public int frailMinInPerson = 6;
    public int buddyingMinInPerson = 6;
    public int befriendingMinInPerson = 12;

    public KpiConfig() {}

    public KpiConfig copy() {
        KpiConfig c = new KpiConfig();
        c.robustMinInPerson = this.robustMinInPerson;
        c.frailMinInPerson = this.frailMinInPerson;
        c.buddyingMinInPerson = this.buddyingMinInPerson;
        c.befriendingMinInPerson = this.befriendingMinInPerson;
        return c;
    }
}

