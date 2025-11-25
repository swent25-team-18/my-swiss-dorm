// public/main.js
import { firebaseConfig } from "./firebase-config.js";

// Firebase v9+ modular SDK via CDN
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.0.0/firebase-app.js";
import {
  getFirestore,
  doc,
  getDoc,
} from "https://www.gstatic.com/firebasejs/11.0.0/firebase-firestore.js";

console.log("main.js loaded");

//This file has the logic to get all the necessary data from Firebase
//And enables displaying it on the website
//it was implemented with the help AI

// -------------------------------------------------------------
// 1) Init Firebase / Firestore
// -------------------------------------------------------------
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// Collections
const LISTINGS_COLLECTION = "rental_listings";
const REVIEWS_COLLECTION = "reviews";

// Root content element
const contentEl = document.getElementById("content");

// -------------------------------------------------------------
// 2) Helpers
// -------------------------------------------------------------
function setContent(html) {
  contentEl.innerHTML = html;
}

function getPathParts() {
  // "/listing/abc123" -> ["listing", "abc123"]
  const raw = window.location.pathname.replace(/^\/+/, ""); // remove leading "/"
  if (!raw) return [];
  return raw.split("/");
}

// This is the canonical URL we want to show / share
function getCurrentFullUrl() {
  return window.location.href;
}

function formatDate(value) {
  if (!value) return null;

  // Firestore Timestamp object
  if (typeof value.toDate === "function") {
    return value.toDate().toLocaleDateString();
  }

  // String / number that can be parsed as date
  const d = new Date(value);
  if (!isNaN(d.getTime())) {
    return d.toLocaleDateString();
  }

  return String(value);
}

function formatMonthlyPrice(data) {
  // Try several possible field names – adjust to your schema if needed
  const raw =
    data.pricePerMonth ??
    null;

  if (raw == null) return "N/A";

  const num = Number(raw);
  if (Number.isNaN(num)) return `${raw}.-/month`;

  return `${num.toFixed(1)}.-/month`;
}

function formatSize(data) {
  const raw =
    data.areaInM2 ??
    null;

  if (raw == null) return null;

  const num = Number(raw);
  if (Number.isNaN(num)) return `${raw}`;
  return `${num}m²`;
}

function formatLocation(data) {
  const residency =
    data.residencyName ??
    null;

  const city =
    data.city ??
    data.town ??
    data.locationCity ??
    null;

  const parts = [residency, city].filter(Boolean);
  return parts.length ? parts.join(", ") : null;
}

function buildStars(ratingValue) {
  const rating = Number(ratingValue ?? 0);
  const total = 5;
  const full = Math.round(rating); // simple 0–5 rounding
  const fullStars = "★".repeat(Math.max(0, Math.min(full, total)));
  const emptyStars = "☆".repeat(total - fullStars.length);
  return fullStars + emptyStars;
}


// -------------------------------------------------------------
// 3) Fetch and render listing
// -------------------------------------------------------------
async function loadListing(listingId) {
  setContent(`<p>Loading listing <code>${listingId}</code>...</p>`);

  try {
    const ref = doc(db, LISTINGS_COLLECTION, listingId);
    const snap = await getDoc(ref);

    if (!snap.exists()) {
      setContent(`<p>Listing not found.</p>`);
      return;
    }

    const data = snap.data();

    // Try to match the app:
    // • Studio
    // • 1300.0.-/month
    // • 25m²
    // • Starting 04/11/2025
    const roomType =
      data.roomType ??
      "Listing";

    const priceStr = formatMonthlyPrice(data);
    const sizeStr = formatSize(data);
    const startDate = formatDate(
      data.startDate ?? data.availableFrom ?? data.fromDate
    );
    const locationStr = formatLocation(data);

    const description = data.description ?? "No description.";

    const shareUrl = getCurrentFullUrl();

    setContent(`
      <section class="card">
        <h2 class="section-title">Listing details</h2>
        <ul class="details-list">
          <li>${roomType}</li>
          <li>${priceStr}</li>
          ${sizeStr ? `<li>${sizeStr}</li>` : ""}
          ${startDate ? `<li>Starting ${startDate}</li>` : ""}
        </ul>
      </section>

      <section class="card">
        <h3 class="section-subtitle">Description :</h3>
        <p>${description}</p>
      </section>

      <section class="card">
        <h3 class="section-subtitle">Photos & location</h3>
        <p>
          Download the <strong>MySwissDorm</strong> app to see pictures
          and the exact location on the map.
        </p>
        ${
          locationStr
            ? `<p class="meta"><strong>Approximate location:</strong> ${locationStr}</p>`
            : ""
        }
      </section>

      <section class="share-card">
        <h3>Share this listing</h3>
        <p>You can share this link:</p>
        <div class="share-row">
          <input class="share-input" type="text" readonly value="${shareUrl}" onclick="this.select()" />
        </div>
        <p class="hint">
          Copy it and send it to your friends or scan the QR code from the app.
        </p>
      </section>
    `);
  } catch (err) {
    console.error(err);
    setContent(`<p>Something went wrong while loading the listing.</p>`);
  }
}


// -------------------------------------------------------------
// 4) Fetch and render review
// -------------------------------------------------------------
async function loadReview(reviewId) {
  setContent(`<p>Loading review <code>${reviewId}</code>...</p>`);

  try {
    const ref = doc(db, REVIEWS_COLLECTION, reviewId);
    const snap = await getDoc(ref);

    if (!snap.exists()) {
      setContent(`<p>Review not found.</p>`);
      return;
    }

    const data = snap.data();

    const title = data.title ?? "Review";

    const createdAt = formatDate(data.createdAt ?? data.created_at);

    const roomType =
      data.roomType ??
      data.listingType ??
      data.type ??
      "Room";

    const priceStr = formatMonthlyPrice(data);
    const sizeStr = formatSize(data);

    const ratingVal = data.grade ?? null;
    const stars = ratingVal != null ? buildStars(ratingVal) : null;

    const reviewText =
      data.text ??
      data.reviewText ??
      data.comment ??
      "No review text.";

    const shareUrl = getCurrentFullUrl();

    setContent(`
      <section class="card">
        <h2 class="review-title">${title}</h2>
      </section>

      <section class="card">
        <ul class="details-list">
          <li>${roomType}</li>
          <li>${priceStr}</li>
          ${sizeStr ? `<li>${sizeStr}</li>` : ""}
        </ul>
        ${
          stars
            ? `<p class="stars" aria-label="Rating ${ratingVal}/5">${stars}</p>`
            : ""
        }
      </section>

      <section class="card">
        <h3 class="section-subtitle">Review :</h3>
        <p>${reviewText}</p>
      </section>

      <section class="card">
        <h3 class="section-subtitle">Photos & location</h3>
        <p>
          Download the <strong>MySwissDorm</strong> app to see pictures
          and the exact location for this review.
        </p>
      </section>

      <section class="share-card">
        <h3>Share this review</h3>
        <p>You can share this link:</p>
        <div class="share-row">
          <input class="share-input" type="text" readonly value="${shareUrl}" onclick="this.select()" />
        </div>
        <p class="hint">
          Copy it and send it to your friends or scan the QR code from the app.
        </p>
      </section>
    `);
  } catch (err) {
    console.error(err);
    setContent(`<p>Something went wrong while loading the review.</p>`);
  }
}


// -------------------------------------------------------------
// 5) Router: decide what to load based on /listing/{id} or /review/{id}
// -------------------------------------------------------------
async function bootstrap() {
  console.log("bootstrap() start, pathname =", window.location.pathname);
  const parts = getPathParts(); // e.g. ["listing", "abc123"]

  if (parts.length < 2) {
    // Home / unknown path
    setContent(`
      <section class="card">
        <h2>Welcome to MySwissDorm</h2>
        <p>This page is meant to display shared listings and reviews.</p>
        <p>If you came here by mistake, try opening the app or check the link you received.</p>
      </section>
    `);
    return;
  }

  const type = parts[0];
  const id = parts[1];

  if (type === "listing") {
    await loadListing(id);
  } else if (type === "review") {
    await loadReview(id);
  } else {
    setContent(`<p>Unknown path: <code>${window.location.pathname}</code></p>`);
  }
}

// Start
bootstrap();
