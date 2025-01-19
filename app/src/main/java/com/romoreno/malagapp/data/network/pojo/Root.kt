package com.romoreno.malagapp.data.network.pojo

import com.google.gson.annotations.SerializedName


// import com.fasterxml.jackson.databind.ObjectMapper; // version 2.11.1
// import com.fasterxml.jackson.annotation.SerializedName; // version 2.11.1
/* ObjectMapper om = new ObjectMapper();
Root root = om.readValue(myJsonString, Root.class); */
class Crs {
    var type: String? = null
    var properties: Properties? = null
}

class Feature {
    var type: String? = null
    var id: String? = null
    var geometry: Geometry? = null
    var geometry_name: String? = null
    var properties: Properties? = null
}

class Geometry {
    var type: String? = null
    var coordinates: ArrayList<Double>? = null
}

class Properties {
    @SerializedName("ID")
    var id: Int = 0

    @SerializedName("NOMBRE")
    var nombre: String? = null

    @SerializedName("DESCRIPCION")
    var descripcion: String? = null

    @SerializedName("DIRECCION")
    var direccion: String? = null

    @SerializedName("URL")
    var url: String? = null

    @SerializedName("EMAIL")
    var email: Any? = null

    @SerializedName("CONTACTO")
    var contacto: Any? = null

    @SerializedName("HORARIOS")
    var horarios: Any? = null

    @SerializedName("PRECIOS")
    var precios: Any? = null

    @SerializedName("TITULARIDAD")
    var titularidad: String? = null

    @SerializedName("TARJETAJOVEN")
    var tarjetaJoven: String? = null

    @SerializedName("ACCESOPMR")
    var accesoPmr: String? = null

    @SerializedName("URLIMAGEN")
    var urlImagen: String? = null
}

class Root {
    var type: String? = null
    var totalFeatures: Int = 0
    var features: ArrayList<Feature>? = null
    var crs: Crs? = null
}
