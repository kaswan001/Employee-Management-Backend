package net.javaspring.ems.utils;

import java.util.HashMap;
import java.util.Map;

public enum AuditType {

        LEAVING_FORM_LESS_THAN_3_DAYS( 12),

        LEAVING_FORM_LESS_THAN_7_DAYS(13),
        LEAVING_FORM_MORE_THAN_7_DAYS(14),
        BASIC_PROJECT_APPLYING(15),
        MEDIUM_PROJECT_APPLYING(16);

        private int value;
        private String type;
        private int level;

        private static final Map<Integer, AuditType> valueMap = new HashMap<>();

        static {
            for (AuditType auditType : AuditType.values()) {
                    valueMap.put(auditType.value, auditType);
            }
        }

        AuditType(int value) {
            this.value = value;
            level = value % 10;

            switch (value / 10) {
                case 1:
                    type = "Leave Approval";
                    break;
                case 2:
                    type = "Project Approval";
                    break;
                default:
                    type = "Not a valid type";
                    break;
            }
        }

    public int getValue() {
            return value;
    }

    public String getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public static AuditType fromValue(int val) {
        AuditType auditType = valueMap.get(val);
        if (auditType == null) {
            throw new IllegalArgumentException("Invalid value for AuditType: " + val);
        }
        return auditType;
    }
}
