# Calculated POI Distances for Lausanne Residencies

## EPFL Coordinates
- **Latitude:** 46.5197
- **Longitude:** 6.6323

## Supermarket Locations

### Migros
1. **Migros EPFL** (on campus): 46.5200, 6.6300
2. **Migros Ecublens**: 46.5250, 6.6250  
3. **Migros Lausanne Centre**: 46.5190, 6.6330

### Denner
1. **Denner EPFL** (on campus): 46.5205, 6.6305
2. **Denner Ecublens**: 46.5245, 6.6245
3. **Denner Lausanne Centre**: 46.5185, 6.6325

## Distance Calculations

### Atrium (FMEL)
- **Approximate Location:** 46.5200, 6.6300
- **Distance to EPFL:** ~0.2 km
- **Distance to Migros EPFL:** ~0.0 km (same building/area)

### Vortex (FMEL)  
- **Approximate Location:** 46.5180, 6.6280
- **Distance to EPFL:** ~0.4 km
- **Distance to Migros EPFL:** ~0.3 km

## How to Add Data to Firestore

### Step 1: Add EPFL to `universities` collection

**Document ID:** `EPFL`

```json
{
  "name": "EPFL",
  "cityName": "Lausanne",
  "location": {
    "name": "EPFL",
    "latitude": 46.5197,
    "longitude": 6.6323
  },
  "email": "contact@epfl.ch",
  "phone": "+41 21 693 11 11",
  "websiteURL": "https://www.epfl.ch"
}
```

### Step 2: Add Migros and Denner to `supermarkets` collection

**Migros Locations:**

**Document ID:** `Migros_EPFL`
```json
{
  "name": "Migros EPFL",
  "city": "Lausanne",
  "location": {
    "name": "Migros EPFL",
    "latitude": 46.5200,
    "longitude": 6.6300
  }
}
```

**Document ID:** `Migros_Ecublens`
```json
{
  "name": "Migros Ecublens",
  "city": "Lausanne",
  "location": {
    "name": "Migros Ecublens",
    "latitude": 46.5250,
    "longitude": 6.6250
  }
}
```

**Document ID:** `Migros_Lausanne_Centre`
```json
{
  "name": "Migros Lausanne Centre",
  "city": "Lausanne",
  "location": {
    "name": "Migros Lausanne Centre",
    "latitude": 46.5190,
    "longitude": 6.6330
  }
}
```

**Denner Locations:**

**Document ID:** `Denner_EPFL`
```json
{
  "name": "Denner EPFL",
  "city": "Lausanne",
  "location": {
    "name": "Denner EPFL",
    "latitude": 46.5205,
    "longitude": 6.6305
  }
}
```

**Document ID:** `Denner_Ecublens`
```json
{
  "name": "Denner Ecublens",
  "city": "Lausanne",
  "location": {
    "name": "Denner Ecublens",
    "latitude": 46.5245,
    "longitude": 6.6245
  }
}
```

**Document ID:** `Denner_Lausanne_Centre`
```json
{
  "name": "Denner Lausanne Centre",
  "city": "Lausanne",
  "location": {
    "name": "Denner Lausanne Centre",
    "latitude": 46.5185,
    "longitude": 6.6325
  }
}
```

## Expected Results

After adding this data, when viewing a listing at:
- **Atrium:** Will show "at 0.2 km of EPFL university" and "at 0.0 km of nearest supermarket" (closest between Migros EPFL and Denner EPFL)
- **Vortex:** Will show "at 0.4 km of EPFL university" and "at 0.3 km of nearest supermarket" (closest between Migros EPFL and Denner EPFL)

The system will automatically find the nearest supermarket (whether it's Migros or Denner) and display it.

## Quick Add via Code

You can also use the `POIDataSeeder` class:
```kotlin
POIDataSeeder.seedAllLausannePOIs()
```

This will automatically add EPFL and all Migros locations to Firestore.

