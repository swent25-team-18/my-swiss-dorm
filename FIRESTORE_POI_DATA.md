# Firestore POI Data to Add

This document contains the exact data to add to Firestore for Points of Interest near Lausanne residencies.

## EPFL (University)

**Collection:** `universities`  
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

## Migros Supermarkets (near EPFL/Lausanne residencies)

**Collection:** `supermarkets`

### Migros EPFL (on campus)
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

### Migros Ecublens (near residencies)
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

### Migros Lausanne Centre
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

## Distance Calculations

### Atrium (FMEL)
- **Location:** ~46.5200, 6.6300
- **Distance to EPFL:** ~0.2 km
- **Distance to Migros EPFL:** ~0.0 km (same location)

### Vortex (FMEL)
- **Location:** ~46.5180, 6.6280
- **Distance to EPFL:** ~0.4 km
- **Distance to Migros EPFL:** ~0.3 km

## How to Add This Data

### Option 1: Firebase Console (Web)
1. Go to https://console.firebase.google.com/
2. Select your project: `my-swiss-dorm`
3. Go to Firestore Database
4. For each collection/document above:
   - Click "Start collection" or select existing collection
   - Add document with the ID specified
   - Paste the JSON data (Firebase will convert it)

### Option 2: Using Admin Script
You can use the existing repository methods in your app's admin interface to add this data programmatically.

