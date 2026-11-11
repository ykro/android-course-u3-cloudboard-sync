package com.awa2027.cloudboard.di

import com.awa2027.cloudboard.BuildConfig
import com.awa2027.cloudboard.data.ai.GeminiBoardSummarizer
import com.awa2027.cloudboard.data.ai.LocalBoardSummarizer
import com.awa2027.cloudboard.data.firebase.FirebaseAuthRepository
import com.awa2027.cloudboard.data.firebase.FirestoreBoardRepository
import com.awa2027.cloudboard.domain.repository.AuthRepository
import com.awa2027.cloudboard.domain.repository.BoardRepository
import com.awa2027.cloudboard.domain.repository.BoardSummarizer
import com.awa2027.cloudboard.domain.support.Clock
import com.awa2027.cloudboard.domain.support.IdGenerator
import com.awa2027.cloudboard.domain.sync.Jitter
import com.awa2027.cloudboard.domain.sync.RetryPolicy
import com.awa2027.cloudboard.domain.usecase.CreateCardUseCase
import com.awa2027.cloudboard.domain.usecase.DeleteCardUseCase
import com.awa2027.cloudboard.domain.usecase.ObserveBoardUseCase
import com.awa2027.cloudboard.domain.usecase.SummarizeBoardUseCase
import com.awa2027.cloudboard.domain.usecase.UpdateCardUseCase
import com.awa2027.cloudboard.ui.BoardViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.random.Random

/** Cómo arrancó la app: con Firebase configurado o en modo demo. */
data class AppMode(
    val cloud: Boolean,
)

const val BOARD_ID = "principal"

/** Dominio y presentación: no saben si los datos vienen de Firebase o de memoria. */
val domainModule =
    module {
        single { Clock.System }
        single { IdGenerator.Uuid }
        single {
            RetryPolicy(
                maxAttempts = 4,
                initialDelayMs = 300,
                maxDelayMs = 4_000,
                jitter = Jitter.equal(Random.Default),
            )
        }
        factoryOf(::ObserveBoardUseCase)
        factoryOf(::CreateCardUseCase)
        factoryOf(::UpdateCardUseCase)
        factoryOf(::DeleteCardUseCase)
        factory { SummarizeBoardUseCase(primary = get(), fallback = LocalBoardSummarizer()) }
        viewModelOf(::BoardViewModel)
    }

/** Paso 5 del taller: modo demo sin red ni credenciales. Una sola instancia por tipo para todo el grafo. */
val demoDataModule =
    module {
        // Paso 5 del taller: declara aquí AuthRepository (DemoAuthRepository con DemoData.users),
        // BoardRepository (InMemoryBoardRepository con DemoData.cards) y BoardSummarizer (LocalBoardSummarizer).
    }

/** Firebase real: solo se carga cuando existe una app de Firebase inicializada (google-services.json). */
val firebaseDataModule =
    module {
        single { FirebaseAuth.getInstance() }
        single { FirebaseFirestore.getInstance() }
        single<AuthRepository> { FirebaseAuthRepository(get()) }
        single<BoardRepository> { FirestoreBoardRepository(get(), BOARD_ID) }
        single<BoardSummarizer> { GeminiBoardSummarizer(BuildConfig.GEMINI_MODEL) }
    }

fun appModules(cloudEnabled: Boolean): List<Module> =
    listOf(
        domainModule,
        if (cloudEnabled) firebaseDataModule else demoDataModule,
        module { single { AppMode(cloud = cloudEnabled) } },
    )
