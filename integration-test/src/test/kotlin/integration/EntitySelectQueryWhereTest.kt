package integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.Database
import org.komapper.core.dsl.EntityQuery
import org.komapper.core.dsl.desc
import org.komapper.core.dsl.plus
import org.komapper.core.dsl.runQuery
import org.komapper.core.dsl.scope.WhereDeclaration
import org.komapper.core.dsl.scope.WhereScope.Companion.plus

@ExtendWith(Env::class)
class EntitySelectQueryWhereTest(private val db: Database) {

    @Test
    fun isNull() {
        val e = Employee.alias
        val list = db.runQuery {
            EntityQuery.from(e).where {
                e.managerId.isNull()
            }
        }
        assertEquals(listOf(9), list.map { it.employeeId })
    }

    @Test
    fun isNotNull() {
        val e = Employee.alias
        val list = db.runQuery {
            EntityQuery.from(e).where {
                e.managerId.isNotNull()
            }
        }
        assertTrue(9 !in list.map { it.employeeId })
    }

    @Test
    fun between() {
        val a = Address.alias
        val idList = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId between 5..10
            }.orderBy(a.addressId)
        }
        assertEquals((5..10).toList(), idList.map { it.addressId })
    }

    @Test
    fun notBetween() {
        val a = Address.alias
        val idList = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId notBetween 5..10
            }.orderBy(a.addressId)
        }
        val ids = (1..4) + (11..15)
        assertEquals(ids.toList(), idList.map { it.addressId })
    }

    @Test
    fun like() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street like "STREET 1_"
            }.orderBy(a.addressId)
        }
        assertEquals((10..15).toList(), list.map { it.addressId })
    }

    @Test
    fun like_asPrefix() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street like "STREET 1".asPrefix()
            }.orderBy(a.addressId)
        }
        assertEquals(listOf(1) + (10..15), list.map { it.addressId })
    }

    @Test
    fun like_asInfix() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street like "T 1".asInfix()
            }.orderBy(a.addressId)
        }
        assertEquals(listOf(1) + (10..15), list.map { it.addressId })
    }

    @Test
    fun like_asSuffix() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street like "1".asSuffix()
            }.orderBy(a.addressId)
        }
        assertEquals(listOf(1, 11), list.map { it.addressId })
    }

    @Test
    fun like_escape() {
        val a = Address.alias
        val insertQuery = EntityQuery.insert(a, Address(16, "\\STREET _16%", 1))
        val selectQuery = EntityQuery.from(a).where {
            a.street like escape("\\S") + text("%") + escape("T _16%")
        }.orderBy(a.addressId)
        val list = db.runQuery {
            insertQuery + selectQuery
        }
        assertEquals(listOf(16), list.map { it.addressId })
    }

    @Test
    fun like_escapeWithEscapeSequence() {
        val a = Address.alias
        val insertQuery = EntityQuery.insert(a, Address(16, "\\STREET _16%", 1))
        val selectQuery = EntityQuery.from(a).where {
            a.street like escape("\\S") + text("%") + escape("T _16%")
        }.orderBy(a.addressId).option {
            it.copy(escapeSequence = "|")
        }
        val list = db.runQuery {
            insertQuery + selectQuery
        }
        assertEquals(listOf(16), list.map { it.addressId })
    }

    @Test
    fun notLike() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street notLike "STREET 1_"
            }.orderBy(a.addressId)
        }
        assertEquals((1..9).toList(), list.map { it.addressId })
    }

    @Test
    fun notLike_asPrefix() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street notLike "STREET 1".asPrefix()
            }.orderBy(a.addressId)
        }
        assertEquals((2..9).toList(), list.map { it.addressId })
    }

    @Test
    fun notLike_asInfix() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street notLike "T 1".asInfix()
            }.orderBy(a.addressId)
        }
        assertEquals((2..9).toList(), list.map { it.addressId })
    }

    @Test
    fun notLike_asSuffix() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street notLike "1".asSuffix()
            }.orderBy(a.addressId)
        }
        assertEquals(((2..10) + (12..15)).toList(), list.map { it.addressId })
    }

    @Test
    fun notLike_escape() {
        val a = Address.alias
        val insertQuery = EntityQuery.insert(a, Address(16, "\\STREET _16%", 1))
        val selectQuery = EntityQuery.from(a).where {
            a.street notLike escape("\\S") + text("%") + escape("T _16%")
        }.orderBy(a.addressId)
        val list = db.runQuery {
            insertQuery + selectQuery
        }
        assertEquals((1..15).toList(), list.map { it.addressId })
    }

    @Test
    fun startsWith() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street startsWith "STREET 1"
            }.orderBy(a.addressId)
        }
        assertEquals(listOf(1) + (10..15), list.map { it.addressId })
    }

    @Test
    fun startsWith_escape() {
        val a = Address.alias
        val insertQuery = EntityQuery.insert(a, Address(16, "STREET 1%6", 1))
        val selectQuery = EntityQuery.from(a).where {
            a.street startsWith "STREET 1%"
        }.orderBy(a.addressId)
        val list = db.runQuery { insertQuery + selectQuery }
        assertEquals(listOf(16), list.map { it.addressId })
    }

    @Test
    fun notStartsWith() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street notStartsWith "STREET 1"
            }.orderBy(a.addressId)
        }
        assertEquals((2..9).toList(), list.map { it.addressId })
    }

    @Test
    fun contains() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street contains "T 1"
            }.orderBy(a.addressId)
        }
        assertEquals(listOf(1) + (10..15), list.map { it.addressId })
    }

    @Test
    fun notContains() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street notContains "T 1"
            }.orderBy(a.addressId)
        }
        assertEquals((2..9).toList(), list.map { it.addressId })
    }

    @Test
    fun endsWith() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street endsWith "1"
            }.orderBy(a.addressId)
        }
        assertEquals(listOf(1, 11), list.map { it.addressId })
    }

    @Test
    fun notEndsWith() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.street notEndsWith "1"
            }.orderBy(a.addressId)
        }
        assertEquals(((2..10) + (12..15)).toList(), list.map { it.addressId })
    }

    @Test
    fun inList() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId inList listOf(9, 10)
            }.orderBy(a.addressId.desc())
        }
        assertEquals(
            listOf(
                Address(10, "STREET 10", 1),
                Address(9, "STREET 9", 1)
            ),
            list
        )
    }

    @Test
    fun notInList() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId notInList (1..9).toList()
            }.orderBy(a.addressId)
        }
        assertEquals((10..15).toList(), list.map { it.addressId })
    }

    @Test
    fun inList_empty() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId inList emptyList()
            }.orderBy(a.addressId.desc())
        }
        assertTrue(list.isEmpty())
    }

    @Test
    fun inList_SubQuery() {
        val e = Employee.alias
        val a = Address.alias
        val query =
            EntityQuery.from(e).where {
                e.addressId inList {
                    EntityQuery.from(a)
                        .where {
                            e.addressId eq a.addressId
                            e.employeeName like "%S%"
                        }.asSqlQuery().select(a.addressId)
                }
            }
        val list = db.runQuery { query }
        assertEquals(5, list.size)
    }

    @Test
    fun notInList_SubQuery() {
        val e = Employee.alias
        val a = Address.alias
        val query =
            EntityQuery.from(e).where {
                e.addressId notInList {
                    EntityQuery.from(a).where {
                        e.addressId eq a.addressId
                        e.employeeName like "%S%"
                    }.asSqlQuery().select(a.addressId)
                }
            }
        val list = db.runQuery { query }
        assertEquals(9, list.size)
    }

    @Test
    fun inList2() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId to a.version inList2 listOf(9 to 1, 10 to 1)
            }.orderBy(a.addressId.desc())
        }
        assertEquals(
            listOf(
                Address(10, "STREET 10", 1),
                Address(9, "STREET 9", 1)
            ),
            list
        )
    }

    @Test
    fun notInList2() {
        val seq = sequence {
            var i = 0
            while (++i < 10) yield(i to 1)
        }
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId to a.version notInList2 seq.toList()
            }.orderBy(a.addressId)
        }
        assertEquals((10..15).toList(), list.map { it.addressId })
    }

    @Test
    fun inList2_SubQuery() {
        val e = Employee.alias
        val a = Address.alias
        val query =
            EntityQuery.from(e).where {
                e.addressId to e.version inList2 {
                    EntityQuery.from(a)
                        .where {
                            e.addressId eq a.addressId
                            e.employeeName like "%S%"
                        }.asSqlQuery().select(a.addressId, a.version)
                }
            }
        val list = db.runQuery { query }
        assertEquals(5, list.size)
    }

    @Test
    fun notInList_SubQuery2() {
        val e = Employee.alias
        val a = Address.alias
        val query =
            EntityQuery.from(e).where {
                e.addressId to e.version notInList2 {
                    EntityQuery.from(a).where {
                        e.addressId eq a.addressId
                        e.employeeName like "%S%"
                    }.asSqlQuery().select(a.addressId, a.version)
                }
            }
        val list = db.runQuery { query }
        assertEquals(9, list.size)
    }

    @Test
    fun exists() {
        val e = Employee.alias
        val a = Address.alias
        val query =
            EntityQuery.from(e).where {
                exists {
                    EntityQuery.from(a).where {
                        e.addressId eq a.addressId
                        e.employeeName like "%S%"
                    }
                }
            }
        val list = db.runQuery { query }
        assertEquals(5, list.size)
    }

    @Test
    fun notExists() {
        val e = Employee.alias
        val a = Address.alias
        val query =
            EntityQuery.from(e).where {
                notExists {
                    EntityQuery.from(a).where {
                        e.addressId eq a.addressId
                        e.employeeName like "%S%"
                    }
                }
            }
        val list = db.runQuery { query }
        assertEquals(9, list.size)
    }

    @Test
    fun not() {
        val a = Address.alias
        val idList = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId greater 5
                not {
                    a.addressId greaterEq 10
                }
            }.orderBy(a.addressId)
        }.map { it.addressId }
        assertEquals((6..9).toList(), idList)
    }

    @Test
    fun and() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId greater 1
                and {
                    a.addressId greater 1
                }
            }.orderBy(a.addressId.desc())
                .limit(2)
                .offset(5)
        }
        assertEquals(
            listOf(
                Address(10, "STREET 10", 1),
                Address(9, "STREET 9", 1)
            ),
            list
        )
    }

    @Test
    fun or() {
        val a = Address.alias
        val list = db.runQuery {
            EntityQuery.from(a).where {
                a.addressId greaterEq 1
                or {
                    a.addressId greaterEq 1
                }
            }.orderBy(a.addressId.desc())
                .limit(2)
                .offset(5)
        }
        assertEquals(
            listOf(
                Address(10, "STREET 10", 1),
                Address(9, "STREET 9", 1)
            ),
            list
        )
    }

    @Test
    fun composition() {
        val a = Address.alias
        val w1: WhereDeclaration = {
            a.addressId eq 1
        }
        val w2: WhereDeclaration = {
            a.version eq 1
        }
        val list = db.runQuery { EntityQuery.from(a).where(w1 + w2) }
        assertEquals(1, list.size)
    }
}
