package com.crushmateapp.crushmate.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.crushmateapp.crushmate.Fragments.MatchesFragment
import com.crushmateapp.crushmate.Fragments.ProfileFragment
import com.crushmateapp.crushmate.Fragments.SwipeFragment
import com.crushmateapp.crushmate.R
import com.crushmateapp.crushmate.util.DATA_CHATS
import com.crushmateapp.crushmate.util.DATA_USERS
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.IOException

const val REQUEST_CODE = 123
class TinderActivity : AppCompatActivity(),CallbackInterace {



    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userId = firebaseAuth.currentUser?.uid

    private lateinit var userDatabase: DatabaseReference
    private lateinit var chatDatabase: DatabaseReference

    private var mylist = ArrayList<String>()
    private var arrayAdapter: ArrayAdapter<String>? = null
    private var i = 0

    private var profileFragment: ProfileFragment? = null
    private var swipeFragment: SwipeFragment? = null
    private var matchesFragment: MatchesFragment? = null

    private var profileTab: TabLayout.Tab? = null
    private var swipeTab: TabLayout.Tab? = null
    private var matchesTab: TabLayout.Tab? = null

    private var resultImageUrl: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if(userId.isNullOrEmpty()) {
            onSignout()
        }

        userDatabase = FirebaseDatabase.getInstance().reference.child(DATA_USERS)
        chatDatabase = FirebaseDatabase.getInstance().reference.child(DATA_CHATS)

        profileTab = navigationTabs.newTab()
        swipeTab = navigationTabs.newTab()
        matchesTab = navigationTabs.newTab()

        profileTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_profile)
        swipeTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_swipe)
        matchesTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_matches)

        navigationTabs.addTab(profileTab!!)
        navigationTabs.addTab(swipeTab!!)
        navigationTabs.addTab(matchesTab!!)

        navigationTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabSelected(tab)
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

                    when (tab) {
                        profileTab -> {
                            if (profileFragment == null) {
                                profileFragment = ProfileFragment()
                                profileFragment!!.setCallback(this@TinderActivity)

                            }
                            replaceFragment(profileFragment!!)
                        }
                        swipeTab -> {
                            if (swipeFragment == null) {
                                swipeFragment = SwipeFragment()
                                swipeFragment!!.setCallback(this@TinderActivity)

                            }
                            replaceFragment(swipeFragment!!)
                        }
                        matchesTab -> {
                            if (matchesFragment == null) {
                                matchesFragment = MatchesFragment()
                                matchesFragment!!.setCallback(this@TinderActivity)

                            }
                            replaceFragment(matchesFragment!!)
                        }
                    }

                }

        })

        profileTab?.select()  //bydefault

    }

    override fun onSignout() {
        firebaseAuth.signOut()



        val intent=Intent(this,StartActivity::class.java)
         startActivity(intent)
        finish()

    }

    override fun onGetUserId(): String {
        return userId!!

    }

    override fun getUserDatabase(): DatabaseReference {
        return userDatabase

    }
    override fun getChatDatabase(): DatabaseReference {
       return chatDatabase
    }
    override fun profileComplete() {
        swipeTab?.select()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            resultImageUrl = data?.data
            saveImage()

        }

    }

    fun saveImage(){

        if(resultImageUrl != null && userId != null) {
            val filePath = FirebaseStorage.getInstance().reference.child("profileImage").child(userId)
            var bitmap: Bitmap? = null
            try {
                bitmap = MediaStore.Images.Media.getBitmap(application.contentResolver, resultImageUrl)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 20, baos)
            val data = baos.toByteArray()

            val uploadTask = filePath.putBytes(data)
            uploadTask.addOnFailureListener { e -> e.printStackTrace() }
            uploadTask.addOnSuccessListener { taskSnapshot ->
                filePath.downloadUrl
                    .addOnSuccessListener { uri ->
                        profileFragment?.updateImageUri(uri.toString())
                    }
                    .addOnFailureListener { e -> e.printStackTrace() }
            }
        }
    }
    override fun ActivityForPhoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE) //it opens new activity for choosing photo
                                                     //recieve it inside onactivityresult

    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

}