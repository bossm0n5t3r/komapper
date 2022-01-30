package org.komapper.core.dsl.runner

import org.komapper.core.DatabaseConfig
import org.komapper.core.DryRunStatement
import org.komapper.core.Statement
import org.komapper.core.dsl.context.EntityInsertContext
import org.komapper.core.dsl.metamodel.EntityMetamodel

class EntityInsertMultipleRunner<ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>>(
    context: EntityInsertContext<ENTITY, ID, META>,
    private val entities: List<ENTITY>
) :
    Runner {

    private val support: EntityInsertRunnerSupport<ENTITY, ID, META> =
        EntityInsertRunnerSupport(context)

    override fun dryRun(config: DatabaseConfig): DryRunStatement {
        if (entities.isEmpty()) return DryRunStatement.EMPTY
        val statement = buildStatement(config, entities)
        return DryRunStatement.of(statement, config.dialect)
    }

    fun buildStatement(config: DatabaseConfig, entities: List<ENTITY>): Statement {
        return support.buildStatement(config, entities)
    }
}
