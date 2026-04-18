package com.example.aihelper.rag.metadata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库元数据提取器。
 */
@Slf4j
@Component
public class DatabaseMetadataExtractor {

    private final DataSource dataSource;

    public DatabaseMetadataExtractor(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * 提取数据库中所有表的元数据
     * 
     * @return 所有表的元数据列表
     */
    public List<TableMetadata> extractAllTables() {
        List<TableMetadata> allTables = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 获取所有表
            try (ResultSet tables = metaData.getTables(connection.getCatalog(), null, "%", 
                    new String[]{"TABLE"})) {
                
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String tableComment = tables.getString("REMARKS");
                    
                    // 跳过系统表
                    if (DatabaseMetadataRules.isSystemTable(tableName)) {
                        continue;
                    }
                    
                    log.info("[metadata-extractor] event=table_extract_start table={}", tableName);
                    
                    // 提取表的详细信息
                    TableMetadata tableMetadata = TableMetadata.builder()
                            .tableName(tableName)
                            .tableComment(tableComment)
                            .columns(extractColumns(metaData, tableName))
                            .indexes(extractIndexes(metaData, tableName))
                            .foreignKeys(extractForeignKeys(metaData, tableName))
                            .primaryKey(extractPrimaryKey(metaData, tableName))
                            .build();
                    
                    allTables.add(tableMetadata);
                }
            }
            
            log.info("[metadata-extractor] event=extract_completed tableCount={}", allTables.size());
            
        } catch (SQLException e) {
            log.error("[metadata-extractor] event=extract_error", e);
            throw new RuntimeException("提取数据库元数据失败", e);
        }
        
        return allTables;
    }
    
    /**
     * 提取指定表的字段信息
     */
    private List<TableMetadata.ColumnMetadata> extractColumns(DatabaseMetaData metaData, String tableName) {
        List<TableMetadata.ColumnMetadata> columns = new ArrayList<>();
        
        try (ResultSet columnsRs = metaData.getColumns(null, null, tableName, "%")) {
            while (columnsRs.next()) {
                String columnName = columnsRs.getString("COLUMN_NAME");
                String dataType = columnsRs.getString("TYPE_NAME");
                String comment = columnsRs.getString("REMARKS");
                boolean nullable = "YES".equals(columnsRs.getString("IS_NULLABLE"));
                String defaultValue = columnsRs.getString("COLUMN_DEF");
                boolean autoIncrement = "YES".equals(columnsRs.getString("IS_AUTOINCREMENT"));
                
                int columnSize = columnsRs.getInt("COLUMN_SIZE");
                dataType = DatabaseMetadataRules.normalizeDataType(dataType, columnSize);
                
                columns.add(TableMetadata.ColumnMetadata.builder()
                        .columnName(columnName)
                        .dataType(dataType)
                        .comment(comment)
                        .nullable(nullable)
                        .defaultValue(defaultValue)
                        .autoIncrement(autoIncrement)
                        .build());
            }
        } catch (SQLException e) {
            log.error("[metadata-extractor] event=column_extract_error table={}", tableName, e);
        }
        
        return columns;
    }
    
    /**
     * 提取指定表的索引信息
     */
    private List<TableMetadata.IndexMetadata> extractIndexes(DatabaseMetaData metaData, String tableName) {
        List<TableMetadata.IndexMetadata> indexes = new ArrayList<>();
        
        try (ResultSet indexRs = metaData.getIndexInfo(null, null, tableName, false, false)) {
            String currentIndexName = null;
            TableMetadata.IndexMetadata currentIndex = null;
            
            while (indexRs.next()) {
                String indexName = indexRs.getString("INDEX_NAME");
                String columnName = indexRs.getString("COLUMN_NAME");
                boolean nonUnique = indexRs.getBoolean("NON_UNIQUE");
                
                // 跳过没有列名的索引
                if (columnName == null) {
                    continue;
                }
                
                // 新的索引
                if (!indexName.equals(currentIndexName)) {
                    currentIndexName = indexName;
                    currentIndex = TableMetadata.IndexMetadata.builder()
                            .indexName(indexName)
                            .indexType(DatabaseMetadataRules.determineIndexType(nonUnique, indexName))
                            .columns(new ArrayList<>())
                            .build();
                    indexes.add(currentIndex);
                }
                
                if (currentIndex != null) {
                    currentIndex.getColumns().add(columnName);
                }
            }
        } catch (SQLException e) {
            log.error("[metadata-extractor] event=index_extract_error table={}", tableName, e);
        }
        
        return indexes;
    }
    
    /**
     * 提取指定表的外键信息
     */
    private List<TableMetadata.ForeignKeyMetadata> extractForeignKeys(DatabaseMetaData metaData, String tableName) {
        List<TableMetadata.ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        try (ResultSet fkRs = metaData.getImportedKeys(null, null, tableName)) {
            while (fkRs.next()) {
                String fkColumn = fkRs.getString("FKCOLUMN_NAME");
                String pkTable = fkRs.getString("PKTABLE_NAME");
                String pkColumn = fkRs.getString("PKCOLUMN_NAME");
                
                foreignKeys.add(TableMetadata.ForeignKeyMetadata.builder()
                        .column(fkColumn)
                        .referencedTable(pkTable)
                        .referencedColumn(pkColumn)
                        .build());
            }
        } catch (SQLException e) {
            log.error("[metadata-extractor] event=foreign_key_extract_error table={}", tableName, e);
        }
        
        return foreignKeys;
    }
    
    /**
     * 提取指定表的主键字段
     */
    private String extractPrimaryKey(DatabaseMetaData metaData, String tableName) {
        try (ResultSet pkRs = metaData.getPrimaryKeys(null, null, tableName)) {
            if (pkRs.next()) {
                return pkRs.getString("COLUMN_NAME");
            }
        } catch (SQLException e) {
            log.error("[metadata-extractor] event=primary_key_extract_error table={}", tableName, e);
        }
        
        return null;
    }
    
}
