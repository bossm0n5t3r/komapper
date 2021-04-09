package org.komapper.core.dsl.builder

import org.komapper.core.Dialect
import org.komapper.core.data.Statement
import org.komapper.core.data.StatementBuffer
import org.komapper.core.dsl.context.SqlDeleteContext
import org.komapper.core.dsl.element.Criterion
import org.komapper.core.dsl.expression.EntityExpression

internal class SqlDeleteStatementBuilder<ENTITY : Any>(
    val dialect: Dialect,
    val context: SqlDeleteContext<ENTITY>
) {
    private val aliasManager = AliasManagerImpl(context)
    private val buf = StatementBuffer(dialect::formatValue)
    private val support = BuilderSupport(dialect, aliasManager, buf)

    fun build(): Statement {
        buf.append("delete from ")
        table(context.entityMetamodel)
        if (context.where.isNotEmpty()) {
            buf.append(" where ")
            for ((index, criterion) in context.where.withIndex()) {
                visitCriterion(index, criterion)
                buf.append(" and ")
            }
            buf.cutBack(5)
        }
        return buf.toStatement()
    }

    private fun table(expression: EntityExpression<*>) {
        support.visitEntityExpression(expression)
    }

    private fun visitCriterion(index: Int, c: Criterion) {
        return support.visitCriterion(index, c)
    }
}
