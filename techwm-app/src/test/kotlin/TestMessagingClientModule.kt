package il.ac.technion.cs.softwaredesign

import com.google.inject.Singleton
import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class TestMessagingClientModule: KotlinModule() {
    override fun configure() {
        bind<SecureStorageFactory>().to<TestSecureStorageFactory>().`in`<Singleton>()
        bind<MessagingClientFactory>().to<MessagingClientManager>()
        bind<IDsFactory>().to<ProductionIDsFactory>()
    }
}