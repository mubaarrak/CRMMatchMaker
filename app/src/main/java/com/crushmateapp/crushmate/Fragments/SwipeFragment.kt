package com.crushmateapp.crushmate.Fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.crushmateapp.crushmate.Adapters.CardAdapter


import com.crushmateapp.crushmate.R
import com.crushmateapp.crushmate.activities.CallbackInterace
import com.crushmateapp.crushmate.activities.TinderActivity
import com.crushmateapp.crushmate.util.*
import com.google.firebase.database.*
import com.lorentzos.flingswipe.SwipeFlingAdapterView
import kotlinx.android.synthetic.main.fragment_swipe.*

/**
 * A simple [Fragment] subclass.
 */
class SwipeFragment : Fragment() {

    private var callback: CallbackInterace? = null
    private lateinit var userId: String
    private lateinit var userDatabase: DatabaseReference

    private lateinit var chatDatabase: DatabaseReference

    private var cardAdapter: ArrayAdapter<User>? = null
    private var Itemlist = ArrayList<User>()

    private var preferredGender: String? = null
    private var userName: String? = null
    private var imageUrl: String? = null

    fun setCallback(callback: CallbackInterace) {
        this.callback = callback
        userId = callback.onGetUserId()
        userDatabase = callback.getUserDatabase()  ///owhole database not single user database databa
        chatDatabase = callback.getChatDatabase()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        if (!::userDatabase.isInitialized){
            userDatabase = FirebaseDatabase.getInstance().reference.child(DATA_USERS)
        }

        if (!::userId.isInitialized){
            userId ="1000"
        }


        userDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                preferredGender = user?.preferredGender
                userName = user?.name
                imageUrl = user?.imageurl
                populateItems()
            }
        })

        cardAdapter = CardAdapter(context, R.layout.item, Itemlist)

        frame.adapter = cardAdapter
        frame.setFlingListener(object : SwipeFlingAdapterView.onFlingListener {
            override fun removeFirstObjectInAdapter() {
                Itemlist.removeAt(0)
                cardAdapter?.notifyDataSetChanged()

            }

            override fun onLeftCardExit(p0: Any?) {
                var user = p0 as User
                userDatabase.child(user.uid.toString()).child(DATA_SWIPES_LEFT).child(userId).setValue(true)

                //   p0 users
                //   |_id2(swiped left)
                //              |__lefts=id1(user id ho swiped)=true


            }

            override fun onRightCardExit(p0: Any?) {

                val selectedUser = p0 as User
                val selectedUserId = selectedUser.uid
                if (!selectedUserId.isNullOrEmpty()) {
                    userDatabase.child(userId).child(DATA_SWIPES_RIGHT)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                            }

                            override fun onDataChange(p0: DataSnapshot) {

                                //there are two cases if both have swiped or just one swiped right
                                //if both swiped right then make matches value true for both
                                //else just make swiped right true
                                if (p0.hasChild(selectedUserId)) {
                                    Toast.makeText(context, "Match!", Toast.LENGTH_SHORT).show()

                                    val chatKey = chatDatabase.push().key

                                    if (chatKey != null) {

                                        //both siwiped right bcoz p0 has child seleteduserid
                                        userDatabase.child(userId).child(DATA_SWIPES_RIGHT).child(selectedUserId).removeValue()
                                        userDatabase.child(userId).child(DATA_MATCHES).child(selectedUserId).setValue(chatKey)
                                        userDatabase.child(selectedUserId).child(DATA_MATCHES).child(userId).setValue(chatKey)


                                        //update both user chat list recyclerview
                                        chatDatabase.child(chatKey).child(userId).child(DATA_NAME).setValue(userName)
                                        chatDatabase.child(chatKey).child(userId).child(DATA_IMAGE_URL).setValue(imageUrl)

                                        chatDatabase.child(chatKey).child(selectedUserId).child(DATA_NAME).setValue(selectedUser.name)
                                        chatDatabase.child(chatKey).child(selectedUserId).child(DATA_IMAGE_URL).setValue(selectedUser.imageurl)

                                    }

                                } else {
                                    userDatabase.child(selectedUserId).child(DATA_SWIPES_RIGHT).child(userId)
                                        .setValue(true)
                                }
                            }
                        })
                }

            }

            override fun onAdapterAboutToEmpty(p0: Int) {
            }

            override fun onScroll(p0: Float) {
            }
        })

        frame.setOnItemClickListener { position, data -> }
        likeButton.setOnClickListener {
            if (!Itemlist.isEmpty()) {
                frame.topCardListener.selectRight()
            }
        }

        dislikeButton.setOnClickListener {
            if (!Itemlist.isEmpty()) {
                frame.topCardListener.selectLeft()
            }
        }


    }

    fun populateItems() {
        if (noUsersLay != null) {
            noUsersLay.visibility = View.GONE
            progressLay.visibility = View.VISIBLE

        val cardsQuery = userDatabase.orderByChild("gender").equalTo(preferredGender)
        cardsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach {

                    //   p0 is whole users database
                    //   users are its children
                    //    we loop for every children
                    //  get user using  val user = child.getValue(User::class.java)   as child
                    //   check if already (left swiped )(right wiped )or (matches) is done
                    //  check child
                    //           |->swiped left
                    //                    |->userid
                        child ->
                    val user = child.getValue(User::class.java)
                    if (user != null) {
                        var showUser = true
                        if (child.child(DATA_SWIPES_LEFT).hasChild(userId) ||
                            child.child(DATA_SWIPES_RIGHT).hasChild(userId) ||
                            child.child(DATA_MATCHES).hasChild(userId)
                        ) {
                            showUser = false
                        }
                        if (showUser) {
                            // Itemlist.add(user)
                            cardAdapter?.notifyDataSetChanged()
                        }
                    }
                }

                var temp = User(
                    "1000", "Mukesh Ambani", "50", "ambani.mukesh@gmail.com",
                    "male",
                    "female",
                    "https://thumbor.forbes.com/thumbor/fit-in/416x416/filters%3Aformat%28jpg%29/https%3A%2F%2Fspecials-images.forbesimg.com%2Fimageserve%2F5c7d7829a7ea434b351ba0b6%2F0x0.jpg%3Fbackground%3D000000%26cropX1%3D206%26cropX2%3D2043%26cropY1%3D250%26cropY2%3D2089"
                )

                var temp1 = User(
                    "1001", "Gautam Adani", "58", "Gautam.Adani@gmail.com",
                    "male",
                    "female",
                    "https://thumbor.forbes.com/thumbor/fit-in/416x416/filters%3Aformat%28jpg%29/https%3A%2F%2Fspecials-images.forbesimg.com%2Fimageserve%2F5e7689c3d8e1ae0007b722d6%2F0x0.jpg%3Fbackground%3D000000%26cropX1%3D32%26cropX2%3D1066%26cropY1%3D191%26cropY2%3D1226"
                )

                var temp2 = User(
                    "1002", "Hinduja brothers", "62", "Hinduja.brothers@gmail.com",
                    "male",
                    "female",
                    "https://thumbor.forbes.com/thumbor/fit-in/416x416/filters%3Aformat%28jpg%29/https%3A%2F%2Fspecials-images.forbesimg.com%2Fimageserve%2F5c7269a031358e35dd2701d6%2F0x0.jpg%3Fbackground%3D000000%26cropX1%3D771%26cropX2%3D3428%26cropY1%3D11%26cropY2%3D2667"
                )



                Itemlist.add(temp)
                Itemlist.add(temp1)
                Itemlist.add(temp2)




                progressLay.visibility = View.GONE
                if (Itemlist.isEmpty()) {
                    noUsersLay.visibility = View.VISIBLE
                }
            }
        })

    }
    }

}