package com.example.util

import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.RawContacts
import android.util.Log
import com.example.data.Customer

object GoogleContactsSyncHelper {
    private const val TAG = "GoogleContactsSync"
    private const val PREF_MAP_NAME = "google_contacts_sync_map"

    /**
     * Legacy Account Syncing replaced. We no longer use AccountManager to scan system accounts.
     * This protects user privacy, avoids GET_ACCOUNTS permission, and reduces Play Store scrutiny.
     */
    fun getGoogleAccounts(context: Context): List<String> {
        return emptyList()
    }

    private fun getSavedRawContactId(context: Context, customerUuid: String): Long? {
        val prefs = context.getSharedPreferences(PREF_MAP_NAME, Context.MODE_PRIVATE)
        val id = prefs.getLong(customerUuid, -1L)
        return if (id == -1L) null else id
    }

    private fun saveRawContactId(context: Context, customerUuid: String, rawContactId: Long) {
        val prefs = context.getSharedPreferences(PREF_MAP_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(customerUuid, rawContactId).apply()
    }

    fun isContactStillExists(context: Context, rawContactId: Long): Boolean {
        val uri = RawContacts.CONTENT_URI
        val projection = arrayOf(RawContacts._ID)
        val selection = "${RawContacts._ID} = ?"
        val selectionArgs = arrayOf(rawContactId.toString())
        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun findRawContactIdByPhone(context: Context, accountEmail: String, phone: String): Long? {
        if (phone.isBlank()) return null
        val cleanPhone = phone.filter { it.isDigit() }
        val uri = Phone.CONTENT_URI
        val projection = arrayOf(Phone.RAW_CONTACT_ID, Phone.NUMBER)
        
        val selection = if (accountEmail.isNotBlank()) {
            "${RawContacts.ACCOUNT_TYPE} = ? AND ${RawContacts.ACCOUNT_NAME} = ?"
        } else {
            null
        }
        val selectionArgs = if (accountEmail.isNotBlank()) {
            arrayOf("com.google", accountEmail)
        } else {
            null
        }

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val number = cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)) ?: ""
                    val cleanNumber = number.filter { it.isDigit() }
                    if (cleanNumber.endsWith(cleanPhone) || cleanPhone.endsWith(cleanNumber)) {
                        return cursor.getLong(cursor.getColumnIndexOrThrow(Phone.RAW_CONTACT_ID))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding raw contact by phone: $phone", e)
        }
        return null
    }

    fun formatCustomerNameForSync(rawName: String, collectionDay: String): String {
        val capitalized = rawName.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
            word.lowercase(java.util.Locale.US).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString()
            }
        }
        val dayName = collectionDay.trim()
        return "$capitalized ($dayName)"
    }

    private fun getOrCreateContactGroup(context: Context, accountEmail: String, groupTitle: String): Long? {
        if (groupTitle.isBlank()) return null
        val uri = ContactsContract.Groups.CONTENT_URI
        val projection = arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE)
        
        val selection = if (accountEmail.isNotBlank()) {
            "${ContactsContract.Groups.TITLE} = ? AND ${ContactsContract.Groups.ACCOUNT_NAME} = ? AND ${ContactsContract.Groups.ACCOUNT_TYPE} = ?"
        } else {
            "${ContactsContract.Groups.TITLE} = ? AND ${ContactsContract.Groups.ACCOUNT_NAME} IS NULL AND ${ContactsContract.Groups.ACCOUNT_TYPE} IS NULL"
        }
        val selectionArgs = if (accountEmail.isNotBlank()) {
            arrayOf(groupTitle, accountEmail, "com.google")
        } else {
            arrayOf(groupTitle)
        }

        var groupId: Long? = null
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    groupId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Groups._ID))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying group: $groupTitle", e)
        }

        if (groupId != null) {
            return groupId
        }

        // Create group if it doesn't exist
        try {
            val ops = ArrayList<ContentProviderOperation>()
            val insertOp = ContentProviderOperation.newInsert(ContactsContract.Groups.CONTENT_URI)
                .withValue(ContactsContract.Groups.TITLE, groupTitle)
                .withValue(ContactsContract.Groups.GROUP_VISIBLE, 1)
            
            if (accountEmail.isNotBlank()) {
                insertOp.withValue(ContactsContract.Groups.ACCOUNT_NAME, accountEmail)
                insertOp.withValue(ContactsContract.Groups.ACCOUNT_TYPE, "com.google")
            } else {
                insertOp.withValue(ContactsContract.Groups.ACCOUNT_NAME, null)
                insertOp.withValue(ContactsContract.Groups.ACCOUNT_TYPE, null)
            }
            ops.add(insertOp.build())
            
            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            if (results.isNotEmpty() && results[0].uri != null) {
                groupId = android.content.ContentUris.parseId(results[0].uri!!)
                Log.i(TAG, "Successfully created group: $groupTitle with ID: $groupId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group: $groupTitle", e)
        }

        return groupId
    }

    @Synchronized
    fun syncCustomerToGoogleContacts(context: Context, customer: Customer, accountEmail: String): Boolean {
        if (customer.phone.isBlank()) {
            Log.w(TAG, "Sync skipped: customer phone is blank.")
            return false
        }

        try {
            var rawContactId = getSavedRawContactId(context, customer.uuid)
            
            if (rawContactId != null && !isContactStillExists(context, rawContactId)) {
                Log.i(TAG, "Saved contact ID $rawContactId no longer exists. Will re-search or re-create.")
                rawContactId = null
            }

            if (rawContactId == null) {
                rawContactId = findRawContactIdByPhone(context, accountEmail, customer.phone)
            }

            val formattedName = formatCustomerNameForSync(customer.name, customer.collectionDay)

            if (rawContactId != null) {
                // UPDATE existing contact
                Log.i(TAG, "Updating existing Contact with ID $rawContactId for client: $formattedName")
                val ops = ArrayList<ContentProviderOperation>()

                // Name update operation
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), StructuredName.CONTENT_ITEM_TYPE)
                    )
                    .withValue(StructuredName.DISPLAY_NAME, formattedName)
                    .build())

                // Phone delete operation to clear old phones before inserting updated ones
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), Phone.CONTENT_ITEM_TYPE)
                    )
                    .build())

                // Phone insert primary operation
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, customer.phone)
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .build())

                // Phone insert secondary operation
                if (customer.phone2.isNotBlank()) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, customer.phone2)
                        .withValue(Phone.TYPE, Phone.TYPE_HOME)
                        .build())
                }

                // Remove existing group memberships
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                    )
                    .build())

                // Add to the group
                val groupId = getOrCreateContactGroup(context, accountEmail, customer.collectionDay)
                if (groupId != null) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                        .build())
                }

                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                saveRawContactId(context, customer.uuid, rawContactId)
                return true
            } else {
                // INSERT new contact
                Log.i(TAG, "Inserting new Contact for client: $formattedName (Account: ${accountEmail.ifBlank { "Local" }})")
                val ops = ArrayList<ContentProviderOperation>()

                val insertRaw = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                if (accountEmail.isNotBlank()) {
                    insertRaw.withValue(RawContacts.ACCOUNT_TYPE, "com.google")
                    insertRaw.withValue(RawContacts.ACCOUNT_NAME, accountEmail)
                } else {
                    insertRaw.withValue(RawContacts.ACCOUNT_TYPE, null)
                    insertRaw.withValue(RawContacts.ACCOUNT_NAME, null)
                }
                ops.add(insertRaw.build())

                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, formattedName)
                    .build())

                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, customer.phone)
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .build())

                if (customer.phone2.isNotBlank()) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, customer.phone2)
                        .withValue(Phone.TYPE, Phone.TYPE_HOME)
                        .build())
                }

                val groupId = getOrCreateContactGroup(context, accountEmail, customer.collectionDay)
                if (groupId != null) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                        .build())
                }

                val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                if (results.isNotEmpty() && results[0].uri != null) {
                    val rawId = android.content.ContentUris.parseId(results[0].uri!!)
                    saveRawContactId(context, customer.uuid, rawId)
                    Log.i(TAG, "Successfully created contact with ID: $rawId")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync customer: ${customer.name}", e)
        }
        return false
    }

    /**
     * Launch standard ContactsContract Intent to insert or edit contact on user's phone.
     */
    fun getAddContactIntent(customer: Customer): Intent {
        val formattedName = formatCustomerNameForSync(customer.name, customer.collectionDay)
        return Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
            type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, formattedName)
            putExtra(ContactsContract.Intents.Insert.PHONE, customer.phone)
            putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, Phone.TYPE_MOBILE)
            if (customer.phone2.isNotBlank()) {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, customer.phone2)
                putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE, Phone.TYPE_HOME)
            }
        }
    }
}
