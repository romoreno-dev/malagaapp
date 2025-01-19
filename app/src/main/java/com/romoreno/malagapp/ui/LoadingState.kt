package com.romoreno.malagapp.ui

sealed class LoadingState {

    data object Loading : LoadingState()
    data object Loaded : LoadingState()

}