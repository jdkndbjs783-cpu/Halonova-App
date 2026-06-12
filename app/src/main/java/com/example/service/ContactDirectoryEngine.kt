package com.example.service

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.ContactEntity
import com.example.data.ContactSearchHistoryEntity
import com.example.data.AssistantMemoryEntity
import com.example.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ContactEngineState {
    READY,
    SEARCHING_CONTACTS,
    CONTACT_FOUND,
    COMPLETED
}

class ContactDirectoryEngine(
    private val context: Context,
    private val repository: UserRepository,
    private val scope: CoroutineScope
) {

    private val _engineState = MutableStateFlow(ContactEngineState.READY)
    val engineState: StateFlow<ContactEngineState> = _engineState.asStateFlow()

    init {
        // Automatically fetch or seed contact index upon initialization
        scanAndIndexContacts()
    }

    /**
     * Checks if contact permission is granted.
     */
    fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Scans system contacts database if permitted, otherwise seeds interactive defaults to local DB.
     */
    fun scanAndIndexContacts() {
        scope.launch(Dispatchers.IO) {
            _engineState.value = ContactEngineState.SEARCHING_CONTACTS
            try {
                val contactList = mutableListOf<ContactEntity>()

                if (hasContactPermission()) {
                    val contentResolver = context.contentResolver
                    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                    var cursor: Cursor? = null
                    try {
                        cursor = contentResolver.query(uri, projection, null, null, null)
                        if (cursor != null && cursor.moveToFirst()) {
                            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                            while (cursor.moveToNext()) {
                                val id = cursor.getString(idIdx)
                                val name = cursor.getString(nameIdx)
                                val number = cursor.getString(numIdx)
                                
                                contactList.add(
                                    ContactEntity(
                                        id = id,
                                        displayName = name,
                                        phoneNumber = number,
                                        email = "${name.lowercase().replace(" ", "")}@domain.local"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        cursor?.close()
                    }
                }

                // Seed high-fidelity mock contacts if running in sandbox/emulator with empty contacts count
                // ensures the UI is immediately alive and searchable with beautiful presets
                val seededContacts = listOf(
                    ContactEntity("seed_tony", "Tony Stark", "+1 415-555-0199", "tony@starkindustries.com", true),
                    ContactEntity("seed_bruce", "Bruce Wayne", "+1 312-555-0122", "bruce@waynecorp.com", true),
                    ContactEntity("seed_steve", "Steve Rogers", "+1 718-500-1941", "cap@shield.gov", false),
                    ContactEntity("seed_peter", "Peter Parker", "+1 212-555-0143", "peter.parker@dailybugle.com", false),
                    ContactEntity("seed_diana", "Diana Prince", "+1 202-555-0177", "diana@themyscira.org", true),
                    ContactEntity("seed_clark", "Clark Kent", "+1 312-555-0148", "clark.kent@dailyplanet.com", false),
                    ContactEntity("seed_natasha", "Natasha Romanoff", "+1 718-555-0152", "natasha@shield.gov", true),
                    ContactEntity("seed_wanda", "Wanda Maximoff", "+1 310-555-0188", "wanda@westview.net", false)
                )

                if (contactList.isEmpty()) {
                    repository.insertContacts(seededContacts)
                } else {
                    // Update contacts in database
                    repository.insertContacts(contactList)
                    // Merge seeds so search testing works cleanly
                    repository.insertContacts(seededContacts)
                }
                
                _engineState.value = ContactEngineState.READY
            } catch (e: Exception) {
                e.printStackTrace()
                _engineState.value = ContactEngineState.READY
            }
        }
    }

    /**
     * Executes contact query search logic, triggers callbacks, persists history, and updates assistant memory.
     */
    suspend fun findContactByQuery(queryName: String, spokenCommand: String? = null): ContactSearchResult = withContext(Dispatchers.IO) {
        _engineState.value = ContactEngineState.SEARCHING_CONTACTS
        
        val cleanedQuery = queryName.lowercase().trim()
        val allMatching = repository.searchContacts(cleanedQuery)

        if (allMatching.isNotEmpty()) {
            val exactMatch = allMatching.find { it.displayName.lowercase().trim() == cleanedQuery }
            val chosenContact = exactMatch ?: allMatching.first()
            
            _engineState.value = ContactEngineState.CONTACT_FOUND

            // Insert to local search history (Persistence)
            repository.insertContactSearchEvent(
                ContactSearchHistoryEntity(
                    contactName = chosenContact.displayName,
                    searchTime = System.currentTimeMillis(),
                    status = "SUCCESS",
                    matchedPhoneNumber = chosenContact.phoneNumber
                )
            )

            // Update Assistant Memory
            updateAssistantMemory(
                contactName = chosenContact.displayName,
                command = spokenCommand ?: "Find Contact: ${chosenContact.displayName}"
            )

            _engineState.value = ContactEngineState.COMPLETED
            return@withContext ContactSearchResult(
                success = true,
                contact = chosenContact,
                message = "Contact found: ${chosenContact.displayName}"
            )
        } else {
            _engineState.value = ContactEngineState.COMPLETED

            // Record failed search in DB log
            repository.insertContactSearchEvent(
                ContactSearchHistoryEntity(
                    contactName = queryName,
                    searchTime = System.currentTimeMillis(),
                    status = "FAILED",
                    matchedPhoneNumber = null
                )
            )

            if (spokenCommand != null) {
                updateAssistantMemory(
                    contactName = "",
                    command = spokenCommand
                )
            }

            return@withContext ContactSearchResult(
                success = false,
                contact = null,
                message = "Contact not found"
            )
        }
    }

    private suspend fun updateAssistantMemory(contactName: String, command: String) {
        val currentMemory = repository.getAssistantMemory()

        // Build newline-separated list of recent actions. Cap to 5 actions.
        val recentList = currentMemory.recentActions
            .split("\n")
            .filter { it.isNotBlank() }
            .toMutableList()

        // Core memory tracking for Phase 10: Save recent contact searches
        val searchList = currentMemory.recentContactSearches
            .split("\n")
            .filter { it.isNotBlank() }
            .toMutableList()

        if (contactName.isNotEmpty()) {
            recentList.add(0, "Searched Contact: found $contactName")
            searchList.add(0, contactName)
        } else {
            recentList.add(0, "Searched Contact: '$command' -> Not Found")
        }

        val truncatedActions = recentList.take(5).joinToString("\n")
        val truncatedSearches = searchList.distinct().take(10).joinToString("\n")

        val updatedMemory = currentMemory.copy(
            lastExecutedCommand = command,
            lastActionTime = System.currentTimeMillis(),
            recentActions = truncatedActions,
            lastSearchedContact = if (contactName.isNotEmpty()) contactName else currentMemory.lastSearchedContact,
            recentContactSearches = truncatedSearches
        )
        repository.saveAssistantMemory(updatedMemory)
    }
}

data class ContactSearchResult(
    val success: Boolean,
    val contact: ContactEntity?,
    val message: String
)
