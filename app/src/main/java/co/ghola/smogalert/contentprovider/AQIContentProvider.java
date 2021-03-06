/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.ghola.smogalert.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.net.Uri;

import co.ghola.smogalert.db.DBContract;
import co.ghola.smogalert.db.DBHelper;


public class AQIContentProvider extends ContentProvider {
    DBHelper mDatabaseHelper;

    /**
     * Content authority for this provider.
     */
    private static final String AUTHORITY = DBContract.CONTENT_AUTHORITY;

    // The constants below represent individual URI routes, as IDs. Every URI pattern recognized by
    // this ContentProvider is defined using sUriMatcher.addURI(), and associated with one of these
    // IDs.
    //
    // When a incoming URI is run through sUriMatcher, it will be tested against the defined
    // URI patterns, and the corresponding route ID will be returned.
    /**
     * URI ID for route: /entries
     */
    public static final int ROUTE_AQIS = 1;

    /**
     * URI ID for route: /entries/{ID}
     */
    public static final int ROUTE_AQIS_ID = 2;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "air_quality_sample", ROUTE_AQIS);
        sUriMatcher.addURI(AUTHORITY, "air_quality_sample/*", ROUTE_AQIS_ID);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = DBHelper.getInstance(getContext());
        return true;
    }

    /**
     * Determine the mime type for entries returned by a given URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ROUTE_AQIS:
                return DBContract.AirQualitySample.CONTENT_TYPE;
            case ROUTE_AQIS_ID:
                return DBContract.AirQualitySample.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Perform a database query by URI.
     *
     * <p>Currently supports returning all entries (/entries) and individual entries by ID
     * (/entries/{ID}).
     */
    @Override
    public synchronized  Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case ROUTE_AQIS_ID:
                // Return a single entry, by ID.
                String id = uri.getLastPathSegment();
                builder.where(DBContract.AirQualitySample.COLUMN_NAME_ID + "=?", id);
            case ROUTE_AQIS:
                // Return all known entries.
                builder.table(DBContract.AirQualitySample.TABLE_NAME)
                        .where(selection, selectionArgs);
                Cursor c = builder.query(db, projection, sortOrder);
                // Note: Notification URI must be manually set here for loaders to correctly
                // register ContentObservers.
                Context ctx = getContext();
                assert ctx != null;

                c.setNotificationUri(ctx.getContentResolver(), uri);
                return c;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Insert a new entry into the database.
     */
    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {

        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        switch (match) {
            case ROUTE_AQIS:
                long id = db.insertOrThrow(DBContract.AirQualitySample.TABLE_NAME, null, values);
                result = Uri.parse(DBContract.AirQualitySample.CONTENT_URI + "/" + id);
                break;
            case ROUTE_AQIS_ID:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);

        return result;
    }

    /**
     * Delete an entry by database by URI.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_AQIS:
                count = builder.table(DBContract.AirQualitySample.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .delete(db);

                break;
            case ROUTE_AQIS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(DBContract.AirQualitySample.TABLE_NAME)
                        .where(DBContract.AirQualitySample._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .delete(db);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Send broadcast to registered ContentObservers, to refresh UI.
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    /**
     * Update an etry in the database by URI.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count;
        switch (match) {
            case ROUTE_AQIS:
                count = builder.table(DBContract.AirQualitySample.TABLE_NAME)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            case ROUTE_AQIS_ID:
                String id = uri.getLastPathSegment();
                count = builder.table(DBContract.AirQualitySample.TABLE_NAME)
                        .where(DBContract.AirQualitySample._ID + "=?", id)
                        .where(selection, selectionArgs)
                        .update(db, values);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri, null, false);
        return count;
    }

}
