package com.example.aihelper.rag.metadata;

/**
 * 数据库元数据规则工具
 *
 * 职责：集中维护元数据提取中的判定规则，避免提取器中出现分散的规则代码。
 */
final class DatabaseMetadataRules {

    private DatabaseMetadataRules() {
    }

    static boolean isSystemTable(String tableName) {
        return tableName.startsWith("sys_")
                || tableName.equalsIgnoreCase("dual")
                || tableName.startsWith("__");
    }

    static String determineIndexType(boolean nonUnique, String indexName) {
        if (indexName.equalsIgnoreCase("PRIMARY")) {
            return "PRIMARY KEY";
        }
        if (!nonUnique) {
            return "UNIQUE";
        }
        return "INDEX";
    }

    static String normalizeDataType(String dataType, int columnSize) {
        if (columnSize > 0 && (dataType.contains("VARCHAR") || dataType.contains("CHAR"))) {
            return dataType + "(" + columnSize + ")";
        }
        return dataType;
    }
}
