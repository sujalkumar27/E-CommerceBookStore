#!/usr/bin/env python3
"""
fetch_books.py — Seed data acquisition for the E-Commerce Bookstore.

WHAT THIS SCRIPT DOES (in plain English):
    1. Asks the Open Library website (a free online book database) for lists
       of books across 8 subjects — fiction, technology, history, business,
       self-help, science, biography, philosophy.
    2. For each book, asks Open Library separately for its description.
    3. Invents a rupee price and a stock quantity for each book — real
       book APIs never provide these because the seller sets them.
    4. Saves everything to backend/src/main/resources/data/seed.json,
       which the Spring Boot app reads at startup to fill its database.

WHY THIS SCRIPT EXISTS (rather than having Spring Boot call Open Library at runtime):
    Fetching book data is a one-time job — we want a fixed catalogue, not
    one that shuffles every time the app restarts. Doing it here, offline, means:
      - The Spring Boot app has no runtime dependency on any external website.
      - The catalogue is stable and inspectable (just open seed.json).
      - Anyone can re-run this script to refresh the catalogue.

HOW TO RUN IT:
    From the project root:  python seed/fetch_books.py
    Takes about 1-2 minutes (mostly waiting for ~120 network calls).

REQUIREMENTS:
    Python 3.10 or newer. No pip install needed — everything used here
    ships with Python out of the box (json, random, urllib, pathlib).
"""

# ------------------------------------------------------------------
# IMPORTS — all standard library, nothing to install
# ------------------------------------------------------------------
import json               # Convert Python dicts/lists to JSON text and back
import random             # Generate fake prices and stock counts
import sys                # Print errors to stderr, exit with a status code
import time               # Pause between network calls (be polite to the API)
import urllib.error       # Catch HTTP errors like 429 (rate-limit)
import urllib.parse       # Safely build URLs with query parameters
import urllib.request     # Make HTTP GET calls
from pathlib import Path  # Cross-platform file-path handling


# ==================================================================
# CONFIGURATION — tune these values to change the script's behaviour
# ==================================================================

# The 8 subjects we ask Open Library for.
# Each subject becomes the `category` field on the books returned.
SUBJECTS = [
    "fiction",
    "technology",
    "history",
    "business",
    "self-help",
    "science",
    "biography",
    "philosophy",
]

# How many books to request per subject.
# 8 subjects × 15 books = 120 raw results → expect ~90-115 after filtering.
BOOKS_PER_SUBJECT = 15

# Open Library API endpoints
SEARCH_URL        = "https://openlibrary.org/search.json"
WORK_URL_TEMPLATE = "https://openlibrary.org{work_key}.json"

# Identifies our script to the Open Library server
USER_AGENT = "ecommerce-bookstore-seed/1.0 (learning-project; contact: local-dev)"

# Where the output JSON is written.
# Path(__file__) = this Python file's location
# .parent = the seed/ folder
# .parent = the project root
# Then we go into backend/src/main/resources/data/seed.json
OUTPUT_PATH = (
    Path(__file__).resolve().parent.parent
    / "backend" / "src" / "main" / "resources" / "data" / "seed.json"
)

# Fixed random seed = same prices/stock on every run (no noisy git diffs)
random.seed(42)


# ==================================================================
# NETWORKING — helpers for making HTTP calls to Open Library
# ==================================================================

def http_get_json(url: str, retries: int = 2) -> dict:
    """
    Download the contents of `url` and parse them as JSON.
    If the server responds with 429 (rate-limit), waits and retries.
    """
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})

    for attempt in range(retries + 1):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                body = response.read().decode("utf-8")
                return json.loads(body)
        except urllib.error.HTTPError as exc:
            if exc.code == 429 and attempt < retries:
                wait = 5 * (attempt + 1)
                print(f"(429 rate-limited, waiting {wait}s) ", end="", flush=True)
                time.sleep(wait)
                continue
            raise
    return {}


def fetch_subject_books(subject: str) -> list:
    """
    Ask Open Library for BOOKS_PER_SUBJECT English-language books in the given subject.
    Returns a list of raw book dicts from the API.
    """
    params = {
        "subject":  subject,
        "limit":    BOOKS_PER_SUBJECT,
        "language": "eng",
        "fields":   "key,title,author_name,isbn,publisher,"
                    "first_publish_year,number_of_pages_median,cover_i",
    }
    url = f"{SEARCH_URL}?{urllib.parse.urlencode(params)}"
    payload = http_get_json(url)
    return payload.get("docs", [])


def fetch_description(work_key: str):
    """
    Ask Open Library for one book's description via /works/{key}.json.
    Returns the description string, or None if unavailable.
    """
    if not work_key:
        return None
    url = WORK_URL_TEMPLATE.format(work_key=work_key)
    try:
        payload = http_get_json(url, retries=1)
    except Exception:
        return None

    desc = payload.get("description")
    # Open Library returns description as either a string or {"value": "..."}
    if isinstance(desc, dict):
        desc = desc.get("value")
    if isinstance(desc, str) and desc.strip():
        return desc.strip()
    return None


# ==================================================================
# FIELD HELPERS — extract/transform specific fields
# ==================================================================

def pick_isbn(isbns: list) -> str:
    """
    Pick the best ISBN from a list. Prefers ISBN-13 (13 digits), falls back to ISBN-10.
    Returns None if no valid ISBN found.
    """
    for candidate in (isbns or []):
        clean = (candidate or "").strip().replace("-", "")
        if len(clean) == 13 and clean.isdigit():
            return clean
    for candidate in (isbns or []):
        clean = (candidate or "").strip().replace("-", "").upper()
        if len(clean) == 10:
            first_nine, last = clean[:-1], clean[-1]
            if first_nine.isdigit() and (last.isdigit() or last == "X"):
                return clean
    return None


def build_cover_url(cover_id) -> str:
    """Build the Open Library cover image URL for a given cover_id."""
    return f"https://covers.openlibrary.org/b/id/{cover_id}-M.jpg"


def build_fallback_description(book: dict) -> str:
    """
    Build a simple description from known fields when Open Library has none.
    Stays factual — no invented claims about the book's content.
    """
    authors = ", ".join(book["authors"][:2])
    parts = [f"A {book['category'].lower()} book by {authors}"]
    if book.get("publisher"):
        parts.append(f", published by {book['publisher']}")
    if book.get("publishedDate"):
        parts.append(f" in {book['publishedDate']}")
    parts.append(".")
    if book.get("pageCount"):
        parts.append(f" {book['pageCount']} pages.")
    return "".join(parts)


def generate_price(page_count) -> float:
    """
    Invent a rupee price roughly proportional to page count.
    Base ₹199 + ₹1.50/page, capped at ₹899.
    If page count unknown, random ₹250–₹750.
    """
    if page_count and page_count > 0:
        price = min(199 + page_count * 1.5, 899)
    else:
        price = random.uniform(250, 750)
    return round(price, 2)


def generate_stock() -> int:
    """
    Invent a stock count: 10% of books are out of stock (0), rest are 5-50.
    This exercises the out-of-stock UI path.
    """
    if random.random() < 0.10:
        return 0
    return random.randint(5, 50)


# ==================================================================
# TRANSFORMATION — convert a raw Open Library doc into our Book shape
# ==================================================================

def transform_doc(doc: dict, subject: str):
    """
    Convert one raw Open Library search result into our Book JSON shape.
    Returns None if any required field is missing (title, author, isbn, cover).
    """
    isbn     = pick_isbn(doc.get("isbn") or [])
    title    = (doc.get("title") or "").strip()
    authors  = doc.get("author_name") or []
    cover_id = doc.get("cover_i")

    # All four fields are required — skip the book if any is missing
    if not (isbn and title and authors and cover_id):
        return None

    year = doc.get("first_publish_year")
    published_date = str(year) if year else None

    publishers = doc.get("publisher") or []
    publisher = publishers[0].strip() if publishers else None

    page_count = doc.get("number_of_pages_median")
    if not page_count or page_count <= 0:
        page_count = None

    return {
        "isbn":          isbn,
        "title":         title,
        "authors":       authors[:5],       # cap at 5 contributors
        "description":   None,              # filled in the second pass below
        "coverImageUrl": build_cover_url(cover_id),
        "publisher":     publisher,
        "publishedDate": published_date,
        "pageCount":     page_count,
        "language":      "en",
        "category":      subject.title(),   # "fiction" → "Fiction"
        "price":         generate_price(page_count),
        "stockQuantity": generate_stock(),
        "_work_key":     doc.get("key"),    # internal — removed before final write
    }


# ==================================================================
# MAIN — ties everything together
# ==================================================================

def main():
    # ---- Phase A: fetch books from each subject ----
    books = []
    seen_isbns = set()

    for subject in SUBJECTS:
        print(f"Fetching '{subject}'...", end=" ", flush=True)
        try:
            docs = fetch_subject_books(subject)
        except Exception as exc:
            print(f"FAILED: {exc}")
            continue

        added = 0
        for doc in docs:
            book = transform_doc(doc, subject)
            if book is None:
                continue
            if book["isbn"] in seen_isbns:
                continue
            seen_isbns.add(book["isbn"])
            books.append(book)
            added += 1

        print(f"got {len(docs)} raw, kept {added} new")
        time.sleep(0.5)   # polite pause between subjects

    print(f"\nBooks after transform + dedup: {len(books)}")

    if not books:
        print("\nERROR: no books collected. Check your internet connection.", file=sys.stderr)
        sys.exit(1)

    # ---- Phase B: fetch descriptions (one extra call per book) ----
    print(f"\nFetching descriptions ({len(books)} calls, ~200ms each)...")
    real_desc_count = 0
    for i, book in enumerate(books, 1):
        if i % 10 == 0 or i == len(books):
            print(f"  {i}/{len(books)}")

        work_key    = book.pop("_work_key", None)
        description = fetch_description(work_key)

        if description:
            real_desc_count += 1
        else:
            description = build_fallback_description(book)
        book["description"] = description
        time.sleep(0.2)

    print(f"  {real_desc_count}/{len(books)} books got a real description; "
          f"rest use fallback.")

    # ---- Phase C: sanity checks ----
    # Ensure at least one out-of-stock book (exercises that UI path)
    if not any(b["stockQuantity"] == 0 for b in books):
        books[-1]["stockQuantity"] = 0

    categories = {b["category"] for b in books}
    print(f"\nDistinct categories : {len(categories)} — {sorted(categories)}")
    print(f"Total books collected: {len(books)}")

    if len(books) < 50:
        print(f"WARNING: only {len(books)} books (wanted >= 50)")
    if len(categories) < 5:
        print(f"WARNING: only {len(categories)} categories (wanted >= 5)")

    # ---- Phase D: write seed.json ----
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT_PATH.open("w", encoding="utf-8") as f:
        json.dump(books, f, indent=2, ensure_ascii=False)
    print(f"\nDone. Wrote {len(books)} books to:\n  {OUTPUT_PATH}")
    print("\nNext step: start the Spring Boot backend — it will load the books automatically.")


if __name__ == "__main__":
    main()
