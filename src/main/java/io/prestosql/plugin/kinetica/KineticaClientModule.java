/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.plugin.kinetica;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.kinetica.jdbc.Driver;
import io.prestosql.plugin.jdbc.BaseJdbcConfig;
import io.prestosql.plugin.jdbc.ConnectionFactory;
import io.prestosql.plugin.jdbc.DecimalModule;
import io.prestosql.plugin.jdbc.DriverConnectionFactory;
import io.prestosql.plugin.jdbc.ForBaseJdbc;
import io.prestosql.plugin.jdbc.JdbcClient;
import io.prestosql.plugin.jdbc.credential.CredentialProvider;

import java.sql.SQLException;
import java.util.Properties;

import static io.airlift.configuration.ConfigBinder.configBinder;

public class KineticaClientModule
        implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(JdbcClient.class).annotatedWith(ForBaseJdbc.class).to(KineticaClient.class).in(Scopes.SINGLETON);
        configBinder(binder).bindConfig(KineticaJdbcConfig.class);
        configBinder(binder).bindConfig(KineticaConfig.class);
        binder.install(new DecimalModule());
    }

    @Provides
    @Singleton
    @ForBaseJdbc
    public static ConnectionFactory createConnectionFactory(BaseJdbcConfig config, CredentialProvider credentialProvider, KineticaConfig kineticaConfig)
            throws SQLException
    {
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("useInformationSchema", Boolean.toString(kineticaConfig.isDriverUseInformationSchema()));
        connectionProperties.setProperty("nullCatalogMeansCurrent", "false");
        connectionProperties.setProperty("useUnicode", "true");
        connectionProperties.setProperty("characterEncoding", "utf8");
        connectionProperties.setProperty("tinyInt1isBit", "false");
        if (kineticaConfig.isAutoReconnect()) {
            connectionProperties.setProperty("autoReconnect", String.valueOf(kineticaConfig.isAutoReconnect()));
            connectionProperties.setProperty("maxReconnects", String.valueOf(kineticaConfig.getMaxReconnects()));
        }
        if (kineticaConfig.getConnectionTimeout() != null) {
            connectionProperties.setProperty("connectTimeout", String.valueOf(kineticaConfig.getConnectionTimeout().toMillis()));
        }

        return new DriverConnectionFactory(
                new Driver(),
                config.getConnectionUrl(),
                connectionProperties,
                credentialProvider);
    }
}
