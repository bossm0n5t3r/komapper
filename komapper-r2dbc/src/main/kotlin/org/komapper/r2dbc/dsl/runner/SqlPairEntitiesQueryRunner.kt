package org.komapper.r2dbc.dsl.runner

import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import org.komapper.core.Statement
import org.komapper.core.dsl.builder.SqlSelectStatementBuilder
import org.komapper.core.dsl.context.SqlSelectContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.option.SqlSelectOption
import org.komapper.r2dbc.R2dbcDatabaseConfig
import org.komapper.r2dbc.R2dbcDialect
import org.komapper.r2dbc.R2dbcExecutor

class SqlPairEntitiesQueryRunner<A : Any, A_META : EntityMetamodel<A, *, A_META>, B : Any, R>(
    private val context: SqlSelectContext<A, *, A_META>,
    private val option: SqlSelectOption,
    private val provide: (R2dbcDialect, Row) -> Pair<A, B?>,
    private val collect: suspend (Flow<Pair<A, B?>>) -> R
) :
    R2dbcQueryRunner<R> {

    override suspend fun run(config: R2dbcDatabaseConfig): R {
        if (!option.allowEmptyWhereClause && context.where.isEmpty()) {
            error("Empty where clause is not allowed.")
        }
        val statement = buildStatement(config)
        val executor = R2dbcExecutor(config, option)
        return executor.executeQuery(statement, provide, collect)
    }

    override fun dryRun(config: R2dbcDatabaseConfig): String {
        return buildStatement(config).toString()
    }

    private fun buildStatement(config: R2dbcDatabaseConfig): Statement {
        val builder = SqlSelectStatementBuilder(config.dialect, context)
        return builder.build()
    }
}
