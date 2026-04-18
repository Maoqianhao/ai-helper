package com.example.aihelper.rag.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库表元数据实体
 * 用于存储从数据库中提取的表结构信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadata {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表注释/描述
     */
    private String tableComment;
    
    /**
     * 字段列表
     */
    private List<ColumnMetadata> columns;
    
    /**
     * 索引列表
     */
    @Builder.Default
    private List<IndexMetadata> indexes = new ArrayList<>();
    
    /**
     * 外键关系列表
     */
    @Builder.Default
    private List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
    
    /**
     * 主键字段名
     */
    private String primaryKey;
    
    /**
     * 数据库字段元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnMetadata {
        /**
         * 字段名
         */
        private String columnName;
        
        /**
         * 字段类型（如 BIGINT, VARCHAR(255)）
         */
        private String dataType;
        
        /**
         * 字段注释
         */
        private String comment;
        
        /**
         * 是否为主键
         */
        private boolean primaryKey;
        
        /**
         * 是否允许为空
         */
        private boolean nullable;
        
        /**
         * 默认值
         */
        private String defaultValue;
        
        /**
         * 是否自增
         */
        private boolean autoIncrement;
    }
    
    /**
     * 索引元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexMetadata {
        /**
         * 索引名
         */
        private String indexName;
        
        /**
         * 索引类型（PRIMARY, UNIQUE, INDEX）
         */
        private String indexType;
        
        /**
         * 索引包含的字段
         */
        private List<String> columns;
    }
    
    /**
     * 外键元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeignKeyMetadata {
        /**
         * 外键字段
         */
        private String column;
        
        /**
         * 引用的表名
         */
        private String referencedTable;
        
        /**
         * 引用的字段
         */
        private String referencedColumn;
    }
}
