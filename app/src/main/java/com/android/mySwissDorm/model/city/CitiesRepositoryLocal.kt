package com.android.mySwissDorm.model.city

import android.location.Location
import com.android.mySwissDorm.R

/** Represents a repository that manages a local list of todos. */
class CitiesRepositoryLocal : CitiesRepository {
    private val cities: MutableList<City> = mutableListOf(
        City(
            name = CityName.LAUSANNE,
            description = "Lausanne is a city located on Lake Geneva, known for its universities and the Olympic Museum.",
            location = Location("manual").apply {
                latitude = 46.5197
                longitude = 6.6323
            },
            imageId = R.drawable.lausanne
        ),
        City(
            name = CityName.GENEVA,
            description = "Geneva is a global city, hosting numerous international organizations.",
            location = Location("manual").apply {
                latitude = 46.2044
                longitude = 6.1432
            },
            R.drawable.geneve
        ),
        City(
            name = CityName.ZURICH,
            description = "Zurich is the largest city in Switzerland and a major financial hub.",
            location = Location("manual").apply {
                latitude = 47.3769
                longitude = 8.5417
            },
            R.drawable.zurich
        ),
        City(
            name = CityName.FRIBOURG,
            description = "Fribourg is a bilingual city famous for its medieval architecture.",
            location = Location("manual").apply {
                latitude = 46.8065
                longitude = 7.16197
            },
            R.drawable.fribourg
        )
    )

    private var counter = 0

    override suspend fun getAllCities(): List<City> {
        return cities
    }

    override suspend fun getCity(cityName: CityName): City {
        return cities.find { it.name == cityName }
            ?: throw Exception("CitiesRepositoryLocal: City not found")
    }
}