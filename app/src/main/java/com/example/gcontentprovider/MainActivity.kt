package com.example.gcontentprovider

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contacts: List<Contact>
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS), 1)
        } else {
            contacts = getContacts()
            setupConvertButton()
            setupRecyclerView()
        }

    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        contactAdapter = ContactAdapter(contacts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = contactAdapter
    }

    private fun setupConvertButton() {
        val btn_convert = findViewById<Button>(R.id.btn_convert)
        btn_convert.setOnClickListener {
            convertAndSaveContacts()
            contactAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Contacts updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertAndSaveContacts() {
        contacts.forEach { contact ->
            val newNumber = convertPhoneNumber(contact.phoneNumber)
            if (newNumber != contact.phoneNumber) {
                updatePhoneNumber(contact.name, newNumber)
                contact.phoneNumber = newNumber
            }
        }
    }

    private fun getContacts(): List<Contact> {
        val contactList = mutableListOf<Contact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            if (numberIndex != -1) {
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    contactList.add(Contact(name, number))
                }
            } else {
                Log.e("Cursor Error", "Column index not found.")
            }
        }
        return contactList
    }

    private fun convertPhoneNumber(number: String): String {
        return when {
            number.startsWith("0167") -> "037" + number.substring(4)
            number.startsWith("84167") -> "037" + number.substring(5)
            else -> number
        }
    }

    private fun updatePhoneNumber(contactName: String, newNumber: String) {
        val values = ContentValues().apply {
            put(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumber)
        }

        val rowsUpdated = contentResolver.update(
            ContactsContract.Data.CONTENT_URI,
            values,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?",
            arrayOf(contactName)
        )

        if (rowsUpdated > 0) {
            Log.d("Update Success", "Updated contact: $contactName")
        } else {
            Log.e("Update Failed", "Failed to update contact: $contactName")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            contacts = getContacts()
            setupConvertButton()
        }
    }
}