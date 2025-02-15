package integration.jdbc

import integration.core.department
import integration.core.employee
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.operator.max
import org.komapper.jdbc.JdbcDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(JdbcEnv::class)
class JdbcSelectSubqueryTest(private val db: JdbcDatabase) {

    @Test
    fun subquery_selectClause() {
        val d = Meta.department
        val e = Meta.employee
        val subquery = QueryDsl.from(e).where { d.departmentId eq e.departmentId }.select(count())
        val query = QueryDsl.from(d)
            .orderBy(d.departmentId)
            .select(d.departmentName, subquery)
        val list = db.runQuery { query }
        val expected = listOf("ACCOUNTING" to 3L, "RESEARCH" to 5L, "SALES" to 6L, "OPERATIONS" to 0L)
        assertEquals(expected, list)
    }

    @Test
    fun subquery_selectClause_notNull() {
        val d = Meta.department
        val e = Meta.employee
        val subquery = QueryDsl.from(e).where { d.departmentId eq e.departmentId }.selectNotNull(count())
        val query = QueryDsl.from(d)
            .orderBy(d.departmentId)
            .selectNotNull(d.departmentName, subquery)
        val list = db.runQuery { query }
        val expected = listOf("ACCOUNTING" to 3L, "RESEARCH" to 5L, "SALES" to 6L, "OPERATIONS" to 0L)
        assertEquals(expected, list)
    }

    @Test
    fun subquery_whereClause() {
        val d = Meta.department
        val e = Meta.employee
        val subquery = QueryDsl.from(d)
            .where {
                d.departmentName eq "SALES"
            }.select(max(d.departmentId))
        val query = QueryDsl.from(e).where {
            e.departmentId eq subquery
        }
        val list = db.runQuery { query }
        assertEquals(6, list.size)
    }
}
