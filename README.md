> Proyecto actualmente en fase de desarrollo

# Malagapp

## Descripción del proyecto

Aplicación en desarrollo cuyo objetivo principal es poder visionar en tiempo real las cámaras proporcionadas por el servicio de Datos Abiertos del Ayto. de Málaga.
 
Los datos son almacenados en BBDD SQLite gestionada mediante Room
- Los distritos de la ciudad son poblados (hardcode) la primera vez que la aplicación es iniciada
- Las cámaras de la ciudad son tomadas de la API de Datos Abiertos la primera vez (en el futuro se implementará funcionalidad para sincronizar los datos)

El plantemiento inicial es que la aplicación tenga tres funcionalidades:
- (Lista) Listado de las cámaras disponibles con posibilidad de filtrarlas por distrito o de buscarlas mediante autocompletado
- (Mapa) Visualización de un mapa con todas las cámaras disponibles
- (Adivina) Funcionalidad de visualización aleatoria de una cámara de la ciudad para que el usuario pueda adivinar su ubicación.

Cuando se muestra una cámara, el usuario puede observarla en tiempo real (con una tasa de refresco de 5 segundos) y puede ampliarla aplicando gestos sobre ella.
El usuario también podrá en el futuro guardar el fotograma mostrado o compartirlo con otras aplicaciones de su dispositivo. 

Igualmente se preve la integración de otras funcionalidades de datos abiertos que puedan resultar de interés (buses, semáforos,...)

## Estado actual

Por el momento solo se encuentra implementada la funcionalidad de listado (sin buscador) y de visualización de la cámara elegida. 

### Tecnologías empleadas

- Kotlin (Versión 1.9.24)
- Dagger Hilt (Versión 2.52)
- Retrofit (Versión 2.11.0)
- Glide (4.16.0)
- PhotoView (1.0.2)
- Material Design