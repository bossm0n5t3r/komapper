package org.komapper.core.dsl.query

import org.komapper.core.DatabaseConfig
import org.komapper.core.data.Statement
import org.komapper.core.dsl.context.EntityUpdateContext
import org.komapper.core.dsl.scope.EntityUpdateOptionDeclaration
import org.komapper.core.dsl.scope.EntityUpdateOptionScope

interface EntityUpdateQuery<ENTITY : Any> : Query<ENTITY> {
    fun option(declaration: EntityUpdateOptionDeclaration): EntityUpdateQuery<ENTITY>
}

internal data class EntityUpdateQueryImpl<ENTITY : Any>(
    private val context: EntityUpdateContext<ENTITY>,
    private val entity: ENTITY,
    private val option: EntityUpdateOption = QueryOptionImpl()
) :
    EntityUpdateQuery<ENTITY> {

    private val support: EntityUpdateQuerySupport<ENTITY> = EntityUpdateQuerySupport(context, option)

    override fun option(declaration: EntityUpdateOptionDeclaration): EntityUpdateQueryImpl<ENTITY> {
        val scope = EntityUpdateOptionScope(option)
        declaration(scope)
        return copy(option = scope.asOption())
    }

    override fun run(config: DatabaseConfig): ENTITY {
        val newEntity = preUpdate(config, entity)
        val statement = buildStatement(config, newEntity)
        val (count) = update(config, statement)
        return postUpdate(newEntity, count)
    }

    private fun preUpdate(config: DatabaseConfig, entity: ENTITY): ENTITY {
        return support.preUpdate(config, entity)
    }

    private fun update(config: DatabaseConfig, statement: Statement): Pair<Int, LongArray> {
        return support.update(config) { it.executeUpdate(statement) }
    }

    private fun postUpdate(entity: ENTITY, count: Int): ENTITY {
        return support.postUpdate(entity, count)
    }

    override fun dryRun(config: DatabaseConfig): Statement {
        return buildStatement(config, entity)
    }

    private fun buildStatement(config: DatabaseConfig, entity: ENTITY): Statement {
        return support.buildStatement(config, entity)
    }
}
