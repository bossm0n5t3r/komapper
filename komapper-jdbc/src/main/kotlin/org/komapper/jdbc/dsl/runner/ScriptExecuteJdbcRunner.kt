package org.komapper.jdbc.dsl.runner

import org.komapper.core.DatabaseConfig
import org.komapper.core.Statement
import org.komapper.core.dsl.options.ScriptOptions
import org.komapper.core.dsl.runner.ScriptExecuteRunner
import org.komapper.jdbc.JdbcDatabaseConfig
import org.komapper.jdbc.JdbcExecutor

internal class ScriptExecuteJdbcRunner(
    sql: String,
    private val options: ScriptOptions
) :
    JdbcRunner<Unit> {

    private val runner = ScriptExecuteRunner(sql, options)

    override fun run(config: JdbcDatabaseConfig) {
        val statement = runner.buildStatement()
        val executor = JdbcExecutor(config, options)
        return executor.execute(statement)
    }

    override fun dryRun(config: DatabaseConfig): Statement {
        return runner.dryRun(config)
    }
}
