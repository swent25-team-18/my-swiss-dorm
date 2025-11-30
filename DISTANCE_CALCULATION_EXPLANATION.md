# Distance Calculation Explanation

## How Distances Are Calculated

The app uses the **Haversine formula** to calculate great-circle distances between two points on Earth. This is the standard method for calculating distances between latitude/longitude coordinates.

### Formula Implementation

Located in: `app/src/main/java/com/android/mySwissDorm/model/map/Location.kt`

```kotlin
fun Location.distanceTo(otherLoc: Location): Double {
  val R = 6371.0 // Earth's mean radius in kilometers
  
  val latDistance = Math.toRadians(otherLoc.latitude - latitude)
  val lonDistance = Math.toRadians(otherLoc.longitude - longitude)
  
  val a = sin(latDistance / 2).pow(2) +
          cos(Math.toRadians(latitude)) *
          cos(Math.toRadians(otherLoc.latitude)) *
          sin(lonDistance / 2).pow(2)
  
  val c = 2 * atan2(sqrt(a), sqrt(1 - a))
  
  return R * c // Distance in kilometers
}
```

### How It Works

1. **Converts degrees to radians** - Required for trigonometric functions
2. **Calculates differences** - Latitude and longitude differences between the two points
3. **Haversine formula** - Uses the formula: `a = sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlon/2)`
4. **Central angle** - Calculates the central angle `c = 2 × atan2(√a, √(1-a))`
5. **Distance** - Multiplies by Earth's radius (6371 km) to get distance in kilometers

### Rounding

The distance is rounded to **1 decimal place**:
```kotlin
distanceKm = round(distance * 10) / 10.0
```

## Common Issues

### 1. **Wrong Coordinates**
If coordinates in Firestore are incorrect, distances will be wrong.

**Example:**
- Atrium location: `46.5200, 6.6300`
- Migros EPFL: `46.5200, 6.6300` (should be ~0.0 km)
- Migros Ecublens: `46.5250, 6.6250` (should be ~0.5 km)
- Migros Renens: `46.5400, 6.5900` (should be ~2.6 km)

If Atrium's actual coordinates are different, all distances will be off.

### 2. **Coordinate Format**
- **Latitude**: -90 to +90 (North/South)
- **Longitude**: -180 to +180 (East/West)
- For Lausanne, Switzerland: ~46.5°N, ~6.6°E

### 3. **Precision**
Coordinates should have at least 4 decimal places for accuracy:
- 1 decimal place ≈ 11 km accuracy
- 4 decimal places ≈ 11 m accuracy

## Verification

To verify distances are correct, you can:

1. **Check Logcat** - The code now logs all calculated distances:
   ```
   D DistanceService: Migros Migros EPFL: 0.1 km from (46.5200, 6.6300)
   D DistanceService: Migros Migros Ecublens: 0.5 km from (46.5200, 6.6300)
   ```

2. **Use Google Maps** - Compare with Google Maps distance calculator:
   - Go to Google Maps
   - Right-click on point A → "Measure distance"
   - Click on point B
   - Compare with app's calculated distance

3. **Manual Calculation** - Use an online Haversine calculator:
   - https://www.movable-type.co.uk/scripts/latlong.html

## Expected Distances for Lausanne Area

### Atrium (46.5200, 6.6300)
- EPFL (46.5197, 6.6323): ~0.2 km
- Migros EPFL (46.5200, 6.6300): ~0.0 km
- Denner EPFL (46.5205, 6.6305): ~0.1 km
- Migros Ecublens (46.5250, 6.6250): ~0.6 km
- UNIL (46.5225, 6.5794): ~3.8 km

### Vortex (46.5180, 6.6280)
- EPFL (46.5197, 6.6323): ~0.4 km
- Migros EPFL (46.5200, 6.6300): ~0.3 km
- Denner EPFL (46.5205, 6.6305): ~0.4 km

## Debugging Steps

1. **Check listing coordinates** in Firestore:
   - Collection: `rental_listings`
   - Field: `location.latitude` and `location.longitude`

2. **Check supermarket coordinates** in Firestore:
   - Collection: `supermarkets`
   - Field: `location.latitude` and `location.longitude`

3. **Check Logcat** for calculated distances when viewing a listing

4. **Verify coordinates** match real-world locations using Google Maps

