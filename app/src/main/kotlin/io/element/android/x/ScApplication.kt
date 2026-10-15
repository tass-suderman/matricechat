package io.element.android.x

import android.app.Application
import io.element.android.libraries.di.DependencyInjectionGraphOwner

abstract class ScApplication : Application(), DependencyInjectionGraphOwner
