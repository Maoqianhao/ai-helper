package com.example.aihelper.rag.metadata;

import org.springframework.stereotype.Component;

import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 元数据文本构建器。
 *
 * 将表结构元数据转换为适合向量检索的文本描述。
 */
@Component
public class MetadataTextBuilder {
    private final MetadataSemanticHintProvider semanticHintProvider;

    public MetadataTextBuilder(MetadataSemanticHintProvider semanticHintProvider) {
        this.semanticHintProvider = semanticHintProvider;
    }

    /**
     * 将表元数据转换为向量化文本
     * 文本格式包含：表名、表描述、字段信息、索引、外键关系
     * 
     * @param metadata 表元数据
     * @return 用于向量化的文本描述
     */
    public String buildVectorText(TableMetadata metadata) {
        StringJoiner joiner = new StringJoiner("\n");
        
        // 1. 表基本信息
        joiner.add(String.format("表名: %s", metadata.getTableName()));
        if (hasText(metadata.getTableComment())) {
            joiner.add(String.format("表描述: %s", metadata.getTableComment()));
        }
        
        // 2. 主键信息
        if (metadata.getPrimaryKey() != null) {
            joiner.add(String.format("主键: %s", metadata.getPrimaryKey()));
        }
        
        // 3. 字段详细信息
        joiner.add("字段信息:");
        for (TableMetadata.ColumnMetadata column : metadata.getColumns()) {
            String columnDesc = buildColumnDescription(column);
            joiner.add("  " + columnDesc);
        }
        
        // 4. 索引信息
        if (!metadata.getIndexes().isEmpty()) {
            joiner.add("索引:");
            for (TableMetadata.IndexMetadata index : metadata.getIndexes()) {
                String indexDesc = String.format("  %s: %s(%s)", 
                        index.getIndexType(),
                        index.getIndexName(),
                        String.join(", ", index.getColumns()));
                joiner.add(indexDesc);
            }
        }
        
        // 5. 外键关系
        if (!metadata.getForeignKeys().isEmpty()) {
            joiner.add("外键关系:");
            for (TableMetadata.ForeignKeyMetadata fk : metadata.getForeignKeys()) {
                String fkDesc = String.format("  %s -> %s(%s)", 
                        fk.getColumn(),
                        fk.getReferencedTable(),
                        fk.getReferencedColumn());
                joiner.add(fkDesc);
            }
        }
        
        return joiner.toString();
    }
    
    /**
     * 构建单个字段的描述文本
     */
    private String buildColumnDescription(TableMetadata.ColumnMetadata column) {
        StringJoiner joiner = new StringJoiner(", ");
        
        // 字段名和类型
        joiner.add(String.format("%s: %s", column.getColumnName(), column.getDataType()));
        
        // 约束信息
        if (column.isPrimaryKey()) {
            joiner.add("主键");
        }
        
        if (!column.isNullable()) {
            joiner.add("非空");
        }
        
        if (column.isAutoIncrement()) {
            joiner.add("自增");
        }
        
        if (column.getDefaultValue() != null) {
            joiner.add(String.format("默认值: %s", column.getDefaultValue()));
        }
        
        // 字段注释
        if (hasText(column.getComment())) {
            joiner.add(column.getComment());
        }
        
        return joiner.toString();
    }
    
    /**
     * 构建增强版向量化文本
     * 包含更多语义信息和业务上下文，提升向量检索准确率
     * 
     * @param metadata 表元数据
     * @return 增强版文本描述
     */
    public String buildEnhancedVectorText(TableMetadata metadata) {
        StringJoiner joiner = new StringJoiner("\n");
        
        // 0. 关键词标签（提升表名/字段名的精确匹配率）
        String keywords = buildKeywordTags(metadata);
        if (hasText(keywords)) {
            joiner.add(keywords);
        }
        
        // 1. 表名称和用途
        joiner.add(String.format("数据库表: %s", metadata.getTableName()));
        
        if (hasText(metadata.getTableComment())) {
            joiner.add(String.format("用途: %s", metadata.getTableComment()));
        }
        
        // 2. 表的业务含义推断（根据表名）
        String businessContext = semanticHintProvider.inferBusinessContext(metadata.getTableName());
        if (hasText(businessContext)) {
            joiner.add(String.format("业务场景: %s", businessContext));
        }
        
        // 3. 核心字段（主键和关键字段）
        if (metadata.getPrimaryKey() != null) {
            joiner.add(String.format("主键字段: %s", metadata.getPrimaryKey()));
        }
        
        // 4. 所有字段的详细语义描述
        joiner.add("包含的字段:");
        for (TableMetadata.ColumnMetadata column : metadata.getColumns()) {
            joiner.add("  - " + buildEnhancedColumnDescription(column));
        }
        
        // 5. 表之间的关系
        if (!metadata.getForeignKeys().isEmpty()) {
            joiner.add("关联表:");
            for (TableMetadata.ForeignKeyMetadata fk : metadata.getForeignKeys()) {
                String relationDesc = String.format("  通过 %s 字段关联到 %s 表的 %s 字段",
                        fk.getColumn(),
                        fk.getReferencedTable(),
                        fk.getReferencedColumn());
                joiner.add(relationDesc);
            }
        }
        
        // 6. 查询示例（帮助理解表的用途）
        String queryExample = semanticHintProvider.generateQueryExample(metadata);
        if (hasText(queryExample)) {
            joiner.add("典型查询: " + queryExample);
        }
        
        return joiner.toString();
    }
    
    /**
     * 构建增强版字段描述
     */
    private String buildEnhancedColumnDescription(TableMetadata.ColumnMetadata column) {
        StringJoiner joiner = new StringJoiner("; ");
        
        // 字段名
        joiner.add(String.format("字段名: %s", column.getColumnName()));
        
        // 数据类型
        joiner.add(String.format("类型: %s", column.getDataType()));
        
        // 业务含义
        if (hasText(column.getComment())) {
            joiner.add(String.format("含义: %s", column.getComment()));
        }
        
        // 约束
        String constraints = buildConstraints(column);
        if (!constraints.isEmpty()) {
            joiner.add(String.format("约束: %s", constraints));
        }
        
        return joiner.toString();
    }
    
    /**
     * 构建字段约束描述
     */
    private String buildConstraints(TableMetadata.ColumnMetadata column) {
        StringJoiner joiner = new StringJoiner(", ");
        
        if (column.isPrimaryKey()) {
            joiner.add("主键");
        }
        
        if (!column.isNullable()) {
            joiner.add("必填");
        }
        
        if (column.isAutoIncrement()) {
            joiner.add("自动增长");
        }
        
        if (column.getDefaultValue() != null) {
            joiner.add(String.format("默认 %s", column.getDefaultValue()));
        }
        
        return joiner.toString();
    }
    
    /**
     * 构建关键词标签（用于提升精确匹配率）
     * 包含：表名、别名、字段名、字段含义的英文/中文关键词
     * 
     * @param metadata 表元数据
     * @return 关键词标签文本
     */
    private String buildKeywordTags(TableMetadata metadata) {
        StringJoiner joiner = new StringJoiner(", ");
        String tableName = metadata.getTableName();
        
        // 1. 表名关键词（包括缩写、英文）
        joiner.add(tableName);
        
        // 2. 表别名（根据表名推断）
        String[] aliases = semanticHintProvider.inferTableAliases(tableName);
        if (aliases != null) {
            for (String alias : aliases) {
                joiner.add(alias);
            }
        }
        
        // 3. 字段名关键词（包括字段名、字段注释、英文含义）
        if (metadata.getColumns() != null) {
            for (TableMetadata.ColumnMetadata column : metadata.getColumns()) {
                // 字段名本身
                joiner.add(column.getColumnName());
                
                // 字段注释（如果有）
                if (hasText(column.getComment())) {
                    joiner.add(column.getComment());
                    
                    // 字段注释的英文翻译（常见字段的英文关键词）
                    String[] englishKeywords = semanticHintProvider.inferFieldEnglishKeywords(column.getColumnName(), column.getComment());
                    if (englishKeywords != null) {
                        for (String keyword : englishKeywords) {
                            joiner.add(keyword);
                        }
                    }
                }
            }
        }
        
        String keywords = joiner.toString();
        return keywords.isEmpty() ? null : "关键词: " + keywords;
    }
    
    /**
     * 构建简化版文本（仅包含关键信息，减少 token 消耗）
     */
    public String buildCompactText(TableMetadata metadata) {
        StringJoiner joiner = new StringJoiner(" | ");
        
        joiner.add("表: " + metadata.getTableName());
        
        if (hasText(metadata.getTableComment())) {
            joiner.add(metadata.getTableComment());
        }
        
        // 只包含字段名和类型
        String columns = metadata.getColumns().stream()
                .map(col -> col.getColumnName() + "(" + col.getDataType() + ")")
                .collect(Collectors.joining(", "));
        joiner.add("字段: " + columns);
        
        // 外键关系
        if (!metadata.getForeignKeys().isEmpty()) {
            String fks = metadata.getForeignKeys().stream()
                    .map(fk -> fk.getColumn() + "->" + fk.getReferencedTable())
                    .collect(Collectors.joining(", "));
            joiner.add("关联: " + fks);
        }
        
        return joiner.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
