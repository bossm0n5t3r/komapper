package org.komapper.jdbc.h2

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.komapper.annotation.KmEntity
import org.komapper.annotation.KmId
import org.komapper.annotation.KmTable
import org.komapper.core.Database
import org.komapper.core.dsl.EntityQuery
import org.komapper.core.dsl.runQuery
import org.komapper.core.jdbc.SimpleDataSource
import org.komapper.core.tx.TransactionIsolationLevel
import java.sql.Blob
import java.sql.Clob
import java.sql.NClob
import java.sql.SQLXML

class TransactionTest {

    private val dataSource = SimpleDataSource("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
    private val config = H2DatabaseConfig(dataSource, enableTransaction = true)
    private val db = Database(config)

    @BeforeEach
    fun before() {
        val sql = """
            CREATE TABLE ADDRESS(ADDRESS_ID INTEGER NOT NULL PRIMARY KEY, STREET VARCHAR(20) UNIQUE, VERSION INTEGER);
            CREATE TABLE ARRAY_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE ARRAY);
            CREATE TABLE BLOB_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BLOB);
            CREATE TABLE CLOB_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE CLOB);
            CREATE TABLE NCLOB_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE NCLOB);
            CREATE TABLE SQL_XML_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE CLOB);

            INSERT INTO ADDRESS VALUES(1,'STREET 1',1);
            INSERT INTO ADDRESS VALUES(2,'STREET 2',1);
            INSERT INTO ADDRESS VALUES(3,'STREET 3',1);
            INSERT INTO ADDRESS VALUES(4,'STREET 4',1);
            INSERT INTO ADDRESS VALUES(5,'STREET 5',1);
            INSERT INTO ADDRESS VALUES(6,'STREET 6',1);
            INSERT INTO ADDRESS VALUES(7,'STREET 7',1);
            INSERT INTO ADDRESS VALUES(8,'STREET 8',1);
            INSERT INTO ADDRESS VALUES(9,'STREET 9',1);
            INSERT INTO ADDRESS VALUES(10,'STREET 10',1);
            INSERT INTO ADDRESS VALUES(11,'STREET 11',1);
            INSERT INTO ADDRESS VALUES(12,'STREET 12',1);
            INSERT INTO ADDRESS VALUES(13,'STREET 13',1);
            INSERT INTO ADDRESS VALUES(14,'STREET 14',1);
            INSERT INTO ADDRESS VALUES(15,'STREET 15',1);
        """.trimIndent()

        dataSource.connection.use { con ->
            con.createStatement().use { stmt ->
                stmt.execute(sql)
            }
        }
    }

    @AfterEach
    fun after() {
        val sql = "DROP ALL OBJECTS"
        dataSource.connection.use { con ->
            con.createStatement().use { stmt ->
                stmt.execute(sql)
            }
        }
    }

    @Test
    fun select() {
        val a = Address.alias
        val list = db.transaction.required {
            db.runQuery { EntityQuery.from(a) }
        }
        assertEquals(15, list.size)
        assertEquals(Address(1, "STREET 1", 1), list[0])
    }

    @Test
    fun commit() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        db.transaction.required {
            val address = db.runQuery { query.first() }
            db.runQuery { EntityQuery.delete(a, address) }
        }
        db.transaction.required {
            val address = db.runQuery { query.firstOrNull() }
            assertNull(address)
        }
    }

    @Test
    fun rollback() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        try {
            db.transaction.required {
                val address = db.runQuery { query.first() }
                db.runQuery { EntityQuery.delete(a, address) }
                throw Exception()
            }
        } catch (ignored: Exception) {
        }
        db.transaction.required {
            val address = db.runQuery { query.first() }
            assertNotNull(address)
        }
    }

    @Test
    fun setRollbackOnly() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        db.transaction.required {
            val address = db.runQuery { query.first() }
            db.runQuery { EntityQuery.delete(a, address) }
            assertFalse(isRollbackOnly())
            setRollbackOnly()
            assertTrue(isRollbackOnly())
        }
        db.transaction.required {
            val address = db.runQuery { query.first() }
            assertNotNull(address)
        }
    }

    @Test
    fun isolationLevel() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        db.transaction.required(TransactionIsolationLevel.SERIALIZABLE) {
            val address = db.runQuery { query.first() }
            db.runQuery { EntityQuery.delete(a, address) }
        }
        db.transaction.required {
            val address = db.runQuery { query.firstOrNull() }
            assertNull(address)
        }
    }

    @Test
    fun required_required() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        db.transaction.required {
            val address = db.runQuery { query.first() }
            db.runQuery { EntityQuery.delete(a, address) }
            required {
                val address2 = db.runQuery { query.firstOrNull() }
                assertNull(address2)
            }
        }
        db.transaction.required {
            val address = db.runQuery { query.firstOrNull() }
            assertNull(address)
        }
    }

    @Test
    fun requiresNew() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        db.transaction.requiresNew {
            val address = db.runQuery { query.first() }
            db.runQuery { EntityQuery.delete(a, address) }
            val address2 = db.runQuery { query.firstOrNull() }
            assertNull(address2)
        }
        db.transaction.required {
            val address = db.runQuery { query.firstOrNull() }
            assertNull(address)
        }
    }

    @Test
    fun required_requiresNew() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        db.transaction.required {
            val address = db.runQuery { query.first() }
            db.runQuery { EntityQuery.delete(a, address) }
            requiresNew {
                val address2 = db.runQuery { query.firstOrNull() }
                assertNotNull(address2)
            }
        }
        db.transaction.required {
            val address = db.runQuery { query.firstOrNull() }
            assertNull(address)
        }
    }

    @Test
    fun invoke() {
        val a = Address.alias
        val query = EntityQuery.from(a).where { a.addressId eq 15 }
        db.transaction {
            val address = db.runQuery { query.first() }
            db.runQuery { EntityQuery.delete(a, address) }
        }
        db.transaction {
            val address = db.runQuery { query.firstOrNull() }
            assertNull(address)
        }
    }

    @KmEntity
    @KmTable(name = "ARRAY_TEST")
    data class ArrayTest(@KmId val id: Int, val value: java.sql.Array) {
        companion object
    }

    @Test
    fun array() {
        db.transaction {
            val meta = ArrayTest.alias
            val array = db.factory.createArrayOf("INTEGER", listOf(10, 20, 30))
            val data = ArrayTest(1, array)
            db.runQuery { EntityQuery.insert(meta, data) }
            val data2 = db.runQuery {
                EntityQuery.first(meta) { meta.id eq 1 }
            }
            assertEquals(data.id, data2.id)
            assertArrayEquals(data.value.array as Array<*>, data2.value.array as Array<*>)
        }
    }

    @KmEntity
    @KmTable(name = "BLOB_TEST")
    data class BlobTest(@KmId val id: Int, val value: Blob) {
        companion object
    }

    @Test
    fun blob() {
        db.transaction {
            val m = BlobTest.alias
            val blob = db.factory.createBlob()
            val bytes = byteArrayOf(10, 20, 30)
            blob.setBytes(1, bytes)
            val data = BlobTest(1, blob)
            db.runQuery { EntityQuery.insert(m, data) }
            val data2 = db.runQuery {
                EntityQuery.first(m) { m.id eq 1 }
            }
            assertEquals(data.id, data2.id)
            assertArrayEquals(data.value.getBytes(1, 3), data2.value.getBytes(1, 3))
        }
    }

    @KmEntity
    @KmTable(name = "CLOB_TEST")
    data class ClobTest(@KmId val id: Int, val value: Clob) {
        companion object
    }

    @Test
    fun clob() {
        db.transaction {
            val m = ClobTest.alias
            val clob = db.factory.createClob()
            clob.setString(1, "ABC")
            val data = ClobTest(1, clob)
            db.runQuery { EntityQuery.insert(m, data) }
            val data2 = db.runQuery {
                EntityQuery.first(m) {
                    m.id to 1
                }
            }
            assertEquals(data.id, data2.id)
            assertEquals(data.value.getSubString(1, 3), data2.value.getSubString(1, 3))
        }
    }

    @KmEntity
    @KmTable(name = "NCLOB_TEST")
    data class NClobTest(@KmId val id: Int, val value: NClob) {
        companion object
    }

    @Test
    fun nclob() {
        db.transaction {
            val m = NClobTest.alias
            val nclob = db.factory.createNClob()
            nclob.setString(1, "ABC")
            val data = NClobTest(1, nclob)
            db.runQuery { EntityQuery.insert(m, data) }
            val data2 = db.runQuery {
                EntityQuery.first(m) {
                    m.id eq 1
                }
            }
            assertEquals(data.id, data2.id)
            assertEquals(data.value.getSubString(1, 3), data2.value.getSubString(1, 3))
        }
    }

    @KmEntity
    @KmTable(name = "SQL_XML_TEST")
    data class SqlXmlTest(@KmId val id: Int, val value: SQLXML) {
        companion object
    }

    @Test
    fun sqlXml() {
        db.transaction {
            val m = SqlXmlTest.alias
            val sqlXml = db.factory.createSQLXML()
            sqlXml.string = """<xml a="v">Text</xml>"""
            val data = SqlXmlTest(1, sqlXml)
            db.runQuery { EntityQuery.insert(m, data) }
            val data2 = db.runQuery {
                EntityQuery.first(m) { m.id eq 1 }
            }
            assertEquals(data.id, data2.id)
            assertEquals(data.value.string, data2.value.string)
        }
    }
}
