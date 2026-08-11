// הגדרת פלאגינים החלים על כל המודולים בפרויקט
plugins {
    // פלאגין בסיסי לפרויקט אנדרואיד.
    id("com.android.application") version "8.13.0" apply false
    // פלאגין לשירותים של גוגל, נדרש עבור Firebase.
    id("com.google.gms.google-services") version "4.4.1" apply false
}