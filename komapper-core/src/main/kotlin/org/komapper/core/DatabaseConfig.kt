package org.komapper.core

import org.komapper.core.data.JdbcOption
import org.komapper.core.dsl.spi.TemplateStatementBuilder
import org.komapper.core.dsl.spi.TemplateStatementBuilderFactory
import org.komapper.core.jdbc.SimpleDataSource
import java.util.ServiceLoader
import javax.sql.DataSource

/**
 * A database configuration.
 *
 * @property name the name of this configuration. The name is used as a key to manage sequence values. The name must be unique.
 * @property dialect the dialect
 * @property logger the logger
 * @property clockProvider the clock provider
 * @property jdbcOption the jdbc configuration
 * @property session the session
 */
interface DatabaseConfig {
    val name: String
    val dialect: Dialect
    val logger: Logger
    val clockProvider: ClockProvider
    val jdbcOption: JdbcOption
    val session: DatabaseSession
    val templateStatementBuilder: TemplateStatementBuilder
}

abstract class AbstractDatabaseConfig : DatabaseConfig {
    override val templateStatementBuilder: TemplateStatementBuilder by lazy {
        val loader = ServiceLoader.load(TemplateStatementBuilderFactory::class.java)
        val factory = loader.firstOrNull()
            ?: error(
                "TemplateStatementBuilderFactory is not found. " +
                    "Add komapper-template dependency or override the templateStatementBuilder property."
            )
        factory.create(dialect)
    }
}

open class DefaultDatabaseConfig(
    dataSource: DataSource,
    override val dialect: Dialect,
    enableTransaction: Boolean = false
) :
    AbstractDatabaseConfig() {

    @Suppress("unused")
    constructor(
        dialect: Dialect,
        url: String,
        user: String = "",
        password: String = "",
        enableTransaction: Boolean = false
    ) : this(SimpleDataSource(url, user, password), dialect, enableTransaction)

    override val name: String = System.identityHashCode(object {}).toString()
    override val logger: Logger = StdOutLogger()
    override val clockProvider = DefaultClockProvider()
    override val jdbcOption: JdbcOption = JdbcOption(batchSize = 10)
    override val session: DatabaseSession by lazy {
        if (enableTransaction) {
            TransactionalDatabaseSession(dataSource, logger)
        } else {
            DefaultDatabaseSession(dataSource)
        }
    }
}

object DryRunDatabaseConfig : AbstractDatabaseConfig() {
    override val name: String
        get() = throw UnsupportedOperationException()
    override val dialect: Dialect = DryRunDialect
    override val logger: Logger
        get() = throw UnsupportedOperationException()
    override val clockProvider: ClockProvider
        get() = throw UnsupportedOperationException()
    override val jdbcOption: JdbcOption
        get() = throw UnsupportedOperationException()
    override val session: DatabaseSession
        get() = throw UnsupportedOperationException()
}
