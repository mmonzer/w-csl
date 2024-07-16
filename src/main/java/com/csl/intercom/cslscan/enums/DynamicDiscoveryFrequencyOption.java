package com.csl.intercom.cslscan.enums;

public enum DynamicDiscoveryFrequencyOption {
    DAILY("everyDay", "DAILY"),
    WEEKLY("everyWeek", "WEEKLY"),
    MONTHLY("everyMonth", "MONTHLY"),
    CUSTOM("custom", "CUSTOM")
    ;

    private String dbapiName;
    private String scanName;

    private DynamicDiscoveryFrequencyOption(String dbapiName, String scanName) {
        this.dbapiName = dbapiName;
        this.scanName = scanName;
    }

    public String dbapiName() {
        return dbapiName;
    }

    public String scanName() {
        return scanName;
    }

    public static DynamicDiscoveryFrequencyOption fromDbapiName(String dbapiName) {
        for (DynamicDiscoveryFrequencyOption option : DynamicDiscoveryFrequencyOption.values()) {
            if (option.dbapiName().equals(dbapiName)) {
                return option;
            }
        }
        return null;
    }

    public static DynamicDiscoveryFrequencyOption fromScanName(String scanName) {
        for (DynamicDiscoveryFrequencyOption option : DynamicDiscoveryFrequencyOption.values()) {
            if (option.scanName().equals(scanName)) {
                return option;
            }
        }
        return null;
    }
}
