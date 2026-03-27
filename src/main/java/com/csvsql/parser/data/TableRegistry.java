package com.csvsql.parser.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry for managing loaded CSV tables with lazy loading support.
 *
 * <p>TableRegistry is the central repository for all tables available to the
 * SQL query engine. It provides:</p>
 * <ul>
 *   <li>Table registration with optional aliases</li>
 *   <li>Lazy loading support for deferred table loading</li>
 *   <li>Table lookup by name or alias</li>
 *   <li>Table lifecycle management</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * TableRegistry registry = new TableRegistry();
 *
 * // Load and register a table
 * CsvTable table = registry.loadAndRegister("employees.csv", "emp");
 *
 * // Retrieve by alias
 * CsvTable emp = registry.getTable("emp");
 *
 * // Schedule lazy loading
 * registry.scheduleLazyLoad("large_data", "large_data.csv", null);
 * </pre>
 *
 * @see CsvTable
 * @see CsvLoader
 * @see com.csvsql.parser.engine.QueryExecutor
 */
public class TableRegistry {

    private static final Logger logger = LoggerFactory.getLogger(TableRegistry.class);

    private final Map<String, CsvTable> tables;
    private final Map<String, String> aliases; // alias -> table name
    private final Map<String, String> pendingLoads; // table name -> file path (for lazy loading)
    private final CsvLoader loader;
    private boolean lazyLoadingEnabled = true;

    /**
     * Creates a new TableRegistry with empty tables and lazy loading enabled.
     */
    public TableRegistry() {
        this.tables = new LinkedHashMap<>();
        this.aliases = new HashMap<>();
        this.pendingLoads = new HashMap<>();
        this.loader = new CsvLoader();
    }

    /**
     * Enables or disables lazy loading.
     *
     * <p>When enabled, tables scheduled via {@link #scheduleLazyLoad} will be
     * loaded on-demand when first accessed.</p>
     *
     * @param enabled true to enable lazy loading
     */
    public void setLazyLoadingEnabled(boolean enabled) {
        this.lazyLoadingEnabled = enabled;
    }

    /**
     * Checks if lazy loading is enabled.
     *
     * @return true if lazy loading is enabled
     */
    public boolean isLazyLoadingEnabled() {
        return lazyLoadingEnabled;
    }

    /**
     * Register a table with an optional alias.
     *
     * @param table the table to register
     * @param alias optional alias for the table
     */
    public void register(CsvTable table, String alias) {
        String name = table.getName();
        tables.put(name, table);

        if (alias != null && !alias.isEmpty()) {
            aliases.put(alias, name);
            logger.debug("Registered alias '{}' for table '{}'", alias, name);
        }

        logger.info("Registered table: {} ({} rows, {} columns)",
            name, table.getRowCount(), table.getColumnCount());
    }

    /**
     * Schedule a table for lazy loading.
     *
     * @param tableName the name for the table
     * @param filePath  the path to the CSV file
     * @param alias     optional alias for the table
     */
    public void scheduleLazyLoad(String tableName, String filePath, String alias) {
        pendingLoads.put(tableName, filePath);
        if (alias != null && !alias.isEmpty()) {
            aliases.put(alias, tableName);
            logger.debug("Scheduled lazy load for table '{}' with alias '{}'", tableName, alias);
        }
    }

    /**
     * Load and register a CSV file.
     *
     * @param filePath the path to the CSV file
     * @return the loaded table
     * @throws Exception if loading fails
     */
    public CsvTable loadAndRegister(String filePath) throws Exception {
        return loadAndRegister(filePath, null);
    }

    /**
     * Load and register a CSV file with an optional alias.
     *
     * @param filePath the path to the CSV file
     * @param alias    optional alias for the table
     * @return the loaded table
     * @throws Exception if loading fails
     */
    public CsvTable loadAndRegister(String filePath, String alias) throws Exception {
        CsvTable table = loader.load(filePath, alias);
        register(table, alias);
        return table;
    }

    /**
     * Get a table by name or alias. Triggers lazy loading if needed.
     *
     * @param nameOrAlias the table name or alias
     * @return the table, or null if not found
     */
    public CsvTable getTable(String nameOrAlias) {
        // Resolve alias first
        String tableName = aliases.get(nameOrAlias);
        if (tableName == null) {
            tableName = nameOrAlias;
        }

        // Check if already loaded
        if (tables.containsKey(tableName)) {
            return tables.get(tableName);
        }

        // Check if pending lazy load
        if (pendingLoads.containsKey(tableName)) {
            String filePath = pendingLoads.remove(tableName);
            try {
                logger.info("Lazy loading table '{}' from '{}'", tableName, filePath);
                CsvTable table = loader.load(filePath, tableName);
                tables.put(tableName, table);
                return table;
            } catch (Exception e) {
                logger.error("Failed to lazy load table '{}': {}", tableName, e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * Check if a table exists (or is scheduled for lazy loading).
     *
     * @param nameOrAlias the table name or alias
     * @return true if the table exists or is pending
     */
    public boolean hasTable(String nameOrAlias) {
        // Check alias
        String tableName = aliases.get(nameOrAlias);
        if (tableName == null) {
            tableName = nameOrAlias;
        }
        return tables.containsKey(tableName) || pendingLoads.containsKey(tableName);
    }

    /**
     * Get all registered table names (excludes pending lazy loads).
     *
     * @return a set of table names
     */
    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(tables.keySet());
    }

    /**
     * Get all aliases.
     *
     * @return a map of alias to table name
     */
    public Map<String, String> getAliases() {
        return Collections.unmodifiableMap(aliases);
    }

    /**
     * Unregister a table.
     *
     * @param nameOrAlias the table name or alias
     * @return true if the table was removed
     */
    public boolean unregister(String nameOrAlias) {
        // Check if it's an alias
        final String tableName;
        if (aliases.containsKey(nameOrAlias)) {
            tableName = aliases.remove(nameOrAlias);
        } else {
            tableName = nameOrAlias;
        }

        // Remove any aliases pointing to this table
        aliases.values().removeIf(name -> name.equals(tableName));

        // Remove from pending loads
        pendingLoads.remove(tableName);

        CsvTable removed = tables.remove(tableName);
        if (removed != null) {
            logger.info("Unregistered table: {}", tableName);
            return true;
        }

        return pendingLoads.remove(tableName) != null;
    }

    /**
     * Clear all registered tables and pending loads.
     */
    public void clear() {
        tables.clear();
        aliases.clear();
        pendingLoads.clear();
        logger.info("Cleared all registered tables");
    }

    /**
     * Get the number of registered tables (excludes pending lazy loads).
     */
    public int size() {
        return tables.size();
    }

    /**
     * Get the number of tables pending lazy load.
     */
    public int pendingCount() {
        return pendingLoads.size();
    }
}