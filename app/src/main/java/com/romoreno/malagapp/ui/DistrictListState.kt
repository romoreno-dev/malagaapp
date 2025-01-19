package com.romoreno.malagapp.ui

import com.romoreno.malagapp.data.database.entities.DistrictEntity

sealed class DistrictListState {

    data class Success(val district: List<DistrictEntity>) : DistrictListState()

}