package il.ac.technion.cs.softwaredesign

import dev.misfitlabs.kotlinguice4.KotlinModule

class MessagingClientModule: KotlinModule() {
    override fun configure() {
        install(SecureStorageWrapperModule())
        bind<MessagingClientFactory>().to<MessagingClientManager>()
        bind<IDsFactory>().to<ProductionIDsFactory>()
    }
}