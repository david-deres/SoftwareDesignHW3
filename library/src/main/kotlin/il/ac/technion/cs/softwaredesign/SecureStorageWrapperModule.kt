package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule

class SecureStorageWrapperModule : KotlinModule() {

    private val injector = Guice.createInjector(SecureStorageModule())
    private val secureStorageFactory = injector.getInstance<SecureStorageFactory>()

    override fun configure() {
        bind<SecureStorageFactory>().toInstance(secureStorageFactory)
    }

}