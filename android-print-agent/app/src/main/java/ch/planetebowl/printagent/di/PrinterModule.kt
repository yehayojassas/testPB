package ch.planetebowl.printagent.di

import ch.planetebowl.printagent.domain.repository.PrinterClient
import ch.planetebowl.printagent.printing.TcpEscPosPrinterClient
import ch.planetebowl.printagent.printing.TicketFormatter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrinterModule {

    @Binds
    @Singleton
    abstract fun bindPrinterClient(impl: TcpEscPosPrinterClient): PrinterClient

    companion object {
        @Provides
        fun provideTicketFormatter(): TicketFormatter = TicketFormatter()
    }
}
